package r2020_SEP1.z03;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class WordCounter {

    private static final class FileProcessor implements Runnable{

        private final File file;
        private final Map<String, Integer> finalMap;
        private final Map<String, Integer> localMap;

        public FileProcessor(File file, Map<String, Integer> finalMap) {
            this.file = file;
            this.finalMap = finalMap;
            this.localMap = new HashMap<>();
        }

        public void mergeIntoFinal(){
            synchronized (finalMap){
                for (var entry: localMap.entrySet()){
                    String localWord = entry.getKey();
                    int localCount = entry.getValue();
                    finalMap.putIfAbsent(localWord, 0);
                    finalMap.put(localWord, finalMap.get(localWord) + localCount);
                }
            }
        }

        @Override
        public void run() {
            try (var fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))){
                String line;
                while ((line = fileReader.readLine()) != null){
                     Arrays
                            .stream(line.split(" "))
                            .map(String::toLowerCase)
                            .forEach(w -> {
                                localMap.putIfAbsent(w, 0);
                                localMap.put(w, localMap.get(w) + 1);
                            });

                }

                mergeIntoFinal();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void traversePath(Path p) throws IOException, InterruptedException {
        List<Thread> fileProcessThreads = new ArrayList<>();
        Map<String, Integer> wordCounts = new HashMap<>();
        Files.walk(p)
                .map(Path::toFile)
                .filter(file -> !file.isDirectory() && file.getName().endsWith(".txt"))
                .forEach(file -> {
                    var t = new Thread(new FileProcessor(file, wordCounts));
                    fileProcessThreads.add(t);
                    t.start();
                });
        for (var t: fileProcessThreads)
            t.join();

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(wordCounts.entrySet());
        entries.sort(Comparator.comparingInt(Map.Entry::getValue));
        Collections.reverse(entries);
        for (var entry: entries){
            System.out.printf("%s : %d\r\n", entry.getKey(), entry.getValue());
        }

    }

    public static void main(String[] args) {
        try(Scanner filePathIn = new Scanner(System.in)){
            String filePath = filePathIn.nextLine();
            Path p = Paths.get(filePath).toAbsolutePath();
            traversePath(p);
        }catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
    }
}
