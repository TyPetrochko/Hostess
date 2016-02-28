import java.io.*;
import java.net.*;
import java.util.*;

class SHTTPTestClient{
	public static Socket socket;
	public static OutputStreamWriter clientOut;
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

		// Make server connection
		try{
			// When will we stop testing?
			long startTime = System.currentTimeMillis();
			long endTime = startTime + (long)(testingTime * 1000);

			int numFilesDownloaded = 0;
			while(System.currentTimeMillis() < endTime){
				// Download all files!
				if(files == null){
					break;
				}
				for(String file : files){
					// Don't go over alotted time
					if(System.currentTimeMillis() >= endTime){
						break;
					}

					// Make a new socket with reader/writer
					socket = new Socket(server, port);
					Writer outWriter = new OutputStreamWriter(socket.getOutputStream(), 
						"US-ASCII");
					BufferedReader inReader = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));

					// Request file
					outWriter.write("GET " + file + " HTTP/1.0\r\n");
					if(serverName != null){
						outWriter.write("Host: " + serverName + "\r\n");
					}
					outWriter.write("\r\n");
					outWriter.flush();

					// Receive response from network
					String response = inReader.readLine();
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
							break;
						}
						line = inReader.readLine();
					}
					numFilesDownloaded++;
				} // end for-loop over files
			} // end timer while-loop

			System.out.println("Downloaded " + numFilesDownloaded + " files");

		}catch(Exception e){
			System.out.println("Error downloading files from " + server);
			e.printStackTrace();
		} // end try-catch
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