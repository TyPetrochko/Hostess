/**
 ** Code adopted from:
 ** 
 ** Yale CS433/533 Demo Basic Web Server
 **/
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

class WebServer{
	
    public static int serverPort = 6789;
    public static List<VirtualHost> virtualHosts = new ArrayList<VirtualHost>();
    public static ServerSocket listenSocket = null;
    public static String WWW_ROOT = "./";
    public static int threadPoolSize = 1;

    public static void main(String args[]) throws Exception  {

		if (args.length == 3 && args[1].equals("-config")){
			// Try to parse configuration file
			try{
				ConfigParser cp = new ConfigParser(args[2]);
				virtualHosts = cp.virtualHosts;
				if(cp.port != -1)
					serverPort = cp.port;
				if(cp.threadPoolSize != -1)
					threadPoolSize = cp.threadPoolSize;
			}catch(Exception e){
				System.out.println("Could not load configurations: " + args[2]);
				e.printStackTrace();
				return;
			}
			String serverType = args[0].toLowerCase();
			switch(serverType){
				case "sequential":
					initSocket();
					sequential();
					break;
				case "per-request-thread":
					initSocket();
					perRequestThread();
					break;
				case "competing":
					initSocket();
					competing();
					break;
				case "busywait":
					initSocket();
					busyWait();
					break;
				case "suspension":
					initSocket();
					suspension();
					break;
				default:
					System.out.println("Could not detect server type " + args[0]);
					printUsage();
					return;
				}
		}else{
			printUsage();
			return;
		} // end if
    } // end of main

    public static void initSocket() throws IOException{
    	// create server socket
		listenSocket = new ServerSocket(serverPort);

		// list all listening hosts
		System.out.println("server listening at: " + listenSocket);
		for (VirtualHost v : virtualHosts){
			System.out.println("host www root: " + v.documentRoot);
		}
    }

    // run sequential server
    public static void sequential () throws IOException{
		while (true) {
		    try {
			    // take a ready connection from the accepted queue
			    Socket connectionSocket = listenSocket.accept();
			    System.out.println("\nReceive request from " + connectionSocket);
		
			    // process a request
			    WebRequestHandler wrh = 
			        new WebRequestHandler( connectionSocket, virtualHosts );

			    wrh.processRequest();

		    } catch (Exception e){
				e.printStackTrace();
			}
		}
    } // end of sequential function

    // make a single request handler per thread
    public static void perRequestThread () throws IOException{
		while (true) {
		    // take a ready connection from the accepted queue
		    Socket connectionSocket = listenSocket.accept();
		    System.out.println("\nReceive request from " + connectionSocket);
	
		    // process a request
		    new SingleThreadRequestHandler(connectionSocket, virtualHosts).start();
		} // end of while (true)
    }

    // have all threads compete on a single socket
    public static void competing() throws IOException{
        // create thread pool
        CompetingServiceThread[] threads = 
        new CompetingServiceThread[threadPoolSize];

        // start all threads
        for (int i = 0; i < threads.length; i++) {
	        threads[i] = new CompetingServiceThread(listenSocket, virtualHosts); 
	        threads[i].start();
        }
    };

    // Use a shared queue with busy waiting
    public static void busyWait() throws IOException{
    	List<Socket> socketPool = new Vector<Socket>();

    	BusyWaitServiceThread[] threads = 
        new BusyWaitServiceThread[threadPoolSize];

    	// start all threads
        for (int i = 0; i < threads.length; i++) {
	        threads[i] = new BusyWaitServiceThread(socketPool, virtualHosts); 
	        threads[i].start();
        }

    	new BusyWaitDelegate(socketPool, listenSocket).start();
    };
    public static void suspension(){
    	List<Socket> socketPool = new Vector<Socket>();
    	// ReentrantLock lock = new ReentrantLock();

    	SuspensionServiceThread[] threads = 
        new SuspensionServiceThread[threadPoolSize];

    	// start all threads
        for (int i = 0; i < threads.length; i++) {
	        threads[i] = new SuspensionServiceThread(socketPool, virtualHosts); 
	        threads[i].start();
        }

    	new SuspensionDelegate(socketPool, listenSocket).start();
    };

    public static void printUsage(){
    	System.out.println("Usage: java WebServer <Server> -config config.conf");
	 	System.out.println("\tServer: sequential | per-request-thread | competing | busywait | suspension");
    }
} // end of class WebServer

class BusyWaitDelegate extends Thread {
	private List<Socket> socketPool;
	private ServerSocket welcomeSocket;

	public BusyWaitDelegate(List <Socket> socketPool, 
		ServerSocket welcomeSocket){

		this.socketPool = socketPool;
		this.welcomeSocket = welcomeSocket;
	}

	public void run (){
		while (true) {
	        try {
		        // accept connection from connection queue
		        Socket connSock = welcomeSocket.accept();

		        // how to assign to an idle thread?
		        synchronized (socketPool) {
		            socketPool.add(connSock);
		        } // end of sync
	        } catch (Exception e) {
	        	System.out.println("server run failed.");
	        } // end of catch
	    } // end of loop
	}
}

class SuspensionDelegate extends Thread {
	private List<Socket> socketPool;
	private ServerSocket welcomeSocket;

	public SuspensionDelegate(List <Socket> socketPool, 
		ServerSocket welcomeSocket){

		this.socketPool = socketPool;
		this.welcomeSocket = welcomeSocket;
	}

	public void run (){
		while (true) {
	        try {
		        // accept connection from connection queue
		        Socket connSock = welcomeSocket.accept();

		        // how to assign to an idle thread?
		        synchronized (socketPool) {
		            socketPool.add(connSock);
		            socketPool.notifyAll();
		        } // end of sync
	        } catch (Exception e) {
	        	System.out.println("server run failed.");
	        } // end of catch
	    } // end of loop
	}
}