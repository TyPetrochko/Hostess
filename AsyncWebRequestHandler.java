/**
 ** Code adopted from:
 **
 ** Yale CS433/533 Demo Basic Web Server
 **/
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.nio.*;
import java.nio.channels.*;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;

class AsyncWebRequestHandler extends WebRequestHandler {

    public static int numHandlers = 0;

    ByteBuffer outBuff;
    StringBuffer inBuff;

    FileInputStream fileStream;

    boolean doneProcessing;
    long progress; // how many bytes we've read through

    byte[] cgiBuffer;
    InputStream cgiInput;
    long cgiBytesRead;

    public AsyncWebRequestHandler(InetAddress remoteAddress, 
        InetAddress serverAddress, int port,
        StringBuffer inBuff, ByteBuffer outBuff, 
        List<VirtualHost> virtualHosts) throws Exception
    {
        super(null, null, virtualHosts);

        this.inBuff = inBuff;
        this.outBuff = outBuff;

        this.remoteAddress = remoteAddress;

        this.serverAddress = serverAddress;

        this.port = port;

        numHandlers++;

        inFromClient = new BufferedReader(new StringReader(inBuff.toString()));

        doneProcessing = false;
        progress = 0;

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
                if(isLoadBalancing){
                    loadBalance();
                }else if(ifModifiedSince != null && ifModifiedSince.compareTo(lastModifiedZdt) > 0){
                    outputNotModified();
                }else{
                    outputResponseHeader();
                    outputResponseBody();
                }
            } else{
                // some error occurred
                doneProcessing = true;
            }

        } catch (Exception e) {
            try{
                fileStream.close();
            }catch (Exception ee){
                System.err.println("Could not close file stream");
                ee.printStackTrace();
            }
            outputError(400, "Server error");
            Debug.DEBUG("We encountered error but we're done processing!");
            doneProcessing = true;
            e.printStackTrace();
        }

    } // end of processRequest

    /*
    public boolean continueProcessing(){

        try{
            // send file content
            byte [] batch = new byte[outBuff.remaining()];
            int isComplete = fileStream.read(batch);
            outBuff.put(batch);
            if(isComplete == -1){
                doneProcessing = true;
            }else{
                doneProcessing = false;
            }
        }catch (Exception e) {
            Debug.DEBUG("Error continuing to send large file");
            e.printStackTrace();
        }
    }
    */
    /*
    public void mapURL2File() throws IOException 
    {
        // Configure default host
        if(!virtualHosts.isEmpty()){
            this.WWW_ROOT = virtualHosts.get(0).documentRoot;
        }else{
            throw new IOException();
        }

        String requestMessageLine = inFromClient.readLine();
        DEBUG("Request " + reqCount + ": " + requestMessageLine);

        // process the request
        String[] request = requestMessageLine.split("\\s");
        
        if (request.length < 2 || !request[0].equals("GET"))
        {
            outputError(500, "Bad request: "+ requestMessageLine);
            return;
        }

        // parse URL to retrieve file name
        urlName = request[1];
        
        if ( urlName.startsWith("/") == true )
           urlName  = urlName.substring(1);

        // Did the request specify a host? 
        String line = inFromClient.readLine();
        while (line != null && !line.equals("") ) {
          String[] tokens = line.split("\\s");
          if(tokens.length >= 2 && tokens[0].equals("Host:")){
            
            // User specified a host
            for(VirtualHost v : virtualHosts){
                if(v.serverName.equals(tokens[1])){
                    WWW_ROOT = v.documentRoot;
                    serverName = v.serverName;
                }
            }
          }
          line = inFromClient.readLine();
        }

        // System.out.print();ut optional slash at end
        if(!WWW_ROOT.substring(WWW_ROOT.length() - 1).equals("/")){
            WWW_ROOT = WWW_ROOT + "/";
        }
       

        // map to file name
        fileName = WWW_ROOT + urlName;
        DEBUG("Map to File name: " + fileName);

        fileInfo = new File( fileName );
        if ( !fileInfo.isFile() ) 
        {
            outputError(404,  "Not Found");
            fileInfo = null;
        }

    } // end mapURL2file
    */
    


    private void outputResponseHeader() throws IOException 
    {
        /*
            outToClient.writeBytes("HTTP/1.0 200 Document Follows\r\n");
            outToClient.writeBytes("Date: " 
                + new SimpleDateFormat("EEEE, dd MMM HH:mm:ss z").format(new Date()) + "\r\n");

            if(serverName != null){
                outToClient.writeBytes("Server: " + serverName + "\r\n");
            }

            if (urlName.endsWith(".jpg"))
                outToClient.writeBytes("Content-Type: image/jpeg\r\n");
            else if (urlName.endsWith(".gif"))
                outToClient.writeBytes("Content-Type: image/gif\r\n");
            else if (urlName.endsWith(".html") || urlName.endsWith(".htm"))
                outToClient.writeBytes("Content-Type: text/html\r\n");
            else
                outToClient.writeBytes("Content-Type: text/plain\r\n");
            */

        write("HTTP/1.0 200 Document Follows\r\n");
        write("Date: " 
            + new SimpleDateFormat("EEEE, dd MMM HH:mm:ss z").format(new Date()) + "\r\n");

        if(serverName != null){
            write("Server: " + serverName + "\r\n");
        }

        write("Last-Modified: " + DateTimeFormatter
            .RFC_1123_DATE_TIME.format(lastModifiedZdt) + "\r\n");

        if (urlName.endsWith(".jpg"))
            write("Content-Type: image/jpeg\r\n");
        else if (urlName.endsWith(".gif"))
            write("Content-Type: image/gif\r\n");
        else if (urlName.endsWith(".html") || urlName.endsWith(".htm"))
            write("Content-Type: text/html\r\n");
        else
            write("Content-Type: text/plain\r\n");
    }

    private void outputResponseBody() throws IOException 
    {
        if(fileInfo.canExecute()){
            Debug.DEBUG("Run cgi script now!");
            try{
                isExecutable = true;
                runCGI();
            }catch (Exception e){
                System.err.println("Can't run CGI script");
                e.printStackTrace();
            }
            return;
        }else{
            isExecutable = false;
        }
        int fileSize = (int) fileInfo.length();
        write("Content-Length: " + fileSize + "\r\n");
        write("\r\n");

        // only cache files small enough to cache
        
        if(fileSize > outBuff.remaining()){
            if (fileStream == null)
                fileStream  = new FileInputStream (fileName);
            Debug.DEBUG("Not done yet");
            byte [] batch = new byte[outBuff.remaining()];
            fileStream.read(batch);
            outBuff.put(batch);
            doneProcessing = false;
            progress += batch.length;
        }else{
            // we want to check cache first
            byte[] fileInBytes;
            fileStream  = new FileInputStream (fileName);
            if(FileCache.globalCache.hasFile(fileName) && FileCache.globalCache
                .cachedTimeMillis(fileName) > fileInfo.lastModified()){
                // cache hit
                Debug.DEBUG("Cache hit!");
                fileInBytes = FileCache.globalCache.getFile(fileName);
            }
            else {
                // cache miss; read in file
                Debug.DEBUG("Cache miss :(");
                
                fileInBytes = new byte[fileSize];
                fileStream.read(fileInBytes);
                fileStream.close();

                // cache result
                FileCache.globalCache.cacheIfPossible(fileName, fileInBytes);
            }

            outBuff.put(fileInBytes);
            doneProcessing = true;
        }
    }

    public boolean continueProcessing() throws Exception{
        // assume that outBuff has been cleared
        if(outBuff.remaining() <= 0){
            System.err.println("Outbuff hasn't been cleared");
            throw new Exception();
        }

        if(isExecutable){
            continueRunningCGI();
            return doneProcessing;
        }

        int fileSize = (int) fileInfo.length();

        if (fileSize - progress > outBuff.remaining()){
            byte [] batch = new byte[outBuff.remaining()];
            fileStream.read(batch);
            outBuff.put(batch);
            doneProcessing = false;
            progress += batch.length;
        }else{
            byte[] fileInBytes = new byte[fileSize - (int)progress];
            fileStream.read(fileInBytes);
            outBuff.put(fileInBytes);
            fileStream.close();
            doneProcessing = true;
        }

        return doneProcessing;
    }

    private void runCGI() throws Exception{
        Debug.DEBUG("Running CGI first time");

        // build process and set query string in environment variable
        ProcessBuilder pb = new ProcessBuilder(fileName);
        Map<String, String> env = pb.environment();
        if(QUERY_STRING != null){
            env.put("QUERY_STRING", QUERY_STRING);
        }

        setEnvironmentVariables(env);

        // funnel into the pipe 1024 bytes at a time
        cgiInput = pb.start().getInputStream();
        cgiBuffer = new byte[1024];

        long batchProgress = 0;
        while ((cgiBytesRead = cgiInput.read(cgiBuffer)) != -1)
        {
            if(outBuff.remaining() < cgiBytesRead){
                // save buffer state, don't go any further
                doneProcessing = false;
                progress += batchProgress;
                return;
            }else{
                outBuff.put(cgiBuffer, 0, (int)cgiBytesRead);
            }
        }
        doneProcessing = true;
    }

    private void continueRunningCGI() throws Exception{
        Debug.DEBUG("Running CGI again");
        
        // flush remaining bytes left from last pass
        outBuff.put(cgiBuffer, 0, (int)cgiBytesRead);

        long batchProgress = 0;
        while ((cgiBytesRead = cgiInput.read(cgiBuffer)) != -1)
        {
            if(outBuff.remaining() < cgiBytesRead){
                // save buffer, don't go any further
                doneProcessing = false;
                progress += batchProgress;
                return;
            }else{
                outBuff.put(cgiBuffer, 0, (int)cgiBytesRead);
                batchProgress += cgiBytesRead;
            }
        }
        progress += batchProgress;
        doneProcessing = true;
    }

    void outputError(int errCode, String errMsg)
    {
        try {
            write("HTTP/1.0 " + errCode + " " + errMsg + "\r\n");
            Debug.DEBUG("Could not write to file!");
        } catch (Exception e) {}
    }

    void outputNotModified()
    {
        Debug.DEBUG("Well, it wasn't modified!");
        try {
            write("HTTP/1.0 304 Not Modified\r\n");
        } catch (Exception e) {}
    }

    void write(String s){
        try{
            outBuff.put(s.getBytes("US-ASCII"));
        }catch (Exception e){
            Debug.DEBUG("Couldn't write to output buffer");
            e.printStackTrace();
        }
    }

    void loadBalance(){
        if(LoadBalancer.loadBalancerSingleton != null){
            Map<String, Object> statusVars = new HashMap<String, Object>();
            statusVars.put("numUsers", numHandlers);
            try{
                if(LoadBalancer.loadBalancerSingleton.canAcceptNewConnections(statusVars)){
                    write("HTTP/1.0 200 OK\r\n");
                }else{
                    write("HTTP/1.0 503 Service Unavailable\r\n");
                }
            }catch (Exception e){
                System.err.println("Error using load balancer");
                e.printStackTrace();
            }
        }
        doneProcessing = true;
    }

    static void DEBUG(String s) 
    {
       if (_DEBUG)
          Debug.DEBUG( s );
    }

    public boolean isDoneProcessing(){
        return doneProcessing;
    }
}