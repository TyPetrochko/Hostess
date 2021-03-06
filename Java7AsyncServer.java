/*
** A simple web-server using Java-7 async sockets
*/

import java.nio.channels.*;
import java.net.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.nio.*;

public class Java7AsyncServer{

	public static int DEFAULT_PORT = 6789;
    public static long INCOMPLETE_TIMEOUT = 3000;
    public static long cacheSize = 8000;
    public static List<VirtualHost> virtualHosts = new ArrayList<VirtualHost>();
    public static AsynchronousServerSocketChannel serverSocket;


    public static AsynchronousServerSocketChannel openServerChannel(int port) {

    	// open a new async server socket
    	AsynchronousServerSocketChannel toReturn = null;
    	try{
    		toReturn = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(DEFAULT_PORT));	
    	}catch (Exception e){
    		System.err.println("Problem opening async server socket channel");
    		e.printStackTrace();
    	}

        return toReturn;
    }

    public static void main(String[] args) {
        
        // get command line arguments
        getCommandLineArgs(args);

        // open server socket on port
        Debug.DEBUG("Opening server on port " + DEFAULT_PORT);
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
    			Debug.DEBUG("Main thread cannot sleep");
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
                if(cp.incompleteTimeout != -1.0f)
                    INCOMPLETE_TIMEOUT = (long) (cp.incompleteTimeout * 1000);
                if(cp.cacheSize != -1)
                    cacheSize = cp.cacheSize;
                if(cp.loadBalancer != null)
					new LoadBalancer(cp.loadBalancer);
            }catch(Exception e){
                Debug.DEBUG("Could not load configurations: " + args[1]);
                e.printStackTrace();
                return;
            }
        }else{
            printUsage();
            System.exit(-1);
        }
    }

    public static void printUsage(){
        System.err.println("Usage: java Java7AsyncServer -config config.conf");
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
		in = ByteBuffer.allocate(1024 * 100);
		out = ByteBuffer.allocate(1024 * 100);
		request = new StringBuffer(1024);
		doneReading = false;
		doneWriting = false;
		Debug.DEBUG("Making a new handler");
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
    				Debug.DEBUG("Error handling client socket: ");
    				e.printStackTrace();
    				try{
						toHandle.close();
					}catch (Exception ee){
						Debug.DEBUG("Couldn't close socket");
						ee.printStackTrace();
					}
    			}
			}
			// in case of timeout, close socket connection
			public void failed (Throwable ex, Void att){
				try{
					toHandle.close();
				}catch (Exception e){
					Debug.DEBUG("Couldn't close socket");
					e.printStackTrace();
				}
			}
		});
	}

	// perform a write operation to client
	public void write(){

		// prepare buffer for writing
		out.flip();

		// async write
		toHandle.write(out, null, new CompletionHandler<Integer, Void>(){

			// did the write complete?
			public void completed(Integer bytesWritten, Void att){

				// we may need to do more passes
				if(asyncHandler.isDoneProcessing()){
					Debug.DEBUG("Handler finished");
					try{
						toHandle.close();
					}catch (Exception e){
						Debug.DEBUG("Couldn't close socket");
						e.printStackTrace();
					}
				}else{
					Debug.DEBUG("Handler not finished - write some more");

					// clear output buffer
					out.clear();

					// continue processing (client may time-out)
					try{
						asyncHandler.continueProcessing();
						write();
					}catch (Exception e){
						System.err.println("Couldn't continue processing file");
						e.printStackTrace();
					}
				}
			}

			// did the write fail?
			public void failed (Throwable ex, Void att){
				Debug.DEBUG("Writing failed");
				try{
					toHandle.close();
				}catch (Exception e){
					Debug.DEBUG("Couldn't close socket");
					e.printStackTrace();
				}
			}
		});
		
	}
}