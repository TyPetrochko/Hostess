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
        if (args.length == 2 && args[0].equals("-config")){
            // Try to parse configuration file
            try{
                ConfigParser cp = new ConfigParser(args[1]);
                virtualHosts = cp.virtualHosts;
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

        // open server socket on port
        System.out.println("Opening server on port " + DEFAULT_PORT);
        AsynchronousServerSocketChannel ss = openServerChannel(DEFAULT_PORT);

        
    	ss.accept(null, new CompletionHandler<AsynchronousSocketChannel,Void>() {
			public void completed(AsynchronousSocketChannel ch, Void att) {

				// accept the next connection
				ss.accept(null, this);

				// handle this connection
				Java7AsyncHandler handler = new Java7AsyncHandler(ch);
				
				handler.handle();
				
			}
			public void failed(Throwable exc, Void att) {
				// failed connection, don't worry about it
			}
			});

    	while(ss.isOpen()){
    		try{
    			Thread.sleep(5000);
    		}catch (Exception e){
    			System.out.println("Main thread cannot sleep");
    			e.printStackTrace();
    		}
    	}
    } // end of main

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

	public void handle(){
		in.clear();
		toHandle.read(in,
			Java7AsyncServer.INCOMPLETE_TIMEOUT, TimeUnit.MILLISECONDS, null,
			new CompletionHandler<Integer, Void>(){
				public void completed(Integer bytesRead, Void att){
					try{
						System.out.println("Responded!");
						out.clear();
						in.flip();
						StringBuffer request = new StringBuffer(4096);
						request.append(new String(in.array(), "US-ASCII"));
						new AsyncWebRequestHandler(request, out, Java7AsyncServer.virtualHosts).processRequest();
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

				public void failed (Throwable ex, Void att){
					// probably timed out
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