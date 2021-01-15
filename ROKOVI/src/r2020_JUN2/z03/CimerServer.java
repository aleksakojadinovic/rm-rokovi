package r2020_JUN2.z03;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class CimerServer {

    private static final class UserInfo{
        String username;
        SelectionKey key;
        ByteBuffer buffer;
        public UserInfo(String username, SelectionKey key) {
            this.username = username;
            this.key = key;
        }
    }

    public static void main(String[] args) {
        Map<String, Integer> state = new HashMap<>();
        try(ServerSocketChannel serverChannel = ServerSocketChannel.open();
            Selector selector = Selector.open();
            ){
            serverChannel.bind(new InetSocketAddress(12345));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            while(true){
                selector.select();
                var it = selector.selectedKeys().iterator();
                while (it.hasNext()){
                    var key = it.next();
                    it.remove();
                    try{
                        if (key.isAcceptable()){
                            ServerSocketChannel s = (ServerSocketChannel)key.channel();
                            SocketChannel c = (SocketChannel)s.accept();
                            c.configureBlocking(false);
                            c.register(selector, SelectionKey.OP_READ);
                        }else if (key.isReadable()){
                            System.err.println("READ...");
                            //Two cases: reading a username, reading a command
                            var userChannel = (SocketChannel)key.channel();
                            if (key.attachment() == null){
                                var i = new UserInfo(null, key);
                                i.buffer = ByteBuffer.allocate(128);
                                key.attach(i);
                            }

                            UserInfo info = (UserInfo)key.attachment();

                            userChannel.read(info.buffer);
                            String partialInput = new String(info.buffer.array(), 0, info.buffer.position());
                            String finalInput = null;
                            System.err.println("Partial input received: " + partialInput + " ...");
                            if (!info.buffer.hasRemaining() || partialInput.contains("\r\n\r\n")){
                                // Done reading whatever.

                                int markerIdx = partialInput.indexOf("\r\n\r\n");
                                if (markerIdx == -1)
                                    markerIdx = partialInput.length();
                                finalInput = partialInput.substring(0, markerIdx);
                                System.err.println("\tFull input received : " + finalInput);
                            }

                            if (finalInput == null)
                                continue;

                            System.err.println("Received from client: " + finalInput);

                            if (info.username == null){
                                System.err.println("\t\tSetting client username for " + finalInput);
                                info.username = finalInput;
                                // TODO: Provera da li je ime zauzeto (nije trazeno?)
                                state.put(info.username, 0);
                                info.buffer.clear();
                                key.interestOps(SelectionKey.OP_READ);
                            }else{
                                String response = null;
                                if (finalInput.equalsIgnoreCase("stanje")){
                                    response = state
                                            .entrySet()
                                            .stream()
                                            .map(e -> e.getKey() + " : " + e.getValue().toString())
                                            .collect(Collectors.joining("\n")) +
                                            "\r\n\r\n";
                                }else{
                                    int amount = Integer.parseInt(finalInput);
                                    state.put(info.username, state.get(info.username) + amount);
                                }

                                if (response == null){
                                    info.buffer = ByteBuffer.allocate(128); // priprema za naredno citanje
                                    continue;
                                }


                                info.buffer = ByteBuffer.wrap(response.getBytes());
                                info.buffer.clear();
                                key.interestOps(SelectionKey.OP_WRITE);
                            }

                            System.err.println("DONE READING!");
                        }else if(key.isWritable()){
                            var info = (UserInfo)key.attachment();
                            var userChannel = (SocketChannel)key.channel();
                            userChannel.write(info.buffer);
                            if (!info.buffer.hasRemaining()){
                                info.buffer = ByteBuffer.allocate(128); // priprema za sledece citanje
                                key.interestOps(SelectionKey.OP_READ);
                            }
                        }

                    }catch (IOException e){
                        System.err.println("Failed to process client: " + e.getMessage());
                        key.cancel();
                    }
                }
            }



        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
