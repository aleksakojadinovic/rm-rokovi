package r2020_JUN2.z03;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class CimerClient {

    public static void main(String[] args) {
        try(var clientChannel = SocketChannel.open(new InetSocketAddress("localhost", 12345));
            var userIn = new Scanner(System.in)){
            clientChannel.configureBlocking(true);
            System.out.print("Ime: ");
            String name = userIn.nextLine();
            var bb = ByteBuffer.wrap((name + "\r\n\r\n").getBytes()).clear();
//            System.err.println("Sending username to server...");
            while(bb.hasRemaining())
                clientChannel.write(bb);
//            System.err.println("username sent!");
            System.err.flush();

            while (true){
                System.out.print("$> ");
                String cmd = userIn.nextLine();
                ByteBuffer toServer = null;
                boolean expectAnswer = false;
                if (cmd.equalsIgnoreCase("kraj")){
                    System.exit(0);
                }
                if (cmd.equalsIgnoreCase("stanje")){
                    toServer = ByteBuffer.wrap((cmd + "\r\n\r\n").getBytes());
                    expectAnswer = true;
                }else{
                    try{
                        var num = Integer.parseInt(cmd);
                        if (num <= 0)
                            continue;
                        toServer = ByteBuffer.wrap((cmd + "\r\n\r\n").getBytes());
                    }catch (Exception e){
                        continue;
                    }
                }
                toServer.clear();
//                System.err.println("Sending to server...");
                while (toServer.hasRemaining()){
//                    System.err.println("\t\tsending...");
                    clientChannel.write(toServer);
                }

//                System.err.println("\tDone sending!");
                if (expectAnswer){
                    final StringBuilder sbServerResponse = new StringBuilder();
                    ByteBuffer bbServerResponse = ByteBuffer.allocate(4096);
//                    System.err.println("Waiting for response...");
                    while (true){
                        clientChannel.read(bbServerResponse);
                        String partialResponse = new String(bbServerResponse.array(), 0, bbServerResponse.position());
//                        System.err.println("\t\tPartial response: " + partialResponse);
                        sbServerResponse.append(partialResponse.replace("\r\n\r\n", ""));
                        if (partialResponse.contains("\r\n\r\n"))
                            break;
                    }
//                    System.err.println("\tFetched response!");
                    System.out.println(sbServerResponse.toString());

                }

            }

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }


}
