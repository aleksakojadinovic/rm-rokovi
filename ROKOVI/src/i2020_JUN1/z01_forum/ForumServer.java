package i2020_JUN1.z01_forum;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ForumServer {
    private static final class Post{
        final int postId;
        final String postTitle;
        final String postContent;
        final List<String> postReplies;

        public Post(int postId, String postTitle, final String postContent) {
            this.postId = postId;
            this.postTitle = postTitle;
            this.postContent = postContent;
            postReplies = new ArrayList<>();
        }

        public void reply(final String content){
            postReplies.add(content);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(postId).append(": ").append(postTitle).append("\r\n");
            sb.append("   # ").append(postContent).append("\r\n");
            postReplies.forEach(r -> sb.append("   - ").append(r).append("\r\n"));
            return sb.toString();
        }
    }


    private static final class ClientHandler implements Runnable{
        final ForumServer    server;
        final BufferedReader clientIn;
        final PrintWriter clientOut;
        public ClientHandler(final Socket clientSocket, final ForumServer server) throws IOException{
            clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            clientOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8)));
            this.server = server;
        }

        @Override
        public void run(){
            while(true) {
                try {
                    System.out.println("Starting client processing.");
                    String clientInput = clientIn.readLine();

                    var clientCommands = clientInput.split(" ");
                    if (clientCommands.length < 1) {
                        clientOut.write("No input");
                        clientOut.println("END");
                        clientOut.flush();
                        return;
                    }


                    var allPosts = server.getPosts();
                    if (clientCommands[0].equalsIgnoreCase("list")) {
                        System.out.println(" >> Sending list to client...");
                        synchronized (allPosts) {
                            allPosts.stream().forEach(p -> clientOut.println(p.toString()));
                        }
                    } else if (clientCommands[0].equalsIgnoreCase("reply")) {
                        if (clientCommands.length != 3) {
                            clientOut.println("Invalid input.");
                        } else {
                            System.out.println(" >> Client replying...");
                            int postIdx;
                            try {
                                postIdx = Integer.parseInt(clientCommands[1]) - 1;
                                synchronized (allPosts) {
                                    if (postIdx >= allPosts.size()) {
                                        clientOut.println("No such post.");
                                    } else {
                                        allPosts.get(postIdx).reply(clientCommands[2]);
                                        clientOut.println(allPosts.get(postIdx));
                                    }
                                }

                            } catch (NumberFormatException e) {
                                clientOut.println("Post id not a number.");
                            }
                        }

                        clientOut.println("END");
                    } else if (clientCommands[0].equalsIgnoreCase("post")) {
                        if (clientCommands.length != 3){
                            clientOut.println("invalid post.");
                        }
                        var postTitle = clientCommands[1];
                        var postContent = clientCommands[2];
                        synchronized (allPosts){
                            allPosts.add(new Post(allPosts.size() + 1, postTitle, postContent));
                        }
                        clientOut.println("successfully posted.");
                    } else {
                        clientOut.println("Unknown: " + clientCommands[0]);
                    }
                    System.out.println("Flushing...");
                    clientOut.println("END");
                    clientOut.flush(); // not sure if needed

                } catch (IOException e) {
                    System.err.println("Error reading client input: " + e.getMessage());
                    clientOut.close();
                    try {
                        clientIn.close();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    return;
                }
            }
        }
    }


    private final ServerSocket serverSocket;
    private final List<Post>   posts;

    private ForumServer(int port) throws IOException{
        posts = Collections.synchronizedList(new ArrayList<>());
        posts.add(new Post(1, "Hey", "Sup"));
        posts.get(0).reply("Replying to yoiu");
        posts.get(0).reply("Replying to yoiu again");
        posts.get(0).reply("ehehehe");
        posts.add(new Post(2, "eyeye", "qqqqqq"));
        posts.get(1).reply("You have one too...");
        serverSocket = new ServerSocket(port);


    }

    public List<Post> getPosts() {
        return posts;
    }

    public void addPost(final Post post){
        posts.add(post);
    }

    private void execute(){
        System.out.println("Server started at: " + serverSocket.getInetAddress().getHostName() + ":" + serverSocket.getLocalPort());
        while(true){
            try{
                new Thread(new ClientHandler(serverSocket.accept(), this)).start();
            }catch(IOException e){
                System.err.println("Failed to process client: " + e.getMessage());
            }
        }

    }


    private static final int PORT = 7337;

    public static void main(String[] args) {
        try{
            new ForumServer(PORT).execute();
        }catch (IOException e){
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }



}
