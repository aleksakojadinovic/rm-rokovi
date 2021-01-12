package r2020_JUN1.z03;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class CircleClient {

    private final SocketChannel socketChannel;


    public CircleClient(final String hostname, final int port) throws IOException {
        socketChannel = SocketChannel.open(new InetSocketAddress(hostname, port));
        socketChannel.configureBlocking(true);
    }

    public void clientExec(){

        try(Scanner userIn = new Scanner(System.in)){
            while (true){
                double input;

                System.out.print("Radius: ");
                String strInput = userIn.nextLine();
                try{
                    input = Double.parseDouble(strInput);
                }catch (Exception e){
                    System.out.println("Not a number.");
                    continue;
                }

                ByteBuffer bufferOut = ByteBuffer.allocate(8);
                bufferOut.putDouble(input);
                bufferOut.clear();
//                    System.out.println("Beginning to write to server...");
                socketChannel.write(bufferOut);
//                    System.out.println("Done writing to server!");
                if (input >= 0){
                    ByteBuffer bufferIn = ByteBuffer.allocate(8);
//                        System.out.println("Beginning to read from server...");
                    socketChannel.read(bufferIn);
//                        System.out.println("Done reading from server!");
                    bufferIn.clear();
                    double area = bufferIn.getDouble();
                    System.out.println("Area: " + area);
                }else break;

            }
        }catch (IOException e){
            System.err.println("Client error occurred: " + e.getMessage());
        }finally {
            try {
                socketChannel.close();
            } catch (IOException e) {
                System.err.println("Error closing client channel: ");
            }
        }

    }

    private static final String HOSTNAME = "localhost";
    private static final int    PORT     = 31415;

    public static void main(String[] args) {
        CircleClient client;
        try{
            client = new CircleClient(HOSTNAME, PORT);
            client.clientExec();
        }catch (Exception e){
            System.out.println("Failed to start client: " + e.getMessage()  + e.getMessage() + " exception type: " + e.getClass().toString());
        }

    }


}
