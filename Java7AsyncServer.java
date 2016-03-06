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
        AsynchronousServerSocketChannel ss = openServerChannel(DEFAULT_PORT);

        // set up async callback for accepting a new client
    	ss.accept(null, new CompletionHandler<AsynchronousSocketChannel,Void>() {
			public void completed(AsynchronousSocketChannel ch, Void att) {

				// accept the next connection
				ss.accept(null, this);

				// handle this connection
				Java7AsyncHandler handler = new Java7AsyncHandler(ch);
				
				handler.handle();
				
			}
			public void failed(Throwable exc, Void att) {
				// failed connection, don't handle it explicitly
			}
			});

    	// to prevent application from closing, do a semi-busy-wait
    	while(ss.isOpen()){
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

	final ByteBuffer in;
	final ByteBuffer out;
	public Java7AsyncHandler(AsynchronousSocketChannel toHandle){
		this.toHandle = toHandle;
		in = ByteBuffer.allocate(1024);
		out = ByteBuffer.allocate(1024);
	}

	// set up async read and write handlers for a new connection
	public void handle(){
		in.clear();

		// specify read completion callback, with timeout
		toHandle.read(in,
			Java7AsyncServer.INCOMPLETE_TIMEOUT, TimeUnit.MILLISECONDS, null,
			new CompletionHandler<Integer, Void>(){

				// when a read is successful
				public void completed(Integer bytesRead, Void att){
					try{

						// clear outbuffer and prepare to read from in
						out.clear();
						in.flip();
						StringBuffer request = new StringBuffer(4096);
						request.append(new String(in.array(), "US-ASCII"));

						// handle the request
						new AsyncWebRequestHandler(request, out, 
							Java7AsyncServer.virtualHosts).processRequest();

						// write to out
	        			out.flip();
	        			toHandle.write(out);
        			}catch (Exception e){
        				System.out.println("Error handling client socket: ");
        				e.printStackTrace();
        			}finally{
        				try{
							toHandle.close();
						}catch (Exception e){
							System.out.println("Couldn't close socket");
							e.printStackTrace();
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
}