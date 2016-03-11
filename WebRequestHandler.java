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
    ServerSocket listenSocket;
    Socket connSocket;
    List<VirtualHost> virtualHosts;
    BufferedReader inFromClient;
    DataOutputStream outToClient;

    String urlName;
    String fileName;
    File fileInfo;

    ZonedDateTime ifModifiedSince;
    ZonedDateTime lastModifiedZdt;

    boolean isExecutable;
    boolean isLoadBalancing = false;
    boolean usingMobile;

    InetAddress remoteAddress;
    InetAddress serverAddress;
    int port;


    public WebRequestHandler(Socket connectionSocket, 
        ServerSocket listenSocket, 
        List<VirtualHost> virtualHosts) throws Exception
    {
        reqCount ++;

        this.virtualHosts = virtualHosts;

        this.connSocket = connectionSocket;

        this.listenSocket = listenSocket;

        if (connectionSocket != null)
            this.remoteAddress = connectionSocket.getInetAddress();

        if (listenSocket != null){
            this.serverAddress = listenSocket.getInetAddress();
            this.port = listenSocket.getLocalPort();
        }
        


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
            if (isLoadBalancing){
                loadBalance();
            }else if(ifModifiedSince != null && ifModifiedSince.compareTo(lastModifiedZdt) > 0){
                outputNotModified();
                return;
            }else{
                outputResponseHeader();
                outputResponseBody();
            }
        } // do not handle error

        
    } catch (Exception e) {
        Debug.DEBUG("Encountered error, sending report and closing socket");
        e.printStackTrace();
        outputError(400, "Server error");
    } finally {
        try{
            connSocket.close();
        }catch (Exception e){
            Debug.DEBUG("Couldn't close socket");
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
            Debug.DEBUG("Detected question mark");
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
                    Debug.DEBUG("Couldn't parse datetime: " + remaining);
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

        // we want index.html
        if(!fileInfo.isFile() && urlName.equals("")){
            urlName = "index.html";
            fileName = WWW_ROOT + urlName;
            fileInfo = new File(fileName);
        }
        
        // we want to load balancer
        if(!fileInfo.isFile() && urlName.equalsIgnoreCase("load")){
            isLoadBalancing = true;
        }else if(!fileInfo.isFile()){

            // file missing
            outputError(404,  "Not Found");
            fileInfo = null;
        }

    } // end mapURL2file

    // output all response headers to client for this request
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

    // output everything after headers to client
    private void outputResponseBody() throws Exception 
    {

        // check if CGI
        if(fileInfo.canExecute()){
            isExecutable = true;
            runCGI();
            return;
        }else{
            isExecutable = false;
        }

        // get file length
        int numOfBytes = (int) fileInfo.length();
        outToClient.writeBytes("Content-Length: " + numOfBytes + "\r\n");
        outToClient.writeBytes("\r\n");


        // look in cache, else read from file
        byte[] fileInBytes;
        if(FileCache.globalCache.hasFile(fileName) && FileCache.globalCache
            .cachedTimeMillis(fileName) > fileInfo.lastModified()){
            // cache hit
            Debug.DEBUG("Cache hit!");
            fileInBytes = FileCache.globalCache.getFile(fileName);
        }
        else {
            // cache miss
            Debug.DEBUG("Cache miss :(");
            FileInputStream fileStream  = new FileInputStream (fileName);
            fileInBytes = new byte[numOfBytes];
            fileStream.read(fileInBytes);

            // cache result
            FileCache.globalCache.cacheIfPossible(fileName, fileInBytes);
        }

        try{
            outToClient.write(fileInBytes, 0, numOfBytes);
        }catch(Exception e){
            // client timed out, swallow error
            throw e;
        }
    }

    private void runCGI() throws Exception{

        // build process and set query string in environment variable
        ProcessBuilder pb = new ProcessBuilder(fileName);
        Map<String, String> env = pb.environment();
        if(QUERY_STRING != null){
            env.put("QUERY_STRING", QUERY_STRING);
        }

        // set the remainder of environment variables
        setEnvironmentVariables(env);

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

    // perform load balancing
    void loadBalance(){
        if(LoadBalancer.loadBalancerSingleton != null){

            // make some status variables
            Map<String, Object> statusVars = new HashMap<String, Object>();
            if(WebServer.isOverloaded){
                statusVars.put("isOverloaded", null);
            }
            statusVars.put("numUsers", WebServer.numUsers);
            statusVars.put("maxUsers", WebServer.maxUsers);
            statusVars.put("maxLoadFactor", WebServer.maxLoadFactor);

            // determine if we can handle more users
            try{
                if(LoadBalancer.loadBalancerSingleton.canAcceptNewConnections(statusVars)){
                    outToClient.writeBytes("HTTP/1.0 200 OK\r\n");
                }else{
                    outToClient.writeBytes("HTTP/1.0 503 Service Unavailable\r\n");
                }
            }catch (Exception e){
                System.err.println("Error using load balancer");
                e.printStackTrace();
            }
        }
    }

    public void setEnvironmentVariables(Map<String, String> env){

        String REMOTE_ADDR = remoteAddress.getHostAddress();
        String REMOTE_HOST = remoteAddress.getHostName();
        
        String REQUEST_METHOD = "GET";
        String SERVER_NAME = serverAddress.getHostAddress();
        String SERVER_PORT = port + "";
        String SERVER_PROTOCOL = "HTTP/1.0";

        env.put("REMOTE_ADDR", REMOTE_ADDR);
        env.put("REMOTE_HOST", REMOTE_HOST);
        env.put("REQUEST_METHOD", REQUEST_METHOD);
        env.put("SERVER_NAME", SERVER_NAME);
        env.put("SERVER_PORT", SERVER_PORT);
        env.put("SERVER_PROTOCOL", SERVER_PROTOCOL);

    }

    static void DEBUG(String s) 
    {
       if (_DEBUG)
          Debug.DEBUG( s );
    }
}