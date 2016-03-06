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

    public AsyncWebRequestHandler(StringBuffer inBuff, ByteBuffer outBuff,
                 List<VirtualHost> virtualHosts) throws Exception
    {
        super(null, virtualHosts);

        this.inBuff = inBuff;
        this.outBuff = outBuff;

        inFromClient = new BufferedReader(new StringReader(inBuff.toString()));
        writer = new StringBuilder();

    }

    public void processRequest() 
    {

        try {
            mapURL2File();

            if ( fileInfo != null ) // found the file and knows its info
            {
                System.out.println("Printing response");
                outputResponseHeader();
                outputResponseBody();
            } // do not handle error

        } catch (Exception e) {
            outputError(400, "Server error");
            e.printStackTrace();
        }

        try{
            outBuff.put(writer.toString().getBytes("US-ASCII"));
        }catch(Exception e){
            System.out.println("Couldn't write to out buffer: " + writer.toString());
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
            System.out.println("Error continuing to send large file");
            e.printStackTrace();
        }
    }
    */
    /*
    private void mapURL2File() throws IOException 
    {
        // Configure default host
        if(!virtualHosts.isEmpty()){
            this.WWW_ROOT = virtualHosts.get(0).documentRoot;
        }else{
            throw new IOException();
        }

        String requestMessageLine = reader.readLine();
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
        String line = reader.readLine();
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
          line = reader.readLine();
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

        int fileSize = (int) fileInfo.length();
        writer.append("Content-Length: " + fileSize + "\r\n");
        writer.append("\r\n");

        flushWriter();
    
        // send file content
        fileStream  = new FileInputStream (fileName);
    
        if(fileSize > outBuff.remaining()){
            byte [] batch = new byte[outBuff.remaining()];
            fileStream.read(batch);
            outBuff.put(batch);
            doneProcessing = false;
        }else{
            byte[] fileInBytes = new byte[fileSize];
            fileStream.read(fileInBytes);
            outBuff.put(fileInBytes);
            doneProcessing = true;
        }
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

    static void DEBUG(String s) 
    {
       if (_DEBUG)
          System.out.println( s );
    }
}