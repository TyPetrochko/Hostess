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

    ByteBuffer outBuff;
    StringBuffer inBuff;

    StringBuilder writer;
    FileInputStream fileStream;

    boolean doneProcessing;
    long progress; // how many bytes we've read through

    byte[] cgiBuffer;
    InputStream cgiInput;
    long cgiBytesRead;

    public AsyncWebRequestHandler(StringBuffer inBuff, ByteBuffer outBuff,
                 List<VirtualHost> virtualHosts) throws Exception
    {
        super(null, virtualHosts);

        this.inBuff = inBuff;
        this.outBuff = outBuff;

        inFromClient = new BufferedReader(new StringReader(inBuff.toString()));
        writer = new StringBuilder();

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
                if(ifModifiedSince != null && ifModifiedSince.compareTo(lastModifiedZdt) > 0){
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
            outputError(400, "Server error");
            System.out.println("We encountered error but we're done processing!");
            doneProcessing = true;
            //e.printStackTrace();
        }

        flushWriter();
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
            System.out.println("Error continuing to send large file");
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

        writer.append("HTTP/1.0 200 Document Follows\r\n");
        writer.append("Date: " 
            + new SimpleDateFormat("EEEE, dd MMM HH:mm:ss z").format(new Date()) + "\r\n");

        if(serverName != null){
            writer.append("Server: " + serverName + "\r\n");
        }

        writer.append("Last-Modified: " + DateTimeFormatter
            .RFC_1123_DATE_TIME.format(lastModifiedZdt) + "\r\n");

        if (urlName.endsWith(".jpg"))
            writer.append("Content-Type: image/jpeg\r\n");
        else if (urlName.endsWith(".gif"))
            writer.append("Content-Type: image/gif\r\n");
        else if (urlName.endsWith(".html") || urlName.endsWith(".htm"))
            writer.append("Content-Type: text/html\r\n");
        else
            writer.append("Content-Type: text/plain\r\n");

        flushWriter();
    }

    private void outputResponseBody() throws IOException 
    {
        if(fileInfo.canExecute()){
            System.out.println("Run cgi script now!");
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
        writer.append("Content-Length: " + fileSize + "\r\n");
        writer.append("\r\n");

        flushWriter();
    
        // send file content
        fileStream  = new FileInputStream (fileName);
    
        if(fileSize > outBuff.remaining()){
            System.out.println("Not done yet");
            byte [] batch = new byte[outBuff.remaining()];
            fileStream.read(batch);
            outBuff.put(batch);
            doneProcessing = false;
            progress += batch.length;
        }else{
            System.out.println("Done!");
            byte[] fileInBytes = new byte[fileSize];
            fileStream.read(fileInBytes);
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
            doneProcessing = true;
        }

        return doneProcessing;
    }

    private void runCGI() throws Exception{
        System.out.println("Running CGI first time");

        // build process and set query string in environment variable
        ProcessBuilder pb = new ProcessBuilder(fileName);
        Map<String, String> env = pb.environment();
        if(QUERY_STRING != null){
            env.put("QUERY_STRING", QUERY_STRING);
        }

        flushWriter();

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
                System.out.println("Put");
            }
        }
        doneProcessing = true;
    }

    private void continueRunningCGI() throws Exception{
        System.out.println("Running CGI again");
        
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

    void flushWriter(){
        try{
            outBuff.put(writer.toString().getBytes("US-ASCII"));
            writer = new StringBuilder();
        }catch (Exception e) {
            System.out.println("Error flushing StringBuilder to buffer");
            e.printStackTrace();
        }
    }

    void outputError(int errCode, String errMsg)
    {
        try {
            writer.append("HTTP/1.0 " + errCode + " " + errMsg + "\r\n");
            System.out.println("Could not write to file!");
        } catch (Exception e) {}
    }

    void outputNotModified()
    {
        System.out.println("Well, it wasn't modified!");
        try {
            writer.append("HTTP/1.0 304 Not Modified\r\n");
        } catch (Exception e) {}
    }

    static void DEBUG(String s) 
    {
       if (_DEBUG)
          System.out.println( s );
    }

    public boolean isDoneProcessing(){
        return doneProcessing;
    }
}