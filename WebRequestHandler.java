/**
 ** Code adopted from:
 **
 ** Yale CS433/533 Demo Basic Web Server
 **/
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;


class WebRequestHandler {

    static boolean _DEBUG = true;
    static int     reqCount = 0;

    String WWW_ROOT;
    String QUERY_STRING;
    String serverName;
    Socket connSocket;
    List<VirtualHost> virtualHosts;
    BufferedReader inFromClient;
    DataOutputStream outToClient;

    String urlName;
    String fileName;
    File fileInfo;

    ZonedDateTime ifModifiedSince;
    ZonedDateTime lastModifiedZdt;

    boolean usingMobile;


    public WebRequestHandler(Socket connectionSocket, 
                 List<VirtualHost> virtualHosts) throws Exception
    {
        reqCount ++;

        this.virtualHosts = virtualHosts;

        this.connSocket = connectionSocket;

        if(connectionSocket != null){
            inFromClient =
              new BufferedReader(new InputStreamReader(connSocket.getInputStream()));

            outToClient =
              new DataOutputStream(connSocket.getOutputStream());
      }

    }

    public void processRequest() 
    {

    try {
        mapURL2File();

        if ( fileInfo != null ) // found the file and knows its info
        {
            // find out when file last modified
            long lastModified = fileInfo.lastModified();
            Instant instant = Instant.ofEpochMilli(lastModified);
            lastModifiedZdt = ZonedDateTime.ofInstant(instant , 
                ZoneId.of("GMT"));

            // if checking "if-modified-since", we may want to just notify
            if(ifModifiedSince != null && ifModifiedSince.compareTo(lastModifiedZdt) > 0){
                outputNotModified();
                return;
            }else{
                outputResponseHeader();
                outputResponseBody();
            }
        } // do not handle error

        
    } catch (Exception e) {
        System.out.println("Encountered error, sending report and closing socket");
        e.printStackTrace();
        outputError(400, "Server error");
    } finally {
        try{
            connSocket.close();
        }catch (Exception e){
            System.out.println("Couldn't close socket");
            e.printStackTrace();
        }
    }



    } // end of processARequest

    public void mapURL2File() throws Exception 
    {
        // Configure default host
        if(!virtualHosts.isEmpty()){
            this.WWW_ROOT = virtualHosts.get(0).documentRoot;
        }else{
            throw new Exception();
        }

        String requestMessageLine = inFromClient.readLine();
        if(requestMessageLine == null){
            return; // ignore null requests
        }
        DEBUG("Request " + reqCount + ": " + requestMessageLine);

        if(requestMessageLine == null){
            throw new Exception();
        }

        // process the request
        String[] request = requestMessageLine.split("\\s");
        
        if (request.length < 2 || !request[0].equals("GET"))
        {
            outputError(500, "Bad request");
            return;
        }

        // parse URL to retrieve file name
        urlName = request[1];
        
        if ( urlName.startsWith("/") == true )
            urlName  = urlName.substring(1);

       // question mark means there's a query string
        if(urlName.contains("?")){
            System.out.println("Detected question mark");
            String[]tokens = urlName.split("\\?");
            urlName = tokens[0];
            StringBuilder queryStringBuilder = new StringBuilder();
            for(int i = 1 ; i < tokens.length; i++){
                queryStringBuilder.append(tokens[i]);
            }
            QUERY_STRING = queryStringBuilder.toString();
        }

        // Parse all headers, line by line
        String line = inFromClient.readLine();
        ifModifiedSince = null;
        while (line != null && !line.equals("") ) {
            
            // separate line by first space
            String[] tokens = line.split("\\s");
            StringBuilder sb = new StringBuilder();
            for(int i = 1; i < tokens.length; i++){
                sb.append(tokens[i]);
                if(i + 1 != tokens.length)
                    sb.append(" ");
            }
            String remaining = sb.toString();

            // switch based off first param
            if(tokens.length >= 2 && tokens[0].equals("Host:")){

            // User specified a host
            for(VirtualHost v : virtualHosts){
                if(v.serverName.equals(tokens[1])){
                    WWW_ROOT = v.documentRoot;
                    serverName = v.serverName;
                }
            }
            }else if (tokens.length >= 2 && tokens[0].equals("If-Modified-Since:")){
                // try to parse if-modified-since date
                try{
                    ifModifiedSince = LocalDateTime.parse(remaining, 
                        DateTimeFormatter.RFC_1123_DATE_TIME).atZone(ZoneId.of("GMT"));
                }catch(Exception e){
                    ifModifiedSince = null; // couldn't parse it
                    System.out.println("Couldn't parse datetime: " + remaining);
                }
            }else if (tokens.length >= 2 && tokens[0].equals("User-Agent:")){

                // user browsing on mobile?
                if(remaining.toLowerCase().contains("iphone")){
                    usingMobile = true;
                }
            }
            line = inFromClient.readLine(); // get next line
        }


        if(!WWW_ROOT.substring(WWW_ROOT.length() - 1).equals("/")){
            WWW_ROOT = WWW_ROOT + "/";
        }
       
        // map to file name
        fileName = WWW_ROOT + urlName;
        fileInfo = new File( fileName );

        // if blank, try mobile and regular index.html
        if (!fileInfo.isFile() && urlName.equals("") && usingMobile){
            urlName = "index_m.html";
            fileName = WWW_ROOT + urlName;
            fileInfo = new File(fileName);
        }

        if(!fileInfo.isFile() && urlName.equals("")){
            urlName = "index.html";
            fileName = WWW_ROOT + urlName;
            fileInfo = new File(fileName);
        }
        
        if(!fileInfo.isFile()){
            outputError(404,  "Not Found");
            fileInfo = null;
        }

    } // end mapURL2file


