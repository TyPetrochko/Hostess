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
			WebServer.numUsers ++;
			WebRequestHandler wrh = 
			        new WebRequestHandler( socket, virtualHosts );

		    wrh.processRequest();
		    WebServer.numUsers --;
		}catch (Exception e){
			e.printStackTrace();
		}
	}// end run
} // end of class RequestHandler