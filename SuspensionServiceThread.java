/*
 * Code adopted from:
 *
 * CS433/533 Demo
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class SuspensionServiceThread extends Thread {

    List<Socket> sockets;
    List<VirtualHost> virtualHosts;

    public SuspensionServiceThread(List<Socket> sockets, List<VirtualHost> virtualHosts) {
    	this.virtualHosts = virtualHosts;
	    this.sockets = sockets;
    }
  
    public void run() {
    	

	    System.out.println("Thread " + this + " started.");

	    // busy-wait forever
	    while (true) {
	        // ready to serve
	        Socket s = null;

	        // wait our turn to get access
	        synchronized(sockets){

	        	// wait until socket pool becomes non-empty
	        	while(sockets.isEmpty()){
	        		try{
	        			Debug.DEBUG("Thread " + this + " went to sleep");
	        			sockets.wait();
	        		}catch(Exception e){
	        			e.printStackTrace();
	        		}

	        		Debug.DEBUG("Thread " + this + " woke up");
	        	}

	        	s = (Socket) sockets.remove(0);

	        	// update number of users for load balancing
	        	WebServer.numUsers++;
	        }

	        try{
	        	// process the request
				WebRequestHandler wrh = new WebRequestHandler( s, 
					WebServer.listenSocket, virtualHosts );
				wrh.processRequest();

			}catch (Exception e){
				e.printStackTrace();
			}

			// update number of users
			WebServer.numUsers--;
		}
    } // end run
} // end ServiceThread