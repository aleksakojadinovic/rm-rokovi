package r2020_JUN2.z02;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class BinServer {
    private final DatagramSocket serverSocket;
    private static int packetCount;
    private static final int IN_BUFF_SIZE = 4;
    private static final int OUT_BUFF_SIZE = 32;

    public BinServer(int port) throws SocketException {
        serverSocket = new DatagramSocket(port);
        packetCount = 0;
    }

    private static final class ClientWorker implements Runnable{
        private final DatagramPacket clientPacket;
        private final BinServer server;

        public ClientWorker(DatagramPacket clientPacket, BinServer server) {
            this.clientPacket = clientPacket;
            this.server = server;
        }

        @Override
        public void run() {
            var userInput = clientPacket.getData();
            int userInputInt = ByteBuffer.wrap(userInput).getInt();
            String binaryRepresentation = Integer.toBinaryString(userInputInt);
            var response = binaryRepresentation.getBytes();

        }
    }

    public void mainLoop(){
        System.out.println("Starting server loop...");
        while (true){
            DatagramPacket clientRequest = new DatagramPacket(new byte[IN_BUFF_SIZE], IN_BUFF_SIZE);
            try{
                serverSocket.receive(clientRequest);
                System.out.printf("#%d Packet received:\r\n", ++packetCount);
                System.out.println("\tFrom: " + clientRequest.getAddress());
                System.out.println("\tAt port: " + clientRequest.getPort());
                new Thread(new ClientWorker(clientRequest, this)).start();
            }catch (IOException e){
                System.err.println("Failed to accept client. Reason: " + e.getMessage());
            }

        }

    }

    private static final int PORT = 10101;
    public static void main(String[] args) {
        try {
            var server = new BinServer(PORT);
            server.mainLoop();
        } catch (SocketException e) {
            System.out.println("Couldn't start server. Reason: " + e.getMessage());
        }
    }
}
