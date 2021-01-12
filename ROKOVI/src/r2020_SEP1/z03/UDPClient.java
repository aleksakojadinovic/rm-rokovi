package r2020_SEP1.z03;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Scanner;
public class UDPClient {


    public UDPClient() {

    }

    void runClient(){
        try (Scanner userIn = new Scanner(System.in);
             DatagramSocket socket = new DatagramSocket()) {
            System.out.println("Location: ");

            int x, y;

            x = userIn.nextInt();
            y = userIn.nextInt();

            byte[] buffOut = ByteBuffer.allocate(8).putInt(x).putInt(y).clear().array();
            socket.send(new DatagramPacket(buffOut, 0, buffOut.length, new InetSocketAddress(hostname, port)));

            byte[] buffIn = new byte[4];
            DatagramPacket incomingPacket = new DatagramPacket(buffIn, 0, buffIn.length, new InetSocketAddress(hostname, port));
            socket.receive(incomingPacket);

            boolean covered = ByteBuffer.wrap(buffIn).getInt() == 1;
            if (covered)
                System.out.println("Covered.");
            else
                System.out.println("Not covered.");

        }catch (IOException e){
            System.err.println("Client IO error: " + e.getMessage());
            e.printStackTrace();
        }catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }

    }


    private static final String hostname = "localhost";
    private static final int    port     = 12345;
    public static void main(String[] args) {
        var client = new UDPClient();
        client.runClient();
    }
}
