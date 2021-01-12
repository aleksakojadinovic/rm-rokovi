package i2020_JUN1.z01_forum;


import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ForumClient {
    private final String hostname;
    private final int    port;

    private final Socket         clientSocket;
    private final BufferedReader networkIn;
    private final PrintWriter networkOut;
    private final BufferedReader clientIn;

    private ForumClient(String hostname, int port) throws IOException {
        this.hostname = hostname;
        this.port     = port;

        clientSocket = new Socket(hostname, port);
        networkIn    = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        networkOut   = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8)));
        clientIn     = new BufferedReader(new InputStreamReader(System.in));
    }


    private void execute(){
        try{
            while(true){
                System.out.print("$> ");
                String userQuery = clientIn.readLine();
                if (userQuery.isBlank()){
                    continue;
                }
                if (userQuery.equalsIgnoreCase("bye")){
                    clientSocket.close();
                    break;
                }
                String [] userQueryCommands;
                if (userQuery.equalsIgnoreCase("list")){
                    sendRequest("list");
                }else if (userQuery.startsWith("reply")){
                    userQuery = userQuery.replaceFirst("reply", "");
                    System.out.println(userQuery);
                    userQueryCommands = userQuery.split(" ");
                    if (userQueryCommands.length < 2){
                        System.err.println("reply expects 3 args");
                        continue;
                    }
                    int toId;
                    try {
                        toId = Integer.parseInt(userQueryCommands[0]);
                    }catch(NumberFormatException e){
                        System.err.println("Invalid post id: " + userQueryCommands[0]);
                        continue;
                    }
                    String msg = Arrays.stream(userQueryCommands).skip(1)
                                                                 .collect(Collectors.joining());


                    sendRequest("reply " + toId + " " + msg.replaceAll("\"", ""));

                }else if (userQuery.startsWith("post")){
                    var titleAndContentSplit = Arrays.stream(userQuery.split(" "))
                                                        .skip(1)
                                                        .collect(Collectors.joining(" "))
                                                        .split("\" \"");
                    if (titleAndContentSplit.length != 2){
                        System.err.println("Invalid syntax");
                        continue;
                    }
                    var postId = titleAndContentSplit[0].replace("\"", "");
                    var content = titleAndContentSplit[1].replace("\"", "");
                    sendRequest("post " + postId + " " + content);
                }else{
                    System.err.println("Unknown command: " + userQuery);
                }
            }
        }catch(IOException e){
            try {
                clientSocket.close();
            }catch (IOException e2){
                e2.printStackTrace();
            }
            System.err.println("Client execution error: " + e.getMessage());
        }

    }

    private void printServerResponse() throws IOException{
        String s;
        while((s = networkIn.readLine()) != null){
            if (s.equals("END"))
                break;
            System.out.println(s);
        }
    }
    private void sendRequest(final String req){
        try{
            networkOut.println(req);
            networkOut.flush();
            printServerResponse();
        }catch (IOException e){
            System.err.println("Failed to perform " + req + ", reason: " + e.getMessage());
        }
    }

    private static final String HOSTNAME = "localhost";
    private static final int    PORT     = 7337;

    private List<String> extractMessages(final String input, int skip){
        return null;
    }

    public static void main(String[] args) {
        try {
            new ForumClient(HOSTNAME, PORT).execute();
        }catch (IOException e){
            System.err.println("Failed to start client: " + e.getMessage());
        }
    }



}
