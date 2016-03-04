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

class AsyncWebRequestHandler {

    static boolean _DEBUG = true;
    static int     reqCount = 0;

    String WWW_ROOT;
    String serverName;
    List<VirtualHost> virtualHosts;
    SocketChannel connSocket;

    String urlName;
    String fileName;
    File fileInfo;

    ByteBuffer outBuff;
    ByteBuffer inBuff;

    BufferedReader reader;
    StringBuilder writer;

    public AsyncWebRequestHandler(ByteBuffer inBuff, ByteBuffer outBuff,
                 List<VirtualHost> virtualHosts) throws IOException
    {
        reqCount ++;

        this.virtualHosts = virtualHosts;

        this.inBuff = inBuff;
        this.outBuff = outBuff;



        reader = new BufferedReader(new StringReader(new String(inBuff.array(), "US-ASCII")));
        writer = new StringBuilder();

    }

    public void processRequest() 
    {

        try {
            mapURL2File();

            if ( fileInfo != null ) // found the file and knows its info
            {
                outputResponseHeader();
                outputResponseBody();
            } // do not handle error


        } catch (Exception e) {
            outputError(400, "Server error");
        }

        try{
            outBuff.put(writer.toString().getBytes("US-ASCII"));
            System.out.println("WROTE TO BUFFER: " + writer.toString());
        }catch(Exception e){
            e.printStackTrace();
        }

    } // end of processRequest

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
            outputError(500, "Bad request");
            return;
        }

        // parse URL to retrieve file name
        urlName = request[1];
        
        if ( urlName.startsWith("/") == true )
           urlName  = urlName.substring(1);

        // Did the request specify a host? 
        String line = reader.readLine();
        while ( !line.equals("") ) {
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
    }

    private void outputResponseBody() throws IOException 
    {

        int numOfBytes = (int) fileInfo.length();
        writer.append("Content-Length: " + numOfBytes + "\r\n");
        writer.append("\r\n");

    
        // send file content
        FileInputStream fileStream  = new FileInputStream (fileName);
    
        byte[] fileInBytes = new byte[numOfBytes];
        fileStream.read(fileInBytes);


        outBuff.put(fileInBytes);
    
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