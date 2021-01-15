package i2020_JAN1.z01;

import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ChessServer {
    private final ServerSocket serverSocket;

    private static final class ChessPlayer{
        int id;
        String name;
        int elo;

        public ChessPlayer(int id, String name, int elo) {
            this.id = id;
            this.name = name;
            this.elo = elo;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChessPlayer that = (ChessPlayer) o;
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return name + " " + elo;
        }

        private void changeElo(int delta){
            elo += delta;
        }
    }

    private final List<ChessPlayer> players;

    private static final class ClientHandler implements Runnable{
        private final Socket clientSocket;
        private final ChessServer server;

        public ClientHandler(ChessServer server, Socket clientSocket) {
            this.clientSocket = clientSocket;
            this.server = server;
        }

        private String processSelect(int id){
            int idx = id - 1;
            synchronized (server.players) {
                if (idx > server.players.size() - 1)
                    return "A player with that ID does not exist.";
                return server.players.get(idx).toString();
            }
        }

        private String processInsert(String name){
            synchronized (server.players){
                var newPlayer = new ChessPlayer(server.players.size(), name, 1300);
                server.players.add(newPlayer);
                return "Added new player :: " + newPlayer.toString();
            }

        }

        private String processUpdate(int id, int delta){
            int idx = id - 1;
            synchronized (server.players) {
                if (idx > server.players.size() - 1)
                    return "A player with that ID does not exist.";
                server.players.get(idx).changeElo(delta);
                return "Updated player :: " + server.players.get(idx).toString();
            }
        }

        private String processCommand(String clientCommand){
            var split = clientCommand.split(" ");
            if (split.length < 2 || split.length > 3){
                return "Syntax error.";
            }
            if (split[0].equalsIgnoreCase("sel")){
                if (split.length != 2){
                    return "sel expects 1 argument but " + (split.length - 1) + " given.";
                }
                try{
                    int id = Integer.parseInt(split[1]);
                    return processSelect(id);
                }catch (Exception e){
                    return "sel expects integer but " + split[1] + " given.";
                }
            }else if (split[0].equalsIgnoreCase("ins")){
                return processInsert(clientCommand.replaceFirst("ins", ""));
            }else if (split[0].equalsIgnoreCase("upd")){
                if (split.length != 3){
                    return "upd expects 2 args but " + (split.length - 1) + " given.";
                }
                try{
                    int id = Integer.parseInt(split[1]);
                    int de = Integer.parseInt(split[2]);
                    return processUpdate(id, de);
                }catch (Exception e){
                    return "update expects two integers but " + split[1] + " and " + split[2] + " given.";
                }
            }else{
                return "Unknown command: " + split[0];
            }

        }

        @Override
        public void run() {
            try(var clientIn  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                var clientOut = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true)
                ){
                while (true){
                    String clientCommand = clientIn.readLine();
                    clientOut.println(processCommand(clientCommand));
                }

            }catch (IOException e){
                System.err.println("Failed to process client: " + e.getMessage());
            }finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Failed to close client socket:" + e.getMessage());
                }
            }
        }
    }

    public void mainLoop(){
        while (true){
            try{
                var clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(this, clientSocket)).start();
            }catch (IOException e){
                System.err.println("Failed to accept client " + e.getMessage());
            }
        }
    }

    public ChessServer(int port) throws IOException {
        players = new ArrayList<>();
        serverSocket = new ServerSocket(port);
    }

    private static final int PORT = 1996;

    public static void main(String[] args) {
        try {
            var s = new ChessServer(PORT);
            s.mainLoop();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }


    }
}
