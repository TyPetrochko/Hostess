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

        /* we inherit from the non-async web request handler to avoid
        ** reimplementing some of the longer methods
        */
        super(null, null, virtualHosts);

        this.inBuff = inBuff;
        this.outBuff = outBuff;
        this.remoteAddress = remoteAddress;
        this.serverAddress = serverAddress;
        this.port = port;

        // keep number of handlers around for crude load-balancing
        numHandlers++;

        // buffer reading
        inFromClient = new BufferedReader(new StringReader(inBuff.toString()));

        // keep track of whether we're done reading from the client
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
                // some error occurred, but it has already been handled
                doneProcessing = true;
            }

        } catch (Exception e) {
            // some error has occurred, which hasn't been handled yet
            try{
                fileStream.close();
            }catch (Exception ee){
                System.err.println("Could not close file stream");
                ee.printStackTrace();
            }

            // output a 400 error
            outputError(400, "Server error");
            doneProcessing = true;

            if(Debug.DEBUG)
                e.printStackTrace();
        }

    } // end of processRequest

    // write basic response header based off of file type
    private void outputResponseHeader() throws IOException 
    {

        write("HTTP/1.0 200 Document Follows\r\n");
        write("Date: " 
            + new SimpleDateFormat("EEEE, dd MMM HH:mm:ss z").format(new Date()) + "\r\n");

        if(serverName != null){
            write("Server: " + serverName + "\r\n");
        }

        // track last-modified header
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

    // output the response body, potentially resulting in errors
    private void outputResponseBody() throws IOException 
    {
        if(fileInfo.canExecute()){
            Debug.DEBUG("Running CGI script");
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

        // how large is the file?
        int fileSize = (int) fileInfo.length();
        write("Content-Length: " + fileSize + "\r\n");
        write("\r\n");

        /* here, we only cache files if we can handle the file in a single
        ** pass AND the file is small enough to cache.
        */
        if(fileSize > outBuff.remaining()){ // not enough space in buffer

            // write until buffer is full, and finish on next pass
            fileStream  = new FileInputStream (fileName);
            byte [] batch = new byte[outBuff.remaining()];
            fileStream.read(batch);
            outBuff.put(batch);

            // keep track of how much we've written
            doneProcessing = false;
            progress += batch.length;
        }else{
            // we want to check cache first
            byte[] fileInBytes;
            if(FileCache.globalCache.hasFile(fileName) && FileCache.globalCache
                .cachedTimeMillis(fileName) > fileInfo.lastModified()){
                // cache hit, skip file I/O
                Debug.DEBUG("Cache hit: " + fileName);
                fileInBytes = FileCache.globalCache.getFile(fileName);
            }else {
                
                // cache miss
                Debug.DEBUG("Cache miss: " + fileName);

                // handle file I/O
                fileStream  = new FileInputStream (fileName);
                fileInBytes = new byte[fileSize];
                fileStream.read(fileInBytes);
                fileStream.close();

                // cache result
                FileCache.globalCache.cacheIfPossible(fileName, fileInBytes);
            }

            // we have output the whole file, so we won't need another pass
            outBuff.put(fileInBytes);
            doneProcessing = true;
        }
    }

    /* in a previous pass through the file, we couldn't load the whole file
    ** into the buffer, so keep processing
    */
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

        // again, check if the remaining chunks of file will fit in buffer
        if (fileSize - progress > outBuff.remaining()){
            byte [] batch = new byte[outBuff.remaining()];
            fileStream.read(batch);
            outBuff.put(batch);
            doneProcessing = false; // still not done yet
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

    // run a CGI script
    private void runCGI() throws Exception{

        // build process and set query string in environment variable
        ProcessBuilder pb = new ProcessBuilder(fileName);

        // set query string
        Map<String, String> env = pb.environment();
        if(QUERY_STRING != null){
            env.put("QUERY_STRING", QUERY_STRING);
        }

        // set remaining environment variables
        setEnvironmentVariables(env);

        // funnel into the pipe 1024 bytes at a time
        cgiInput = pb.start().getInputStream();
        cgiBuffer = new byte[1024];

        // track how much data we've gone through
        long batchProgress = 0;
        while ((cgiBytesRead = cgiInput.read(cgiBuffer)) != -1)
        {
            if(outBuff.remaining() < cgiBytesRead){
                // save buffer state, don't go any further
                doneProcessing = false;
                progress += batchProgress;
                return;
            }else{
                // write some data
                outBuff.put(cgiBuffer, 0, (int)cgiBytesRead);
            }
        }

        // we're done processing
        doneProcessing = true;
    }

    // we ran out of buffer space; continue running CGI
    private void continueRunningCGI() throws Exception{
        Debug.DEBUG("Finishing CGI again");
        
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
                // write some bytes
                outBuff.put(cgiBuffer, 0, (int)cgiBytesRead);
                batchProgress += cgiBytesRead;
            }
        }

        // done processing
        progress += batchProgress;
        doneProcessing = true;
    }

    // send an error code to client
    void outputError(int errCode, String errMsg)
    {
        try {
            write("HTTP/1.0 " + errCode + " " + errMsg + "\r\n");
            Debug.DEBUG("Could not write to file!");
        } catch (Exception e) {}
    }

    // respond that file hasn't been modified
    void outputNotModified()
    {
        Debug.DEBUG("Well, it wasn't modified!");
        try {
            write("HTTP/1.0 304 Not Modified\r\n");
        } catch (Exception e) {}
    }

    // write some data to file
    void write(String s){
        try{
            outBuff.put(s.getBytes("US-ASCII"));
        }catch (Exception e){
            Debug.DEBUG("Couldn't write to output buffer");
            e.printStackTrace();
        }
    }

    // perform loadbalancing (called from WebRequestHandler)
    void loadBalance(){
        if(LoadBalancer.loadBalancerSingleton != null){

            // send some paramaters to load balancer
            Map<String, Object> statusVars = new HashMap<String, Object>();
            statusVars.put("numUsers", numHandlers);
            
            // send load balancer result
            try{
                if(LoadBalancer.loadBalancerSingleton
                    .canAcceptNewConnections(statusVars)){
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