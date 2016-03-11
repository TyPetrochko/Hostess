/*
 * Code adopted from:
 *
 * CS433/533 Demo
 */
import java.io.*;
import java.net.*;
import java.util.*;

public class CompetingServiceThread extends Thread {

    ServerSocket welcomeSocket;
    List<VirtualHost> virtualHosts;

    public CompetingServiceThread(ServerSocket welcomeSocket, List<VirtualHost> virtualHosts) {
    	this.virtualHosts = virtualHosts;
	    this.welcomeSocket = welcomeSocket;
    }
  
    public void run() {

	    Debug.DEBUG("Thread " + this + " started.");
	    while (true) {
	        // get a new request connection
	        Socket s = null;

	        synchronized (welcomeSocket) {         
		        try {
		        	// track number of users for load balancing
		        	WebServer.numUsers++;
		            s = welcomeSocket.accept();
		        } catch (IOException e) {
		        	Debug.DEBUG("Thread " + this 
		        		+ " encountered trouble accepting sockets");
		        	e.printStackTrace();
		        }
	        } // end of extract a request

	        try{
	        	// process the request
				WebRequestHandler wrh = new WebRequestHandler( s, 
					WebServer.listenSocket, virtualHosts );
				wrh.processRequest();
			}catch (Exception e){
				e.printStackTrace();
			}
			WebServer.numUsers--;
	    } // end while
		
    } // end run
} // end ServiceThread