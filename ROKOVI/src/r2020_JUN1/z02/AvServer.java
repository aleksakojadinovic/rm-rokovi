package r2020_JUN1.z02;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class AvServer {
    private final ServerSocket      serverSocket;
    private final PrintWriter       stdout;

    public AvServer(final int port) throws IOException {
        serverSocket = new ServerSocket(port);
        stdout = new PrintWriter(new OutputStreamWriter(System.out), true);
    }

    /**
     * The runnable that handles the communication with one client.
     * The responsibility of closing the socket is up to this class, not the server.
     */
    private static final class ClientHandler implements Runnable{
        private final Socket            clientSocket;
        private final PrintWriter       stdout;
        private PrintWriter             clientOut;


        public ClientHandler(final AvServer server, final Socket clientSocket, final PrintWriter stdout) {
            this.clientSocket = clientSocket;
            this.stdout = stdout;

        }
        @Override
        public void run() {
            try {
                BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                clientOut = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
                String userInput;
                double sum = 0;
                int read = 0;
                boolean error = false;
                String errorMsg = "";

                while ((userInput = clientIn.readLine()) != null){
                    if (userInput.equalsIgnoreCase("end"))
                        break;
                    if (error)
                        continue;
                    double num;
                    try{
                        num = Double.parseDouble(userInput);
                        sum += num;
                        read++;
                        stdout.printf("[%s-%d] Received number: %f\r\n", clientSocket.getInetAddress(), clientSocket.getPort(), num);
                    } catch(NumberFormatException e){
                        error = true;
                        errorMsg = "Couldn't parse number " + userInput;
                    }
                }
                if (error){
                    clientOut.println(errorMsg);
                }else if (read == 0){
                    clientOut.println("Cannot find average of 0 numbers.");
                }else {
                    clientOut.println(sum/read);
                }
            }catch(IOException e) {
                System.err.println("Error while processing client: " + e.getMessage());
            }
            finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Failed to close client socket: " + e.getMessage());
                }
            }
        }
    }

    private void mainLoop(){
        stdout.printf("Server running at %s:%d\r\n", serverSocket.getInetAddress().getHostAddress(), serverSocket.getLocalPort());

        while (true){
            try{
               Socket clientSocket = serverSocket.accept();
               new Thread(new ClientHandler(this, clientSocket, stdout)).start();
            }catch (IOException e){
                System.err.println("\tAccepting client failed, reason: " + e.getMessage());
            }

        }
    }

    private static final int PORT = 10000;
    public static void main(String[] args) {
        try {
            var server = new AvServer(PORT);
            server.mainLoop();
        } catch (IOException e) {
            System.err.println("Couldn't start server: " + e.getMessage());
        }
    }

}
