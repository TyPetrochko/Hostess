import java.nio.channels.*;
import java.net.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.nio.*;

public class Java7AsyncServer{
	public static int DEFAULT_PORT = 6789;
    public static long INCOMPLETE_TIMEOUT = 3000;

    public static List<VirtualHost> virtualHosts = new ArrayList<VirtualHost>();

    public static AsynchronousServerSocketChannel serverSocket;
	
	public static long cacheSize = 8000;
    
    public static AsynchronousServerSocketChannel openServerChannel(int port) {
    	// open a new async server socket
    	AsynchronousServerSocketChannel toReturn = null;
    	try{
    		toReturn = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(DEFAULT_PORT));	
    	}catch (Exception e){
    		System.out.println("Problem opening async server socket channel");
    		e.printStackTrace();
    	}

        return toReturn;
    }

    public static void main(String[] args) {
        
        // get command line arguments
        getCommandLineArgs(args);

        // open server socket on port
        System.out.println("Opening server on port " + DEFAULT_PORT);
        serverSocket = openServerChannel(DEFAULT_PORT);

        // make a new file cache singleton
        new FileCache(cacheSize);

        // set up async callback for accepting a new client
    	serverSocket.accept(null, new CompletionHandler<AsynchronousSocketChannel,Void>() {
			public void completed(AsynchronousSocketChannel ch, Void att) {

				// accept the next connection
				serverSocket.accept(null, this);

				// handle this connection
				Java7AsyncHandler handler = new Java7AsyncHandler(ch);
				
				handler.handle();
				
			}
			public void failed(Throwable exc, Void att) {
				// failed connection, don't handle it explicitly
			}
			});

    	// to prevent application from closing, do a semi-busy-wait
    	while(serverSocket.isOpen()){
    		try{
    			Thread.sleep(5000);
    		}catch (Exception e){
    			System.out.println("Main thread cannot sleep");
    			e.printStackTrace();
    		}
    	}
    } // end of main

    public static void getCommandLineArgs(String [] args){
    	if (args.length == 2 && args[0].equals("-config")){
            // try to parse configuration file
            try{
                ConfigParser cp = new ConfigParser(args[1]);

                // set virtual hosts
                virtualHosts = cp.virtualHosts;

                // set any extra params
                if(cp.port != -1)
                    DEFAULT_PORT = cp.port;
                if(cp.incompleteTimeout != -1.0f){
                    INCOMPLETE_TIMEOUT = (long) (cp.incompleteTimeout * 1000);
                }
                if(cp.cacheSize != -1)
                    cacheSize = cp.cacheSize;
            }catch(Exception e){
                System.out.println("Could not load configurations: " + args[1]);
                e.printStackTrace();
                return;
            }
        }else{
            printUsage();
            return;
        }
    }

    public static void printUsage(){
        System.out.println("Usage: java AsyncServer -config config.conf");
    }
}

// simple Java 7 handler for socket channel
class Java7AsyncHandler{
	AsynchronousSocketChannel toHandle;

	boolean doneReading;
	boolean doneWriting;

	final ByteBuffer in;
	final ByteBuffer out;

	StringBuffer request;

	AsyncWebRequestHandler asyncHandler;

	public Java7AsyncHandler(AsynchronousSocketChannel toHandle){
		this.toHandle = toHandle;
		in = ByteBuffer.allocate(1024);
		out = ByteBuffer.allocate(1024);
		request = new StringBuffer(1024);
		doneReading = false;
		doneWriting = false;
		System.out.println("Making a new handler");
	}

	// set up async read and write handlers for a new connection
	public void handle(){
		in.clear();
		out.clear();

		read();
	}

	public void setRequestHandler(AsyncWebRequestHandler a){
		asyncHandler = a;
	}

	public void read(){
		// specify read completion callback, with timeout
		toHandle.read(in, Java7AsyncServer.INCOMPLETE_TIMEOUT, 
			TimeUnit.MILLISECONDS, null, new CompletionHandler<Integer, Void>(){

			// when a read is successful
			public void completed(Integer bytesRead, Void att){
				try{
					// clear outbuffer and prepare to read from in
					out.clear();

					in.flip();
			        byte[] msg_bytes = new byte[in.remaining()];
			        in.get(msg_bytes);

			        // Convert to string
			        String line = new String(msg_bytes, "US-ASCII");

			        // Add on to current message
			        request.append(line);

					// only process request if it's finished
					//if(upToHere.endsWith("\r\n\r\n") || upToHere.substring(upToHere.length() - bytesRead).equals("\r\n")){
					if(request.toString().endsWith("\r\n\r\n") || line.equals("")){

			            // handle the request and store handler
			            InetAddress remoteAddr = ((InetSocketAddress)toHandle.getLocalAddress()).getAddress();
			            InetAddress serverAddr = ((InetSocketAddress)toHandle.getLocalAddress()).getAddress();
			            int port = Java7AsyncServer.DEFAULT_PORT;
						AsyncWebRequestHandler a =  new AsyncWebRequestHandler(remoteAddr, serverAddr, port,
							request, out, Java7AsyncServer.virtualHosts);
						setRequestHandler(a);
						a.processRequest();

						// write to out
	        			write();
			        }else{
			        	in.clear();
			        	read();
			        }
    			}catch (Exception e){
    				System.out.println("Error handling client socket: ");
    				e.printStackTrace();
    				try{
						toHandle.close();
					}catch (Exception ee){
						System.out.println("Couldn't close socket");
						ee.printStackTrace();
					}
    			}
			}
			// in case of timeout, close socket connection
			public void failed (Throwable ex, Void att){
				try{
					toHandle.close();
				}catch (Exception e){
					System.out.println("Couldn't close socket");
					e.printStackTrace();
				}
			}
		});
	}

	public void write(){
		out.flip();
		toHandle.write(out, null, new CompletionHandler<Integer, Void>(){
			// probably gonna throw an error about not accessing in static w/e
			public void completed(Integer bytesWritten, Void att){
				if(asyncHandler.isDoneProcessing()){
					System.out.println("We finished!");
					try{
						toHandle.close();
					}catch (Exception e){
						System.out.println("Couldn't close socket");
						e.printStackTrace();
					}
				}else{
					System.out.println("Not done... write some more!");
					out.clear();
					try{
						asyncHandler.continueProcessing();
						write();
					}catch (Exception e){
						System.err.println("Couldn't continue processing file");
						e.printStackTrace();
					}
				}
			}
			public void failed (Throwable ex, Void att){
				System.out.println("Writing failed");
				try{
					toHandle.close();
				}catch (Exception e){
					System.out.println("Couldn't close socket");
					e.printStackTrace();
				}
			}
		});
		
	}
}