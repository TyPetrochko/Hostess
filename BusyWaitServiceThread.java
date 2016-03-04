/*
 * Code adopted from:
 *
 * CS433/533 Demo
 */
import java.io.*;
import java.net.*;
import java.util.*;

public class BusyWaitServiceThread extends Thread {

    List<Socket> sockets;
    List<VirtualHost> virtualHosts;

    public BusyWaitServiceThread(List<Socket> sockets, List<VirtualHost> virtualHosts) {
    	this.virtualHosts = virtualHosts;
	    this.sockets = sockets;
    }
  
    public void run() {

	    System.out.println("Thread " + this + " started.");

	    // busy-wait forever
	    while (true) {
	        // ready to serve
	        Socket s = null;

	        // spin until we can get a new socket
	        while (s == null) {
		        synchronized (sockets) {         
		            if (!sockets.isEmpty()) {
			           // remove the first request
			           s = (Socket) sockets.remove(0); 
			           System.out.println("Thread " + this 
					       + " process request " + s);
		            } // end if
		        } // end of sync
	        } // end while
	        try{
				WebRequestHandler wrh = new WebRequestHandler( s, virtualHosts );
				wrh.processRequest();
			}catch (Exception e){
				e.printStackTrace();
			}
		}
    } // end run
} // end ServiceThread