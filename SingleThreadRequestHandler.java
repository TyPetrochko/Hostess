/**
 ** Code adopted from:
 ** 
 ** Yale CS433/533 Demo Basic Web Server
 **/
import java.io.*;
import java.net.*;
import java.util.*;

// A simple thread request handler
class SingleThreadRequestHandler extends Thread{
	public Socket socket;
	public List<VirtualHost> virtualHosts;
	
	public SingleThreadRequestHandler(Socket socket, List<VirtualHost> virtualHosts){
		this.socket = socket;
		this.virtualHosts = virtualHosts;
	}

	public void run(){
		try{
			// track number of users for load balancing
			WebServer.numUsers ++;

			// process request
			WebRequestHandler wrh = 
			        new WebRequestHandler( socket, WebServer.listenSocket, virtualHosts );
		    wrh.processRequest();

		    // update number of users
		    WebServer.numUsers --;
		}catch (Exception e){
			e.printStackTrace();
		}
	}// end run
} // end of class RequestHandler