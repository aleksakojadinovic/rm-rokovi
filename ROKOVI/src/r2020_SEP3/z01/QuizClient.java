package r2020_SEP3.z01;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class QuizClient {
    private final Socket socket;

    public void mainLoop(){
        try(var networkIn  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            var networkOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            var userIn     = new Scanner(System.in)) {

            System.out.println("Enter username: ");
            String username = userIn.nextLine().trim();
            networkOut.println(username);
            networkOut.flush();


        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
            e.printStackTrace();
        }finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Failed to close client socket: " + e.getMessage());
            }
        }
    }

    public QuizClient(String hostname, int port) throws IOException {
        socket = new Socket(hostname, port);
    }


    public static void main(String[] args) {
        try {
            var c = new QuizClient(HOSTNAME, PORT);
            c.mainLoop();
        } catch (IOException e) {
            System.err.println("Failed to start client: " + e.getMessage());
        }


    }

    private static final String HOSTNAME = "localhost";
    private static final int PORT        = 12321;
}
