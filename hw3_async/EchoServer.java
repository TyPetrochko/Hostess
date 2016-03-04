import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import java.util.*;
import java.io.IOException;

public class EchoServer {

    private static Selector selector;

    public static int DEFAULT_PORT = 6789;

    private static boolean DEBUG = true;

    private static void DEBUG(String s) {
        if (DEBUG) {
            System.out.println(s);
        }
    }

    public static void main(String[] args) {

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception ex) {
            port = DEFAULT_PORT;
        }
        DEBUG("Listening for connections on port " + port);

        // server socket channel and selector initialization
        try {

            // create selector
            selector = Selector.open();

            // open server socket for accept
            ServerSocketChannel serverChannel = openServerSocketChannel(port);

            // register the server channel to selector
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        } // end of catch

        // event loop
        while (true) {

            DEBUG("Enter selection");
            try {
                // check to see if any events
                selector.select();
            } catch (IOException ex) {
                ex.printStackTrace();
                break;
            } // end of catch

            // readKeys is a set of ready events
            Set<SelectionKey> readyKeys = selector.selectedKeys();

            // create an iterator for the set
            Iterator<SelectionKey> iterator = readyKeys.iterator();

            // iterate over all events
            while (iterator.hasNext()) {

                SelectionKey key = (SelectionKey) iterator.next();
                iterator.remove();

                try {
                    if (key.isAcceptable()) {
                        // a new connection is ready to be accepted
                        handleAccept(key);
                    } // end of isAcceptable

                    if (key.isReadable()) {
                        handleRead(key);
                    } // end of isReadable

                    if (key.isWritable()) {
                        handleWrite(key);
                    } // end of if isWritable
                } catch (IOException ex) {
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException cex) {
                    }
                } // end of catch

            } // end of while (iterator.hasNext()) {

        } // end of while (true)

    } // end of main

    private static ServerSocketChannel openServerSocketChannel(int port) {
        ServerSocketChannel serverChannel = null;

        try {
            // create server channel
            serverChannel = ServerSocketChannel.open();

            // extract server socket of the server channel and bind the port
            // In Java 7 and later, you can bind directly
            // erverChannel.bind(new InetSocketAddress(port));
            ServerSocket ss = serverChannel.socket();
            InetSocketAddress address = new InetSocketAddress(port);
            ss.bind(address);

            // configure it to be non blocking
            serverChannel.configureBlocking(false);
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        } // end of catch

        return serverChannel;

    } // end of openServerSocketChannel

    private static void handleAccept(SelectionKey key) 
            throws IOException {

        ServerSocketChannel server = (ServerSocketChannel) key.channel();

        // extract the ready connection
        SocketChannel client = server.accept();
        DEBUG("handleAccept: Accepted connection from " + client);

        // configure the connection to be non-blocking
        client.configureBlocking(false);

        // register the new connection with interests
        // v1:
        //SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ
        //        | SelectionKey.OP_WRITE);
        ///**********************
         // v2: we set only READ
         SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);         
         //*********************/
        
        // attach a buffer to the new connection
        // you may want to read up on ByteBuffer.allocateDirect on performance
        ByteBuffer buffer = ByteBuffer.allocate(100);
        clientKey.attach(buffer);

    } // end of handleAccept

    private static void handleRead(SelectionKey key) throws IOException {
        // a connection is ready to be read
        DEBUG("-->handleRead");
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer output = (ByteBuffer) key.attachment();
        int readBytes = client.read(output);

        ///**********************
        // v2: update state
        SelectionKey sk = key.channel().keyFor(selector);
        int nextState = sk.interestOps();

        // turn off read if readBytes == -1
        if (readBytes == -1) {// no longer need to read, close read
            nextState = nextState & ~SelectionKey.OP_READ;
            DEBUG("   State change: client closed; turn off read.");
        }
        // turn on write if there is data
        if (output.position() > 0) { // position is the available data to echo
            nextState = nextState | SelectionKey.OP_WRITE; // add write
            DEBUG("   State change: has data to send; turn on write.");
        }

        sk.interestOps(nextState);
        //**********************/
        
        DEBUG("   Read data from connection " + client + ": read " + readBytes
                + " byte(s); buffer becomes " + output);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        DEBUG("handleRead-->");

    } // end of handleRead

    private static void handleWrite(SelectionKey key) throws IOException {
        DEBUG("-->handleWrite");
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer output = (ByteBuffer) key.attachment();
        output.flip();
        int writeBytes = client.write(output);
        
        ///***********
        // v2: update write state; did not handle how to close after write all
        SelectionKey sk = key.channel().keyFor(selector);
        int nextState = sk.interestOps();
        if (!output.hasRemaining()) { // no response left
            nextState = nextState & ~SelectionKey.OP_WRITE;
            DEBUG("   State change: all data sent; turn off write");
            sk.interestOps(nextState);
        }    
        //*************/
        output.compact();
        DEBUG("   Write data to connection " + client + ": write " + writeBytes
                + " byte(s); buffer becomes " + output);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        DEBUG("handleWrite-->");
    } // end of handleWrite

} // end of class
