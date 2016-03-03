/**
 ** Code adopted from:
 ** 
 ** Yale CS433/533 Demo Basic Web Server
 **/
import java.io.*;
import java.net.*;
import java.util.*;

class SequentialServer{
	
    public static int serverPort = 6789;
    public static List<VirtualHost> virtualHosts = new ArrayList<VirtualHost>();
    //public static String WWW_ROOT = "/home/httpd/html/zoo/classes/cs433/";
    public static String WWW_ROOT = "./";

    public static void main(String args[]) throws Exception  {
	

	 ConfigParser cp = null;

	 // TODO make sure -config is set!
	 if (args.length >= 2){
	 	try{
	 		cp = new ConfigParser(args[1]);
	 		virtualHosts = cp.virtualHosts;
	 		if(cp.port != -1)
	 			serverPort = cp.port;
	 	}catch(Exception e){
	 		System.out.println("Could not load configurations: " + args[1]);
	 		e.printStackTrace();
	 	}
	 }

	// create server socket
	ServerSocket listenSocket = new ServerSocket(serverPort);
	System.out.println("server listening at: " + listenSocket);
	for (VirtualHost v : virtualHosts){
		System.out.println("host www root: " + v.documentRoot);
	}

	while (true) {

	    try {

		    // take a ready connection from the accepted queue
		    Socket connectionSocket = listenSocket.accept();
		    System.out.println("\nReceive request from " + connectionSocket);
	
		    // process a request
		    WebRequestHandler wrh = 
		        new WebRequestHandler( connectionSocket, virtualHosts );

		    wrh.processRequest();

	    } catch (Exception e)
		{
			e.printStackTrace();
		}
	} // end of while (true)
	
    } // end of main

} // end of class WebServer