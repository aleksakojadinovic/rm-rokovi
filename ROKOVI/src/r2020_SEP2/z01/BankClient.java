package r2020_SEP2.z01;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class BankClient {

    private final SocketChannel channel;


    public void mainLoop(){
        try(Scanner userIn = new Scanner(System.in)){
            ByteBuffer accountNumberBuffer     = ByteBuffer.allocate(4);
            ByteBuffer allAccountsBuffer       = ByteBuffer.allocate(32);
            ByteBuffer transferOutBuffer       = ByteBuffer.allocate(8);
            ByteBuffer transferSuccessBuffer   = ByteBuffer.allocate(64);
            ByteBuffer transferInBuffer        = ByteBuffer.allocate(64);

            System.out.print("Account number: ");
            int accountNumber = userIn.nextInt();
            accountNumberBuffer.asIntBuffer().put(accountNumber);
            accountNumberBuffer.clear();
            System.out.println("Sending account number to server...");
            channel.write(accountNumberBuffer);
            System.out.println("\tAccount number sent to server.");

            System.out.println("Awaiting client list from server...");

            final StringBuilder allAccounts = new StringBuilder();
            while (true){
                allAccountsBuffer.clear();
                channel.read(allAccountsBuffer);
                String maybeAllAccounts = new String(allAccountsBuffer.array(), 0, allAccountsBuffer.position());
                allAccounts.append(maybeAllAccounts.replace("\r\n\r\n", ""));
                if (maybeAllAccounts.contains("\r\n\r\n")){
                    System.out.println("Full data received, done reading.");
                    break;
                }else{
                    System.out.println("\tPartial data received, continue reading.");
                }
            }
            System.out.println("Client list: ");
            System.out.println(allAccounts.toString());



            System.out.print("Account to transfer to and amount: (-1 -1 for nothing) ");
            userIn.nextLine();
            String toAccountAndAmount = userIn.nextLine();

            var dataIn = toAccountAndAmount.split(" ");
            int toAccount = Integer.parseInt(dataIn[0]);
            int amount    = Integer.parseInt(dataIn[1]);
            transferOutBuffer.asIntBuffer().put(toAccount).put(amount);
            transferOutBuffer.clear();
            System.out.println("Sending transfer request to server...");
            channel.write(transferOutBuffer);
            System.out.println("\tTransfer request sent.");

            System.out.println("Awaiting server response...");
            channel.read(transferSuccessBuffer);
            transferSuccessBuffer.clear();
            String serverResponse = new String(transferSuccessBuffer.array());
            serverResponse = serverResponse.split("\r\n\r\n")[0];
            System.out.println("\tServer replied: ");
            System.out.println(serverResponse);


            System.out.println("Awaiting incoming transactions...");
            while (true){
                channel.read(transferInBuffer);
                transferInBuffer.clear();
                System.out.println("\t" + new String(transferInBuffer.array(), 0, transferInBuffer.position()));
            }

        }catch(IOException e){
            System.err.println("Client IO error: " + e.getMessage());
            e.printStackTrace();
        }catch (Exception e){
            System.err.println("Unknown client error: " + e.getMessage());
            e.printStackTrace();
        }finally {
            try {
                channel.close();
            } catch (IOException e) {
                System.err.println("Couldn't close client channel: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    public BankClient(String hostname, int port) throws IOException {

        channel = SocketChannel.open(new InetSocketAddress(hostname, port));
        channel.configureBlocking(true);


    }

    private static final String HOSTNAME = "localhost";
    private static final int    PORT     = 12221;
    public static void main(String[] args) {
        try{
            var c = new BankClient(HOSTNAME, PORT);
            c.mainLoop();
        }catch (IOException e){
            System.err.println("Failed to start client: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
