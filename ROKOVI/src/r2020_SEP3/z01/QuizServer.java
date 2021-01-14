package r2020_SEP3.z01;

import r2020_SEP1.z01.Client;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLOutput;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

public class QuizServer {

    // MEMBERS:
    private final ServerSocket serverSocket;

    private boolean questionsLoaded;
    private List<Question> questions;
    int atQuestion = -1;
    private final Map<String, Integer> points;

    private final Lock lock = new ReentrantLock();
    private final Condition notEnoughPlayers = lock.newCondition();

    private int numberOfPlayers;
    private final int minNumberOfPlayers;


    public QuizServer(int port) throws IOException{
        serverSocket = new ServerSocket(port);
        questionsLoaded = false;
        numberOfPlayers = 0;
        minNumberOfPlayers = 2;
        points = new TreeMap<>();
    }

    public synchronized void incrementNumberOfPlayers(){
        numberOfPlayers++;
        if (numberOfPlayers == minNumberOfPlayers){
            lock.unlock();
            lock.notifyAll();
        }

    }

    public synchronized int getNumberOfPlayers() {
        return numberOfPlayers;
    }

    public void loadQuestions(String filepath) throws ParseException{
        try(Scanner in = new Scanner(new FileInputStream(filepath))){
            questions = new ArrayList<>();
            while(in.hasNextLine()){
                questions.add(Question.fromScanner(in));
            }
        }catch (Exception e){
            throw new ParseException(e.getMessage(), 0);
        }

        questionsLoaded = true;
    }

    public void mainLoop() throws IllegalStateException{
        if (!questionsLoaded){
            throw new IllegalStateException("Questions not loaded.");
        }
        System.out.println("Questions: ");
        questions.forEach(System.out::println);
        System.out.println(" >> Starting server main loop... << ");
        lock.lock();
        while(getNumberOfPlayers() < minNumberOfPlayers){
            try {
                var clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(this, clientSocket)).start();
            } catch (IOException e) {
                System.out.println("Failed to accept client. Reason: " + e.getMessage());
            }
        }
        System.out.println("All clients accepted, now starting the game.");




    }


    // STATICS:
    private static final int port = 12321;
    private static final class Question{
        private final int    id;
        private final int    correct;
        private final String questionText;
        private final String answer1;
        private final String answer2;
        private final String answer3;
        private final String answer4;

        public Question(int id, int correct, String questionText, String answer1, String answer2, String answer3, String answer4) {
            this.id = id;
            this.correct = correct;
            this.questionText = questionText;
            this.answer1 = answer1;
            this.answer2 = answer2;
            this.answer3 = answer3;
            this.answer4 = answer4;
        }

        public static Question fromScanner (Scanner in) throws ParseException {
            int id = 0;
            int correct = 0;
            String questionText = "";
            String answer1 = "";
            String answer2 = "";
            String answer3 = "";
            String answer4 = "";
            try{
                String firstLine = in.nextLine();
//                System.out.println("\tRead first line: " + firstLine);
                id = Integer.parseInt(firstLine.substring(0, firstLine.indexOf('.')));
                questionText = firstLine.substring(firstLine.indexOf('.') + 2, firstLine.length());

                // I know...
                int starIdx;
                int upTo;

                String a1Text = in.nextLine();
                starIdx = a1Text.lastIndexOf('*');
                upTo = a1Text.length();
                if (starIdx != -1){
                    correct = 1;
                    upTo = starIdx;
                }
                answer1 = a1Text.substring(a1Text.indexOf(')') + 2, upTo);
//                System.out.println("\t\tRead a1: " + answer1);


                String a2Text = in.nextLine();
                starIdx = a2Text.lastIndexOf('*');
                upTo = a2Text.length();
                if (starIdx != -1){
                    correct = 2;
                    upTo = starIdx;
                }
                answer2 = a2Text.substring(a2Text.indexOf(')') + 2, upTo);
//                System.out.println("\t\tRead a2: " + answer2);

                String a3Text = in.nextLine();
                starIdx = a3Text.lastIndexOf('*');
                upTo = a3Text.length();
                if (starIdx != -1){
                    correct = 3;
                    upTo = starIdx;
                }
                answer3 = a3Text.substring(a3Text.indexOf(')') + 2, upTo);
//                System.out.println("\t\tRead a3: " + answer3);

                String a4Text = in.nextLine();
                starIdx = a4Text.lastIndexOf('*');
                upTo = a4Text.length();
                if (starIdx != -1){
                    correct = 4;
                    upTo = starIdx;
                }
                answer4 = a4Text.substring(a4Text.indexOf(')') + 2, upTo);
//                System.out.println("\t\tRead a4: " + answer4);
                return new Question(id, correct, questionText, answer1, answer2, answer3, answer4);


            }catch (Exception e){
                System.out.printf("Parsed so far: id=%d\r\ntext=%s\r\na1=%s\r\na2=%s\r\na3=%s\r\na4=%s\r\n",
                        id,
                        questionText,
                        answer1,
                        answer2,
                        answer3,
                        answer4);
                throw new ParseException("Invalid input file. Reason: " + e.getMessage(), 0);
            }

        }

        public boolean checkAnswer(int answerNumber){
            return answerNumber == correct;
        }

        public boolean checkAnswer(String answerLetter){
            return switch (answerLetter) {
                case "A)" -> correct == 1;
                case "B)" -> correct == 2;
                case "C)" -> correct == 3;
                case "D)" -> correct == 4;
                default -> false;
            };
        }

        @Override
        public String toString() {
            return id + ". " + questionText +
                    "\r\nA) " + answer1 +
                    "\r\nB) " + answer2 +
                    "\r\nC) " + answer3 +
                    "\r\nD) " + answer4;
        }
    }
    private static final class ClientHandler implements Runnable{
        private final QuizServer myServer;
        private final Socket     clientSocket;

        public ClientHandler(QuizServer myServer, Socket clientSocket) {
            this.myServer = myServer;
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            var tag = clientSocket.getPort();
            System.out.printf("[%s] Starting to process client...\r\n", tag);
            try(var clientIn  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                var clientOut = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()))){
                System.out.printf("\t[%s] Waiting for client's username...\r\n", tag);
                String clientUsername = clientIn.readLine();
                System.out.printf("\t\t[%s] Got client %s\r\n", tag, clientUsername);
                myServer.points.put(clientUsername, 0);
                myServer.incrementNumberOfPlayers();

                System.out.printf("\t[%s] Awaiting other clients...", clientUsername);
                myServer.lock.lock();
                myServer.lock.wait();
                System.out.printf("\t\t[%s] Everyone here, let's start!", clientUsername);

            } catch (IOException | InterruptedException e) {
                System.err.println("Error while processing client: " + e.getMessage());
            }finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error while closing client socket: " + e.getMessage());
                }
            }

        }
    }

    public static void main(String[] args) {
        try{
            var s = new QuizServer(port);
            s.loadQuestions("./ROKOVI/src/r2020_SEP3/z01/in.txt");
            s.mainLoop();
        }catch (IOException | ParseException e){
            System.err.println("Failed to start server, reason: " + e.getMessage());
        }
    }
}
