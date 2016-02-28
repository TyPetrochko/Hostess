/**
 ** Code adopted from:
 **
 ** Yale CS433/533 Demo Basic Web Server
 **/
import java.io.*;
import java.net.*;
import java.util.*;

class WebRequestHandler {

    static boolean _DEBUG = true;
    static int     reqCount = 0;

    String WWW_ROOT;
    List<VirtualHost> virtualHosts;
    Socket connSocket;
    BufferedReader inFromClient;
    DataOutputStream outToClient;

    String urlName;
    String fileName;
    File fileInfo;

    public WebRequestHandler(Socket connectionSocket, 
                 List<VirtualHost> virtualHosts) throws Exception
    {
        reqCount ++;

        this.virtualHosts = virtualHosts;

        this.connSocket = connectionSocket;

        inFromClient =
          new BufferedReader(new InputStreamReader(connSocket.getInputStream()));

        outToClient =
          new DataOutputStream(connSocket.getOutputStream());

    }

    public void processRequest() 
    {

    try {
        mapURL2File();

        if ( fileInfo != null ) // found the file and knows its info
        {
            outputResponseHeader();
            outputResponseBody();
        } // dod not handle error

        connSocket.close();
    } catch (Exception e) {
        outputError(400, "Server error");
    }



    } // end of processARequest

    private void mapURL2File() throws Exception 
    {
        // Configure default host
        if(!virtualHosts.isEmpty()){
            this.WWW_ROOT = virtualHosts.get(0).documentRoot;
        }else{
            throw new Exception();
        }

        String requestMessageLine = inFromClient.readLine();
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
        String line = inFromClient.readLine();
        while ( !line.equals("") ) {
          String[] tokens = line.split("\\s");
          if(tokens.length >= 2 && tokens[0].equals("Host:")){
            
            // User specified a host
            for(VirtualHost v : virtualHosts){
                if(v.serverName.equals(tokens[1])){
                    WWW_ROOT = v.documentRoot;
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


    private void outputResponseHeader() throws Exception 
    {
        outToClient.writeBytes("HTTP/1.0 200 Document Follows\r\n");
        outToClient.writeBytes("Set-Cookie: MyCool433Seq12345\r\n");

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

        int numOfBytes = (int) fileInfo.length();
        outToClient.writeBytes("Content-Length: " + numOfBytes + "\r\n");
        outToClient.writeBytes("\r\n");
    
        // send file content
        FileInputStream fileStream  = new FileInputStream (fileName);
    
        byte[] fileInBytes = new byte[numOfBytes];
        fileStream.read(fileInBytes);

        outToClient.write(fileInBytes, 0, numOfBytes);
    }

    void outputError(int errCode, String errMsg)
    {
        try {
            outToClient.writeBytes("HTTP/1.0 " + errCode + " " + errMsg + "\r\n");
        } catch (Exception e) {}
    }

    static void DEBUG(String s) 
    {
       if (_DEBUG)
          System.out.println( s );
    }
}