    private void outputResponseHeader() throws Exception 
    {
        outToClient.writeBytes("HTTP/1.0 200 Document Follows\r\n");
        outToClient.writeBytes("Date: " + DateTimeFormatter.RFC_1123_DATE_TIME
            .format(ZonedDateTime.now(ZoneId.of("GMT"))) + "\r\n");

        if(serverName != null){
            outToClient.writeBytes("Server: " + serverName + "\r\n");
        }

        outToClient.writeBytes("Last-Modified: " + DateTimeFormatter
            .RFC_1123_DATE_TIME.format(lastModifiedZdt) + "\r\n");

        if (urlName.endsWith(".jpg"))
            outToClient.writeBytes("Content-Type: image/jpeg\r\n");
        else if (urlName.endsWith(".gif"))
            outToClient.writeBytes("Content-Type: image/gif\r\n");
        else if (urlName.endsWith(".html") || urlName.endsWith(".htm"))
            outToClient.writeBytes("Content-Type: text/html\r\n");
        else
            outToClient.writeBytes("Content-Type: text/plain\r\n");
    }

    private void outputResponseBody() throws Exception 
    {
        if(fileInfo.canExecute()){
            runCGI();
            return;
        }

        int numOfBytes = (int) fileInfo.length();
        outToClient.writeBytes("Content-Length: " + numOfBytes + "\r\n");
        outToClient.writeBytes("\r\n");


        // look in cache, else read from file
        byte[] fileInBytes;
        if(WebServer.cache.hasFile(fileName) && WebServer.cache
            .cachedTimeMillis(fileName) > fileInfo.lastModified()){
            fileInBytes = WebServer.cache.getFile(fileName);
        }
        else { // cache miss
            // read in file
            FileInputStream fileStream  = new FileInputStream (fileName);
            fileInBytes = new byte[numOfBytes];
            fileStream.read(fileInBytes);

            // cache result
            WebServer.cache.cacheIfPossible(fileName, fileInBytes);
        }

        outToClient.write(fileInBytes, 0, numOfBytes);
    }

    private void runCGI() throws Exception{

        // build process and set query string in environment variable
        ProcessBuilder pb = new ProcessBuilder(fileName);
        Map<String, String> env = pb.environment();
        if(QUERY_STRING != null){
            env.put("QUERY_STRING", QUERY_STRING);
        }

        // funnel into the pipe 1024 bytes at a time
        InputStream procOut = pb.start().getInputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = procOut.read(buffer)) != -1)
        {
            outToClient.write(buffer, 0, bytesRead);
        }
    }

    void outputError(int errCode, String errMsg)
    {
        try {
            outToClient.writeBytes("HTTP/1.0 " + errCode + " " + errMsg + "\r\n");
        } catch (Exception e) {}
    }

    void outputNotModified()
    {
        try {
            outToClient.writeBytes("HTTP/1.0 304 Not Modified\r\n");
        } catch (Exception e) {}
    }    

    static void DEBUG(String s) 
    {
       if (_DEBUG)
          System.out.println( s );
    }
}