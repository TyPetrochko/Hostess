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
	public static String[] files;


	public static void main (String [] args){
		configAndRun(args, true);
	}

	public static float[] configAndRun(String [] args, boolean shouldPrint){
		// Correct # of args?
		if(args.length != 12){
			printUsage();
			return new float[2];
		}

		// Get command line args
		setCommandLineArgs(args);

		return runTest(shouldPrint);
	}

	public static void clearVars(){
		Tester.totalFilesDownloaded = new AtomicInteger(0);
		Tester.totalBytesDownloaded = new AtomicLong(0);
		Tester.totalNumWaits = new AtomicLong(0);
		Tester.totalWaitTime = new AtomicLong(0);
		Tester.totalDownloadTime = new AtomicLong(0);
	}

	// perform benchmarking
	public static float [] runTest(boolean print){
		Thread[] threadList = new Thread[threads];
		for(int i = 0; i < threads; i++){
			threadList[i] = new Thread(new Tester(testingTime));
			threadList[i].start();
		}

		try{
			Thread.sleep((long)(testingTime*1000));
			for (Thread t : threadList){
				t.join();
			}
		}catch (Exception e) {
			e.printStackTrace();
		}

		// we may have gone over time limit a bit
		double actualTestTime = ((double) Tester.totalDownloadTime.get() / 1000) / (double)threads;
		
		// track megabytes/second
		float mbps = (float)(Tester.totalBytesDownloaded.get() 
			/ (actualTestTime * 1000000));


		if(print){
			// print metrics
			System.out.println("Total number of files downloaded: " + 
				Tester.totalFilesDownloaded.get());
			System.out.println("Total number of files downloaded/sec: " + 
				(Tester.totalFilesDownloaded.get() / (actualTestTime)));
			System.out.println("Average wait time/download (ms): " + 
				(Tester.totalWaitTime.get() / Tester.totalNumWaits.get()));
			System.out.println("Average bytes downloaded/sec: " 
				+ (Tester.totalBytesDownloaded.get() / actualTestTime));
			System.out.println(String.format("Average MB downloaded/sec: %.4f", mbps));
		}

		// return metrics
		float [] metrics = new float[2];
		metrics[1] = (float) mbps;
		metrics[2] = (float) (Tester.totalWaitTime.get() / Tester.totalNumWaits.get());
		return metrics;
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
		ArrayList<String> filesArrayList = new ArrayList<String>();
		try(BufferedReader br = new BufferedReader(new FileReader(fileList))) {
		    String line = br.readLine();

		    while (line != null) {
		    	if(line.trim() != ""){
			        filesArrayList.add(line);
			        line = br.readLine();
		    	}
		    }
		}catch(Exception e){
			e.printStackTrace();
		}
		files = filesArrayList.toArray(new String[filesArrayList.size()]);
	}
}

// main testing thread
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
	static AtomicLong totalDownloadTime = new AtomicLong(0);

	public Tester(double timeToRun){
		this.timeToRun = timeToRun;
	}
	
	@Override
	public void run(){
		try{
			// When will we stop testing?
			long startTime = System.currentTimeMillis();
			long endTime = startTime + (long)(timeToRun * 1000);

			// track number of files downloaded
			int numFilesDownloaded = 0;
			while(System.currentTimeMillis() < endTime){

				// Download all files!
				if(SHTTPTestClient.files == null){
					break;
				}

				// iterate over all files
				for(String file : SHTTPTestClient.files){
					try{
						// Don't go over alotted time
						if(System.currentTimeMillis() >= endTime){
							break;
						}

						// Make a new socket with reader/writer
						socket = new Socket(SHTTPTestClient.server, SHTTPTestClient.port);
						socket.setSoTimeout(1000);
						
						// get ready to write request
						Writer outWriter = new OutputStreamWriter(socket.getOutputStream(), 
							"US-ASCII");
						BufferedReader inReader = new BufferedReader(
							new InputStreamReader(socket.getInputStream()));

						// make request
						outWriter.write("GET " + file + " HTTP/1.0\r\n");
						if(SHTTPTestClient.serverName != null){
							outWriter.write("Host: " + SHTTPTestClient.serverName + "\r\n");
						}
						outWriter.write("\r\n");
						outWriter.flush();

						// wait for a reply
						long startedWaiting = System.currentTimeMillis();

						// Receive response from network
						String response = inReader.readLine();

						// Log how much time we waited
						waitTime += System.currentTimeMillis() - startedWaiting;
						numWaits ++;

						// Was the request ok?
						if(response == null){
							return;
						}else{
							bytesDownloaded += response.length();
							String[]tkz = response.split(" ");
							if(tkz.length < 2 || !tkz[1].equals("200")){
								System.err.println("Received a bad response: " + response);
								return;
							}
						}

						// process response header
						String line = inReader.readLine();

						// read through each line as long as we have time
						while(line != "" && System.currentTimeMillis() < endTime){

							/* Using .length() is an underestimate of bytes,
							** but avoids processing overhead
							*/
							bytesDownloaded += line.length();

							// parse header
							String[] tokens = line.split(" ");

							// did we get content-length?
							if(line.contains("Content-Length")){

								// max amount of data we could get from this request
								int responseLength = Integer.parseInt(tokens[1]);
								char[] readInto = new char[responseLength];
								int bytesRead = 0;

								/* we give the server three chances to get its data 
								** to client before giving up and starting a new 
								** request
								*/
								int numMisses = 0;
								while(bytesRead < responseLength 
									&& System.currentTimeMillis() < endTime 
									&& numMisses < 3){

									// how many writes did we read this pass?
									int bytesReadThisPass = inReader.read(readInto);
									if(bytesReadThisPass == -1){
										numMisses++; // server didn't respond in time
									}else{
										bytesRead += bytesReadThisPass;
									}
								}

								// update how many bytes this thread has downloaded
								bytesDownloaded += bytesRead;

								if(bytesRead != responseLength){
									Debug.DEBUG("Server promised " 
										+ responseLength 
										+ " bytes but only received " + bytesRead);
								}
								break;
							}

							// read in next header
							line = inReader.readLine();
						}

						// finished downloading a file; request a new one
						numFilesDownloaded++;

						socket.close();
					}catch (Exception e){
						try{
							socket.close();
						} catch (Exception ee){
							System.err.println("Couldn't close socket");
							ee.printStackTrace();
							return;
						}

					}
				} // end for-loop over files
			} // end timer while-loop

			// when did this test finish?
			endTime = System.currentTimeMillis();

			// track global totals
			totalFilesDownloaded.addAndGet(numFilesDownloaded);
			totalWaitTime.addAndGet(waitTime);
			totalNumWaits.addAndGet(numWaits);
			totalBytesDownloaded.addAndGet(bytesDownloaded);
			totalDownloadTime.addAndGet(endTime - startTime);

		}catch(Exception e){
			System.err.println("Error downloading files from " 
				+ SHTTPTestClient.server);
			return;
		} // end try-catch
	} // end run
} // end inner class
