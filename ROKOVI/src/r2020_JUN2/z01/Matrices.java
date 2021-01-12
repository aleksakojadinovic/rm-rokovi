package r2020_JUN2.z01;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

public class Matrices {
    private static final String PATH_PREFIX = "./src/r2020_JUN2/z01/";

    private static void printMatrix(final List<List<Integer>> matrix){
        for (var row: matrix){
            for (var x: row){
                System.out.print(x + " ");
            }
            System.out.println();
        }
    }

    private static List<List<Integer>> getTransposeEmpty(final List<List<Integer>> matrix){
        List<List<Integer>> res = new ArrayList<>();
        for (int i = 0; i < matrix.get(0).size(); i++){
            res.add(new ArrayList<>());
            for (int j = 0; j < matrix.size(); j++){
                res.get(i).add(0);
            }
        }
        return res;
    }

    private static List<List<Integer>> parseOneMatrix(final Scanner in){
        if (!in.hasNext()){
            return null;
        }
        final List<List<Integer>> matrix = new ArrayList<>();

        try{
            final String firstLine = in.nextLine();
            if (firstLine == null)
                throw new IOException("File bad.");
            final var firstRow = Arrays.stream(firstLine.split(" "))
                                            .map(Integer::parseInt)
                                            .collect(Collectors.toList());

            final var numColumns = firstRow.size();
            matrix.add(firstRow);
            while (in.hasNextLine()){
                var currentRowLine = in.nextLine();
                if (currentRowLine.isEmpty())
                     break;
                var currentRow = Arrays.stream(currentRowLine.split(" "))
                                            .map(Integer::parseInt)
                                            .collect(Collectors.toList());
                if (currentRow.size() != numColumns)
                    throw new IllegalArgumentException("Matrix not consistent.");
                matrix.add(currentRow);
            }
        } catch (NumberFormatException e){
            System.out.println("Only numbers in file pls.");
            System.exit(1);
        } catch (IOException | IllegalArgumentException e){
            System.out.println(e.getMessage());
            System.exit(1);
        }

        return matrix;
    }

    private static final class TransposeRunnable implements Runnable{

        final List<List<Integer>> sourceMatrix;
        final List<List<Integer>> targetMatrix;
        final int myRow;
        final int myCol;
        public TransposeRunnable(final List<List<Integer>> sourceMatrix,
                                 final List<List<Integer>> targetMatrix,
                                 final int myRow,
                                 final int myCol) {
//            System.out.println("Source matrix is of size " + sourceMatrix.size() + "x" + sourceMatrix.get(0).size());
//            System.out.println("Target matrix is of size " + targetMatrix.size() + "x" + targetMatrix.get(0).size());
//            System.out.printf("My job is to take %d, %d from source and put it at %d, %d in target\r\n", );
//            System.out.println("=====================================");
            this.sourceMatrix = sourceMatrix;
            this.targetMatrix = targetMatrix;
            this.myRow = myRow;
            this.myCol = myCol;
        }

        @Override
        public void run() {
            targetMatrix.get(myCol).set(myRow, sourceMatrix.get(myRow).get(myCol));
        }
    }

    private static final class SyncInt{
        private int value;

        public SyncInt(int value) {
            this.value = value;
        }

        public synchronized void add (int d){
            value += d;
        }

        public synchronized int getValue() {
            return value;
        }
    }

    private static final class RankRunnable implements Runnable{
        final List<List<Integer>> matrix;
        final int i;
        final SyncInt result;

        public RankRunnable(final List<List<Integer>> matrix, final int i, final SyncInt result) {
            this.matrix = matrix;
            this.i = i;
            this.result = result;
        }

        @Override
        public void run() {
            synchronized (result) {
                result.add(matrix.get(i).get(i));
            }
        }
    }
    public static void main(String[] args) {
        try(Scanner sc = new Scanner(new FileInputStream(PATH_PREFIX + "in.txt"))){
            List<List<Integer>> matrix;
            while ((matrix = parseOneMatrix(sc)) != null){
                var numRows = matrix.size();
                var numCols = matrix.get(0).size();
                if (numRows == 1 && numCols == 1){
                    printMatrix(matrix);
                    continue;
                }

                var transpose = getTransposeEmpty(matrix);
                List<Thread> transposeThreads = new ArrayList<>();
                for (int i = 0; i < numRows; i++){
                    for (int j = 0; j < numCols; j++){
                        var t = new Thread(new TransposeRunnable(matrix, transpose, i, j));
                        t.start();
                        transposeThreads.add(t);
                    }
                }

                var rank = new SyncInt(0);
                List<Thread> rankThreads = new ArrayList<>();
                for (int i = 0; i < Math.min(numRows, numCols); i++){
                    var t = new Thread(new RankRunnable(matrix, i, rank));
                    t.start();
                    rankThreads.add(t);
                }
                for (var t: transposeThreads){
                    t.join();
                }
                for (var t: rankThreads){
                    t.join();
                }
                printMatrix(transpose);
                System.out.println("rank = " + rank.getValue());
                System.out.println();
            }
        }catch (FileNotFoundException e){
            System.out.println("No input file found.");
        }catch (InterruptedException e){
            System.out.println("Thread error: " + e.getMessage());
        }
    }

}
