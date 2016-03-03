import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;

class SHTTPTestClient{
	public static Socket socket;
	public static String server;
	public static String serverName;
	public static int port;
	public static int threads;
	public static String fileList;
	public static double testingTime;
	public static List<String> files;


	public static void main (String [] args){
		
		// Correct # of args?
		if(args.length != 12){
			printUsage();
			return;
		}

		// Get command line args
		setCommandLineArgs(args);

		Thread[] threadList = new Thread[threads];
		for(int i = 0; i < threads; i++){
			threadList[i] = new Thread(new Tester(testingTime));
			threadList[i].start();
		}

		try{
			for (Thread t : threadList){
				t.join();
			}
		}catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Total number of files downloaded: " + Tester.totalFilesDownloaded.get());
		System.out.println("Total number of files downloaded/sec: " + (Tester.totalFilesDownloaded.get() / (testingTime * 1000)));
		System.out.println("Average wait time/download (ms): " + (Tester.totalWaitTime.get() / Tester.totalNumWaits.get()));
		System.out.println("Average bytes downloaded/sec: " + (Tester.totalBytesDownloaded.get() / (testingTime * 1000)));
	} // end main

	public static void printUsage(){
		System.out.println("Usage: java SHTTPTestClient -server <server> " 
			+ "-servname <server name> -port <server port> " 
			+ "-parallel <# of threads> -files <file name> " 
			+ "-T <time of test in seconds>");
	}

	public static void setCommandLineArgs(String [] args){
		for(int i = 0; i < args.length; i += 2){
			if(args[i].equals("-server")){
				server = args[i + 1];
			}else if(args[i].equals("-servname")){
				serverName = args[i + 1];
			}else if(args[i].equals("-port")){
				port = Integer.parseInt(args[i + 1]);
			}else if(args[i].equals("-parallel")){
				threads = Integer.parseInt(args[i + 1]);
			}else if(args[i].equals("-files")){
				fileList = args[i + 1];
			}else if(args[i].equals("-T")){
				testingTime = Double.parseDouble(args[i + 1]);
			}else{
				System.out.println("Bad command: " + args[i]);
				printUsage();
				return;
			}
		}

		// Read in files to download
		files = new ArrayList<String>();
		try(BufferedReader br = new BufferedReader(new FileReader(fileList))) {
		    String line = br.readLine();

		    while (line != null) {
		    	if(line.trim() != ""){
			        files.add(line);
			        line = br.readLine();
		    	}
		    }
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}

class Tester implements Runnable{
	public Socket socket;
	public int numFilesDownloaded = 0;
	public long bytesDownloaded = 0;
	public long waitTime = 0;
	public long numWaits = 0;
	public double timeToRun = 0;

	static AtomicInteger totalFilesDownloaded = new AtomicInteger(0);
	static AtomicLong totalBytesDownloaded = new AtomicLong(0);
	static AtomicLong totalNumWaits = new AtomicLong(0);
	static AtomicLong totalWaitTime = new AtomicLong(0);

	public Tester(double timeToRun){
		this.timeToRun = timeToRun;
	}
	
	@Override
	public void run(){
		try{
			// When will we stop testing?
			long startTime = System.currentTimeMillis();
			long endTime = startTime + (long)(timeToRun * 1000);

			int numFilesDownloaded = 0;
			while(System.currentTimeMillis() < endTime){
				// Download all files!
				if(SHTTPTestClient.files == null){
					break;
				}
				for(String file : SHTTPTestClient.files){
					// Don't go over alotted time
					if(System.currentTimeMillis() >= endTime){
						break;
					}

					// Make a new socket with reader/writer
					socket = new Socket(SHTTPTestClient.server, SHTTPTestClient.port);
					Writer outWriter = new OutputStreamWriter(socket.getOutputStream(), 
						"US-ASCII");
					BufferedReader inReader = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));

					// Request file
					outWriter.write("GET " + file + " HTTP/1.0\r\n");
					if(SHTTPTestClient.serverName != null){
						outWriter.write("Host: " + SHTTPTestClient.serverName + "\r\n");
					}
					outWriter.write("\r\n");
					outWriter.flush();

					// Wait for a reply
					long startedWaiting = System.currentTimeMillis();

					// Receive response from network
					String response = inReader.readLine();

					// Log how much time we waited
					waitTime += System.currentTimeMillis() - startedWaiting;
					numWaits ++;

					// Was the request ok?
					if(!response.split(" ")[1].equals("200")){
						return;
					}

					// Find content length and download file
					String line = inReader.readLine();
					while(line != ""){
						String[] tokens = line.split(" ");
						if(line.contains("Content-Length")){
							int responseLength = Integer.parseInt(tokens[1]);
							inReader.skip(responseLength);
							bytesDownloaded += responseLength;
							break;
						}
						line = inReader.readLine();
					}
					numFilesDownloaded++;
				} // end for-loop over files
			} // end timer while-loop

			totalFilesDownloaded.addAndGet(numFilesDownloaded);
			totalWaitTime.addAndGet(waitTime);
			totalNumWaits.addAndGet(numWaits);
			totalBytesDownloaded.addAndGet(bytesDownloaded);

		}catch(Exception e){
			System.out.println("Error downloading files from " 
				+ SHTTPTestClient.server);
			e.printStackTrace();
		} // end try-catch
	} // end run
} // end inner class
