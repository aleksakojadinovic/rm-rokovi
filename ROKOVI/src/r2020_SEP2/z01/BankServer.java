package r2020_SEP2.z01;

import java.awt.image.WritableRenderedImage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BankServer {
    private ServerSocketChannel serverChannel;
    private final int port;

    private enum UserWriteMode{
        LIST_OF_OTHERS,
        RECEIVED_MONEY,
        CONFIRMATION,
        IDLE
    }

    private static final class UserAttachment{
        boolean accountNumberValid;
        UserWriteMode currentWriteMode;
        int accountNumber;
        ByteBuffer buffer;
        SelectionKey key;

        public UserAttachment(SelectionKey key, int accountNumber, ByteBuffer buffer) {
            this.key = key;
            accountNumberValid = false;
            currentWriteMode = null;
            this.accountNumber = accountNumber;
            this.buffer = buffer;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserAttachment that = (UserAttachment) o;
            return key.equals(that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }

        @Override
        public String toString() {
            return String.format("Account: %d", accountNumber);
        }
    }



    public void mainLoop(){
        List<UserAttachment> allActiveUsers = new ArrayList<>();
        Selector selector = null;
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(port));

            System.out.println("Server running...");

            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (true){
                selector.select();
                var keys = selector.selectedKeys();
                var it = keys.iterator();
                while(it.hasNext()){
                    var key = it.next();
                    it.remove();
                    try{
                        if (key.isAcceptable()){
                            var keyChannel = (ServerSocketChannel)(key.channel());
                            var userChannel = keyChannel.accept();
                            userChannel.configureBlocking(false);
                            userChannel.register(selector, SelectionKey.OP_READ);
                        }else if (key.isReadable()){
                            var userChannel = (SocketChannel)key.channel();
                            if (key.attachment() == null){
                                var a = new UserAttachment(key,-1, ByteBuffer.allocate(4));
                                key.attach(a);
                                allActiveUsers.add(a);
                            }

                            var userState = (UserAttachment)key.attachment();
                            if (!userState.accountNumberValid){
                                //We still haven't read the user's accounts number
                                userChannel.read(userState.buffer);
                                if (!userState.buffer.hasRemaining()){
                                    // We've read the entire user's account number
                                    userState.buffer.clear();
                                    userState.accountNumber = userState.buffer.asIntBuffer().get();
                                    userState.accountNumberValid = true;

                                    // We've read the user's username
                                    // Now we need to send them a list of all other users.
//                                    allActiveUsers.stream().map()

                                    userState.buffer = ByteBuffer.wrap((allActiveUsers.stream().map(a -> a.toString() + "\r\n").collect(Collectors.joining()) + "\r\n\r\n").getBytes());
                                    userState.buffer.clear(); // Just in case
                                    userState.currentWriteMode = UserWriteMode.LIST_OF_OTHERS;
                                    key.interestOps(SelectionKey.OP_WRITE);
                                }

                            }else{
                                //TODO: the user is asking for a transaction.
                                userChannel.read(userState.buffer);
                                if (!userState.buffer.hasRemaining()){
                                    // We've got one transaction.
                                    userState.buffer.clear();
                                    var userDataIn = userState.buffer.asIntBuffer();
                                    int toAccount = userDataIn.get(0);
                                    int amount    = userDataIn.get(1);

                                    if (toAccount == -1 || amount == -1){
                                        userState.currentWriteMode = UserWriteMode.CONFIRMATION;
                                        userState.buffer = ByteBuffer.wrap("OK\r\n\r\n".getBytes());
                                        userState.buffer.clear();
                                        key.interestOps(SelectionKey.OP_WRITE);
                                        continue;
                                    }

                                    String responseToUser = "";

                                    UserAttachment targetUser = null;
                                    boolean exists = false;
                                    boolean isIdle = false;
                                    for (var ta: allActiveUsers){
                                        if (ta.accountNumber != toAccount)
                                            continue;
                                        targetUser = ta;
                                        exists = true;
                                        isIdle = targetUser.currentWriteMode == UserWriteMode.IDLE;
                                        break;

                                    }
                                    if (!exists){
                                        responseToUser = "This user doesn't exist.";
                                    }else if (!isIdle){
                                        responseToUser = "This user is busy.";
                                    }else{
                                        responseToUser = String.format("Successfully transferred $%d to account %d.\r\n\r\n", amount, toAccount);
                                    }

                                    userState.currentWriteMode = UserWriteMode.CONFIRMATION;
                                    userState.buffer = ByteBuffer.wrap(responseToUser.getBytes());
                                    userState.buffer.clear();
                                    key.interestOps(SelectionKey.OP_WRITE);
                                    if (exists && isIdle){
                                        targetUser.key.interestOps(SelectionKey.OP_WRITE);
                                        targetUser.currentWriteMode = UserWriteMode.RECEIVED_MONEY;
                                        targetUser.buffer = ByteBuffer.wrap(String.format("You received a transaction from %d, amount of %d.\r\n\r\n", userState.accountNumber, amount).getBytes());
                                    }

                                }


                            }
                        }else if (key.isWritable()){
                            var userChannel = (SocketChannel)key.channel();
                            var userState = (UserAttachment)key.attachment();
                            if (userState.currentWriteMode == UserWriteMode.LIST_OF_OTHERS){
                                System.out.println("Sending user list...");
                                userChannel.write(userState.buffer);
                                if (!userState.buffer.hasRemaining()){
                                    System.out.println("User list sent!");
                                    // Done sending list of all users. Now it's time to receive a transaction
                                    key.interestOps(SelectionKey.OP_READ);
                                    userState.buffer = ByteBuffer.allocate(8); //two ints
                                }
                            }else if (userState.currentWriteMode == UserWriteMode.RECEIVED_MONEY){
                                // Send transactions to this user
                                userChannel.write(userState.buffer);
                                if (!userState.buffer.hasRemaining()){
                                    userState.currentWriteMode = UserWriteMode.IDLE;
                                    key.interestOps(0);
                                }
                            }else if (userState.currentWriteMode == UserWriteMode.CONFIRMATION){
                                userChannel.write(userState.buffer);
                                if (!userState.buffer.hasRemaining()){
                                    // Done sending confirmation of a transaction.
                                    userState.currentWriteMode = UserWriteMode.IDLE;
                                    key.interestOps(0);
                                }
                            }
                        }
                    }catch (IOException e){
                        System.err.println("Failed to process client, reason: " + e.getMessage());
                        allActiveUsers.removeIf(a -> a.key == key);
                        key.cancel();
                        try{
                            key.channel().close();
                        }catch (Exception e1){
                            System.err.println("Failed to close client, reason: " + e1.getMessage());
                        }

                    }


                }
            }

        } catch (IOException e) {
            System.err.println("Failed to start server " + e.getMessage());
            e.printStackTrace();
        }finally {
            try {
                serverChannel.close();
            } catch (IOException e) {
                System.err.println("Failed to close server socket channel: " + e.getMessage());
            }
        }
    }

    public BankServer(int port) {
        this.port = port;
    }

    private static final int PORT = 12221;

    public static void main(String[] args) {
        var s = new BankServer(PORT);
        s.mainLoop();
    }
}
