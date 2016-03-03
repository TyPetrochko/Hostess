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

	    System.out.println("Thread " + this + " started.");
	    while (true) {
	        // get a new request connection
	        Socket s = null;

	        synchronized (welcomeSocket) {         
		        try {
		            s = welcomeSocket.accept();
		        } catch (IOException e) {
		        	System.out.println("Thread " + this 
		        		+ " encountered trouble accepting sockets");
		        	e.printStackTrace();
		        }
	        } // end of extract a request

	        try{
				WebRequestHandler wrh = new WebRequestHandler( s, virtualHosts );
				wrh.processRequest();
			}catch (Exception e){
				e.printStackTrace();
			}
			
	    } // end while
		
    } // end run
} // end ServiceThread