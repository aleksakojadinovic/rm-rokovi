package r2020_JUN1.z03;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class CircleServer {
    private final int port;

    public CircleServer(int port) {
        this.port = port;
    }

    private void mainLoop(){
        try(ServerSocketChannel serverChannel = ServerSocketChannel.open();
            Selector selector = Selector.open()){

            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Starting server...");

            while (true){
                selector.select();

                var it = selector.selectedKeys().iterator();
                while (it.hasNext()){
                    var key = it.next();
                    it.remove();
                    try{
                        if (key.isAcceptable()){
                            var channel = (ServerSocketChannel)key.channel();
                            var client = channel.accept();
                            client.configureBlocking(false);
                            System.out.println("Accepted connection from " + client);
                            var clientKey = client.register(selector, SelectionKey.OP_READ);
                            clientKey.attach(ByteBuffer.allocate(8));
                        }else if (key.isReadable()){
                            var channel = (SocketChannel)key.channel();
                            System.out.println("Got read from " + channel);
                            var currentBuffer = (ByteBuffer)key.attachment();
                            // CASE 1: Reading for the first time
                            if (currentBuffer == null){
                                System.out.println("\tFirst read detected, allocating buffer.");
                                currentBuffer = ByteBuffer.allocate(8);
                                key.attach(currentBuffer);
                            }
                            System.out.println("\tReading from client...");
                            channel.read(currentBuffer);
                            System.out.println("\tDone reading from client!");
                            // Now we need to tell whether we've read the entire thing
                            if (!currentBuffer.hasRemaining()){
                                // We've read the entire thing
                                System.out.println("\tClient has been fully read, extracting value...");
                                currentBuffer.clear();
                                double radius = currentBuffer.getDouble();
                                System.out.println("\t\textracted: " + radius);
                                if (radius >= 0){
                                    // Means that we should now switch to writing
                                    System.out.println("\t\tPositive, switch to write mode.");
                                    key.interestOps(SelectionKey.OP_WRITE);
                                    ByteBuffer outgoingBuffer = ByteBuffer.allocate(8);
                                    outgoingBuffer.putDouble(0, radius*radius*Math.PI);
                                    key.attach(outgoingBuffer);
                                }else{
                                    // Negative value marks end of communication
                                    System.out.println("\t\tNegative, cancel communication.");
                                    key.cancel();
                                    channel.close();
                                }
                            }else{
                                System.out.println("\tClient not fully read, continue main loop.");
                            }

                        }else if (key.isWritable())
                        {
                            var channel = (SocketChannel)key.channel();
                            System.out.println("Got write from " + channel);
                            var outgoingBuffer = (ByteBuffer)key.attachment();
                            System.out.printf("\tOutgoing buffer info:\r\n" +
                                              " \t\tposition: %d\r\n" +
                                              " \t\tlimit: %d\r\n" +
                                              " \t\tremain: %d\r\n",
                                              outgoingBuffer.position(),
                                              outgoingBuffer.limit(),
                                              outgoingBuffer.remaining()
                                              );
                            System.out.println("\tWriting to outgoing buffer...");
                            channel.write(outgoingBuffer);
                            System.out.println("\tDone writing to outgoing buffer!");
                            System.out.printf("\tOutgoing buffer info:\r\n" +
                                            " \t\tposition: %d\r\n" +
                                            " \t\tlimit: %d\r\n" +
                                            " \t\tremain: %d\r\n",
                                    outgoingBuffer.position(),
                                    outgoingBuffer.limit(),
                                    outgoingBuffer.remaining()
                            );
                            if (!outgoingBuffer.hasRemaining()){
                                System.out.println("\tDone writing fully.");
                                //We're done sending, now we want to read from client again.
                                key.interestOps(SelectionKey.OP_READ);
                                key.attach(ByteBuffer.allocate(8));
                            }else{
                                System.out.println("\tHaven't written the whole thing, keep client in write mode.");
                            }
                        }
                    } catch(Exception e){
                        key.cancel();
                        System.err.println("Error while processing key. [reason:  " + e.getMessage() + "]" + " exception type: " + e.getClass().toString());
                    }
                }
            }




        }catch (IOException e){
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        CircleServer s = new CircleServer(31415);
        s.mainLoop();
    }
}
