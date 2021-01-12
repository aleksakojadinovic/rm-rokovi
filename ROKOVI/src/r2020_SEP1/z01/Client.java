package r2020_SEP1.z01;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Scanner;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Client {



    private final SocketChannel channel;
    private final Scanner clientIn;
    public Client(String host, int port) throws IOException {
        channel = SocketChannel.open(new InetSocketAddress(host, port));
        channel.configureBlocking(true);
        clientIn = new Scanner(System.in);
    }

    private double readCoverage() {
        return 0;
    }

    public void clientMainLoop(){
        try{
            while(true){
                ByteBuffer clientWriteBuffer = ByteBuffer.allocate(DATA_BUFF_SIZE);
                ByteBuffer clientReadBuffer  = ByteBuffer.allocate(COVERAGE_BUFF_SIZE);
                Integer[] values = new Integer[3];
                try{
                    System.out.print("Enter x y and r: ");
                    var line = clientIn.nextLine();
                    Supplier<Stream<Integer>> supplier = () -> Stream.of(line.split(" ")).map(Integer::parseInt);
                    if (supplier.get().count() != 3)
                        throw new IllegalArgumentException("Expecting three values.");
                    if (supplier.get().anyMatch(v -> v < 0))
                        throw new IllegalArgumentException("Expecting only positive values.");
                    values = supplier.get().toArray(Integer[]::new);
                }catch (Exception e){
                    //NOTE: Catching general exception because this could've been caused by either
                    //NumberFormatException or InputMismatchException or god knows what
                    System.out.println("Error while parsing input data: " + e.getMessage());
                }

                System.out.printf("\tSending %d %d %d\r\n", values[0], values[1], values[2]);
                clientWriteBuffer.clear();
                clientWriteBuffer.putInt(values[0]);
                clientWriteBuffer.putInt(values[1]);
                clientWriteBuffer.putInt(values[2]);
                clientWriteBuffer.flip();
                while (clientWriteBuffer.hasRemaining())
                    channel.write(clientWriteBuffer);

                clientReadBuffer.clear();
//                System.out.println("\tBEFORE READ:");
//                Server.printBufferInfo(clientReadBuffer);
                while(clientReadBuffer.hasRemaining()){
                    channel.read(clientReadBuffer);
//                    System.out.println("\tREADING:");
//                    Server.printBufferInfo(clientReadBuffer);
                }
                clientReadBuffer.clear();
                var coverage = clientReadBuffer.getDouble();
                if (coverage == -1){
                    // Indicating that coverage has not been updated yet and that we should just
                    // resume sending data as normal
                    System.out.println("Operation complete.");
                    continue;
                }else if (coverage == -2){
                    System.err.println("Invalid data sent. Server stopped the connection.");
                    break;
                }else if (coverage == 100d){
                    System.out.println("Maximum coverage reached, stopping.");
                    break;
                }

                System.out.printf("Current coverage is %.2f%%\r\n", coverage);

            }
        }catch (IOException e){
            System.err.println("Client error: " + e.getMessage());
        }finally {
            try {
                channel.close();
            } catch (IOException e) {
                System.err.println("Failed to close client channel: " + e.getMessage());
            }
            clientIn.close();
        }

    }

    private static final String HOSTNAME = "localhost";
    private static final int    PORT     = 7337;
    private static final int COVERAGE_BUFF_SIZE = 8;
    private static final int DATA_BUFF_SIZE     = 12;

    public static void main(String[] args) {
        try{
            var c = new Client(HOSTNAME, PORT);
            c.clientMainLoop();
        }catch (IOException e){
            System.out.println("Failed to start client: " + e.getMessage());
        }
    }
}
