package i2020_JAN1.z01;

import java.io.*;
import java.net.Socket;
import java.rmi.ServerError;
import java.util.Scanner;

public class ChessClient {
    private final Socket clientSocket;

    public void mainLoop(){
        try(var networkIn  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            var networkOut = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
            var userIn     = new Scanner(System.in)){
            System.out.println("Client started...");
            while (true){
                System.out.print("> ");
                String command = userIn.nextLine();
                if (command.equalsIgnoreCase("bye")){
                    System.exit(0);
                }
                networkOut.println(command);
                String response = networkIn.readLine();
                System.out.println(response);

            }

        } catch (IOException e) {
            System.err.println("Client IO error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Failed to close connection: " + e.getMessage());
            }
        }


    }

    public ChessClient(String hostname, int port) throws IOException {
        clientSocket = new Socket(hostname, port);
    }

    private static final String HOSTNAME = "localhost";
    private static final int    PORT     = 1996;

    public static void main(String[] args) {
        try {
            var c = new ChessClient(HOSTNAME, PORT);
            c.mainLoop();
        } catch (IOException e) {
            System.err.println("Failed to start client: " + e.getMessage());
        }
    }
}
