package r2020_SEP1.z01;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.spec.RSAOtherPrimeInfo;
import java.util.*;

public class Server {

    private final ServerSocketChannel serverSocketChannel;
    private final int m;
    private final int n;
    private final double totalArea;
    private final List<IntPair> takenSpots;
    private double areaCovered;
    private long lastBroadcastTime;

    private static final class IntPair{
        public int x;
        public int y;

        public IntPair(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IntPair intPair = (IntPair) o;
            return x == intPair.x &&
                    y == intPair.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    private static final class CodeAndBuff{
        public ByteBuffer buff;
        public int code;
        public boolean initialized;

        public CodeAndBuff(ByteBuffer buff, int code) {
            this.buff = buff;
            this.code = code;
            initialized = false;
        }
    }

    public Server(int port) throws IOException{
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);

//        Scanner infoIn = new Scanner(System.in);
//        System.out.print("Enter grid size: ");
        int m = 5;
        int n = 6;
//        if (m < 0 || n < 0)
//            throw new IllegalArgumentException("Negative dimensions.");
//        infoIn.close();
        this.m = m;
        this.n = n;
        totalArea = m*n;
        areaCovered = 0;
        takenSpots = new ArrayList<>();
        lastBroadcastTime = 0;

    }

    public static void printBufferInfo(final ByteBuffer buff){
        System.out.println("\t\tposition=" + buff.position());
        System.out.println("\t\tlimit=" + buff.limit());
        System.out.println("\t\tcapac=" + buff.capacity());
        System.out.println("\t\tremain=" + buff.remaining());
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException{
        System.out.println("[ACCEPT]");
        var acceptingChannel = (ServerSocketChannel)key.channel();
        System.out.println("Accepting...");
        var newClientChannel = acceptingChannel.accept();
        System.out.println("Configuring...");
        newClientChannel.configureBlocking(false);
        System.out.println("Registering...");
        try{
            newClientChannel.register(selector, SelectionKey.OP_READ);
        }catch (Exception e){
            e.printStackTrace();
        }

        System.out.println("New client accepted and registered!");
    }
    private void handleRead(SelectionKey key, Selector selector) throws IOException{
        System.out.println("[READ]");
        var clientBuffer = (ByteBuffer)key.attachment();
        if (clientBuffer == null){
            clientBuffer = ByteBuffer.allocate(DATA_BUFF_SIZE);
            key.attach(clientBuffer);
        }
        System.out.println("\tReading from client...");
        if (clientBuffer.hasRemaining()){
            ((SocketChannel)key.channel()).read(clientBuffer);
//            System.out.println("\tRead some bytes from client, buffer state: ");
//            printBufferInfo(clientBuffer);
            if (clientBuffer.hasRemaining())
                return;
        }
        System.out.println("\tDone reading from client!");
        int x, y, r;
        clientBuffer.clear();
//        System.out.println("\tBuffer before fetching info:");
        x = clientBuffer.getInt();
        y = clientBuffer.getInt();
        r = clientBuffer.getInt();
        System.out.printf("\t\tGot %d %d %d\r\n", x, y, r);
        int area = 4*r*r;
        //TODO: I would also need to keep track of exactly which spots were taken because someone might try and take someone else's spot.
        int code = 0;
        ByteBuffer clientAttachBB = ByteBuffer.allocate(COVERAGE_BUFF_SIZE);
        if (area < 0 || x < 0 || x > m || y < 0 || y > n || r < 0 || takenSpots.contains(new IntPair(x, y))){
            System.out.println(takenSpots.contains(new IntPair(x, y)));
            code = -2;
        }else{
            takenSpots.add(new IntPair(x, y));
            areaCovered += area;
            code = -1;
        }
        System.out.println("\tSwitch client to write mode. Code=" + code);
        key.attach(new CodeAndBuff(clientAttachBB, code));
        key.interestOps(SelectionKey.OP_WRITE);

    }
    private void handleWrite(SelectionKey key, Selector selector) throws IOException{
        System.out.println("[WRITE]");

        CodeAndBuff clientAttachment = (CodeAndBuff) key.attachment();
        var toClientBuffer = clientAttachment.buff;
        var code = clientAttachment.code;
        if (!clientAttachment.initialized){
            if (code == -1 && System.currentTimeMillis() - lastBroadcastTime >= 5000){
                lastBroadcastTime = System.currentTimeMillis();
                toClientBuffer.putDouble(100.0*areaCovered/totalArea);
            }else if (code == -1){
                toClientBuffer.putDouble(-1);
            }else if (code == -2){
                toClientBuffer.putDouble(-2);
            }
            toClientBuffer.clear();
            clientAttachment.initialized = true;
        }


//        printBufferInfo(toClientBuffer);
        if (toClientBuffer.hasRemaining()){
            System.out.println("\tBeginning to write to client.");
            ((SocketChannel)key.channel()).write(toClientBuffer);
            if (toClientBuffer.hasRemaining())
                return;
        }
        System.out.println("\tDone writing to client.");
        if (code == -2){
            System.out.println("\t\tKicking client.");
            key.channel().close();
            key.cancel();
        }else{
            System.out.println("\t\tSwitching client to read mode.");
            key.attach(null);
            key.interestOps(SelectionKey.OP_READ);
        }

    }

    public void mainLoop(){

        try(Selector selector = Selector.open()){
            lastBroadcastTime = System.currentTimeMillis();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Main loop started...");
            while(true){
                selector.select();
                var it = selector.selectedKeys().iterator();
                while (it.hasNext()){
                    SelectionKey key = it.next();
                    it.remove();
                    try{
                        if (key.isAcceptable()){
                            handleAccept(key, selector);
                        }else if (key.isReadable()){
                            handleRead(key, selector);
                        }else if (key.isWritable()){
                            handleWrite(key, selector);
                        }
                    }catch (Exception e){
                        System.err.print("Failed to process client: " + e.getMessage());
                        e.printStackTrace();
                        try{
                            key.cancel();
                            key.channel().close();
                            System.err.println();
                        }catch (IOException e2){
                            System.err.println(" -- also failed to close it: " + e2.getMessage());
                        }
                    }

                }
            }
        }catch (IOException e){
            System.out.println("Server crash: " + e.getMessage());
        }
    }

    private static final int PORT = 7337;
    private static final int DATA_BUFF_SIZE = 12;
    private static final int COVERAGE_BUFF_SIZE = 8;

    public static void main(String[] args) {
        try {
            var server = new Server(PORT);
            server.mainLoop();
        } catch (Exception e) {
            System.out.println("Failed to create/start server: " + e.getMessage());
        }

    }

}
