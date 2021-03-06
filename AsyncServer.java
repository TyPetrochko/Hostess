import java.nio.channels.*;
import java.net.*;
import java.io.IOException;
import java.util.*;

public class AsyncServer {

    public static int DEFAULT_PORT = 6789;
    public static long INCOMPLETE_TIMEOUT = 3000;
    public static List<VirtualHost> virtualHosts = new ArrayList<VirtualHost>();

    public static ServerSocketChannel serverChannel;
    public static int cacheSize = 8000;

    public static ServerSocketChannel openServerChannel(int port) {
        try {
            // open server socket for accept
            serverChannel = ServerSocketChannel.open();

            // extract server socket of the server channel and bind the port
            ServerSocket ss = serverChannel.socket();
            InetSocketAddress address = new InetSocketAddress(port);
            ss.bind(address);

            // configure it to be non blocking
            serverChannel.configureBlocking(false);

            Debug.DEBUG("Server listening for connections on port " + port);

        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        } // end of catch

        return serverChannel;
    } // end of open serverChannel

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
                    Debug.DEBUG("Timeout specified: " + INCOMPLETE_TIMEOUT);
                }
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
            return;
        }

        // get dispatcher/selector
        Dispatcher dispatcher = new Dispatcher();

        // make a timeout thread as well
        TimeoutThread timeout = new TimeoutThread(dispatcher);
        //Timeout t = new Timeout(INCOMPLETE_TIMEOUT);

        // open server channel
        ServerSocketChannel sch = openServerChannel(DEFAULT_PORT);

        // (recycled code from EchoServer.java)
        ISocketReadWriteHandlerFactory echoFactory = new ReadWriteHandlerFactory();
        Acceptor acceptor = new Acceptor(echoFactory);

        // init dispatcher
        Thread dispatcherThread;

        // make a file cache (singleton)
        FileCache globalCache = new FileCache(cacheSize);

        try {
            // make a selection key for acceptor
            SelectionKey key = sch.register(dispatcher.selector(), SelectionKey.OP_ACCEPT);
            key.attach(acceptor);
            
            // start dispatcher
            dispatcherThread = new Thread(dispatcher);
            dispatcherThread.start();
            timeout.start();
        } catch (IOException ex) {
            Debug.DEBUG("Cannot register and start server");
            System.exit(1);
        }
        // may need to join the dispatcher thread

    } // end of main

    public static void printUsage(){
        System.err.println("Usage: java AsyncServer -config config.conf");
    }

} // end of class
