package r2020_SEP1.z02;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class UDPServer {


    private final DatagramSocket socket;
    private int m;
    private int n;
    private List<Entry> entries;

    private static final class Entry{
        private final int x;
        private final int y;
        private final int r;

        public Entry(int x, int y, int r) {
            this.x = x;
            this.y = y;
            this.r = r;
        }

        public boolean contains (int a, int b){
            System.out.printf("\t\t\tChecking whether %d %d %d contains %d %d\r\n", x, y, r, a, b);

            return a >= x - r && a <= x + r && b >= y - r && b <= y + r;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Entry entry = (Entry) o;
            return x == entry.x &&
                    y == entry.y &&
                    r == entry.r;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, r);
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "x=" + x +
                    ", y=" + y +
                    ", r=" + r +
                    '}';
        }
    }
    private static final class ClientHandler implements Runnable{
        private final UDPServer server;
        private final DatagramPacket clientPacket;


        public ClientHandler(UDPServer server, DatagramPacket clientPacket) {
            this.server = server;
            this.clientPacket = clientPacket;
        }

        @Override
        public void run() {
            System.out.printf("\tHandling client at %s:%d\r\n", clientPacket.getAddress(), clientPacket.getPort());
            ByteBuffer buffIn = ByteBuffer.wrap(clientPacket.getData());
            int x = buffIn.getInt();
            int y = buffIn.getInt();
            System.out.printf("\t\tClient checking for %d, %d\r\n", x, y);
            int response = server.entries.stream().anyMatch(e -> e.contains(x, y))? 1: 0;
            byte [] buffOut = ByteBuffer.allocate(4).putInt(response).clear().array();
            try {
                server.socket.send(new DatagramPacket(buffOut, 0, buffOut.length, clientPacket.getAddress(), clientPacket.getPort()));
                System.out.printf("\tClient at %s:%d successfully handled\r\n", clientPacket.getAddress(), clientPacket.getPort());
            } catch (IOException e) {
                System.err.println("\tClient handling failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    void mainLoop(){
        System.out.println("Starting main loop...");

        while (true){
            try{
                byte [] buffIn = new byte[8];
                DatagramPacket userPacket = new DatagramPacket(buffIn, 0, buffIn.length);
                socket.receive(userPacket);
                System.out.printf("Fetched client at %s:%d\r\n", userPacket.getAddress(), userPacket.getPort());
                new Thread(new ClientHandler(this, userPacket)).start();
            }catch (IOException e){
                System.err.println("Failed to receive client: " + e.getMessage());
            }

        }
    }

    private void readEntries(String filename) throws IOException {
        entries = new ArrayList<>();
        try(Scanner fileIn = new Scanner(new FileInputStream("./ROKOVI/src/r2020_SEP1/z02/" + filename))){
            m = fileIn.nextInt();
            n = fileIn.nextInt();
            System.out.println(m + " " + n);
            fileIn.nextLine();
            while (fileIn.hasNextLine()){
                    String line = fileIn.nextLine();
                    Supplier<Stream<Integer>> valSupplier = () ->Arrays.stream(line.split(" ")).map(Integer::parseInt);
                    int x, y, r;
                    var vals = valSupplier.get().toArray(Integer[]::new);
                    x = vals[0];
                    y = vals[1];
                    r = vals[2];
                    Entry e = new Entry(x, y, r);
                    entries.add(e);
            }

        }
    }

    public UDPServer(final int port, final String infile) throws IOException {
        readEntries(infile);
        socket = new DatagramSocket(new InetSocketAddress(port));
        System.out.println("Server running!");
    }

    private static final int PORT = 12345;

    public static void main(String[] args) {
        try {
            var s = new UDPServer(PORT, "in.txt");
            s.mainLoop();
        }catch(IOException e){
            System.err.println("Failed to create server: " + e.getMessage());
        }
    }
}
