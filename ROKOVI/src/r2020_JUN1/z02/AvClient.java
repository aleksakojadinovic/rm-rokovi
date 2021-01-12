package r2020_JUN1.z02;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class AvClient {
    private final Socket clientSocket;

    public AvClient(final String hostname, final int port) throws IOException {
        clientSocket = new Socket(hostname, port);
    }

    public void startClient(){
        try (BufferedReader networkIn  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter    networkOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())), true);
             Scanner        userIn     = new Scanner(System.in)){

            String userInput;
            while (true){
                System.out.println("Number: ");
                userInput = userIn.nextLine();
                networkOut.println(userInput);
                if (userInput.equalsIgnoreCase("end")){
                    break;
                }
            }
            String fromServer = networkIn.readLine();
            System.out.println(fromServer);
        }catch (IOException e){
            System.err.println("Client crash: " + e.getMessage());
        }finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Failed to close socket: " + e.getMessage());
            }
        }
    }

    private static final int    PORT = 10000;
    private static final String HOSTNAME = "localhost";
    public static void main(String[] args) {
        try {
            new AvClient(HOSTNAME, PORT).startClient();
        } catch (IOException e) {
            System.err.println("Failed to start client: " + e.getMessage());
        }
    }
}
