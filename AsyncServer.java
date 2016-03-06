import java.nio.channels.*;
import java.net.*;
import java.io.IOException;
import java.util.*;

public class AsyncServer {

    public static int DEFAULT_PORT = 6789;
    public static long INCOMPLETE_TIMEOUT = 3000;
    public static List<VirtualHost> virtualHosts = new ArrayList<VirtualHost>();

    public static ServerSocketChannel openServerChannel(int port) {
        ServerSocketChannel serverChannel = null;
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
                    System.out.println("Timeout specified: " + INCOMPLETE_TIMEOUT);
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

        // get dispatcher/selector
        Dispatcher dispatcher = new Dispatcher();

        // make a timeout thread as well
        //TimeoutThread timeout = new TimeoutThread(dispatcher);
        Timeout t = new Timeout(INCOMPLETE_TIMEOUT);

        // open server socket channel
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception ex) {
            port = DEFAULT_PORT;
        }
        ServerSocketChannel sch = openServerChannel(port);

        // create server acceptor for Echo Line ReadWrite Handler
        ISocketReadWriteHandlerFactory echoFactory = new ReadWriteHandlerFactory();
        Acceptor acceptor = new Acceptor(echoFactory);

        Thread dispatcherThread;
        // register the server channel to a selector
        try {
            SelectionKey key = sch.register(dispatcher.selector(), SelectionKey.OP_ACCEPT);
            key.attach(acceptor);
            
            // start dispatcher
            dispatcherThread = new Thread(dispatcher);
            dispatcherThread.start();
            //timeout.start();
        } catch (IOException ex) {
            System.out.println("Cannot register and start server");
            System.exit(1);
        }
        // may need to join the dispatcher thread

    } // end of main

    public static void printUsage(){
        System.out.println("Usage: java AsyncServer -config config.conf");
    }

} // end of class
