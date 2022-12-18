package javabench;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class Matrix {
    public List<ArrayList<ArrayList<Integer>>> genMatrix() {
        int maxSize = 4096;

        ArrayList<ArrayList<Integer>> A = new ArrayList<>(maxSize);
        ArrayList<ArrayList<Integer>> B = new ArrayList<>(maxSize);

        for (int m = 0; m < maxSize; m++) {
            ArrayList<Integer> data = new ArrayList<>(maxSize);
            for (int n = 0; n < maxSize; n++) {
                data.add((int) (Math.random() * 40) + 1);
            }
            A.add(data);
        }

        for (int m = 0; m < maxSize; m++) {
            ArrayList<Integer> data = new ArrayList<>(maxSize);
            for (int n = 0; n < maxSize; n++) {
                data.add((int) (Math.random() * 40) + 1);
            }
            B.add(data);
        }

        List<ArrayList<ArrayList<Integer>>> res = new LinkedList<>();
        res.add(A);
        res.add(B);
        return res;
    }

    public int[][] matrixMultiplication(ArrayList<ArrayList<Integer>> A, ArrayList<ArrayList<Integer>> B, int m, int n) {
        int[][] C = new int[m][n];
        for (int i = 0; i < m; i++) {
            for (int k = 0; k < n; k++) {
                int temp = A.get(i).get(k);
                for (int j = 0; j < n; j++) {
                    C[i][j] += temp * B.get(k).get(j);
                }
            }
        }
        return C;
    }

    public ArrayList<ArrayList<ArrayList<Integer>>> splitMatrix(ArrayList<ArrayList<Integer>> A, int nrOfThreads) {
        int n = A.size();
        int m = n / nrOfThreads;
        ArrayList<ArrayList<ArrayList<Integer>>> B = new ArrayList<>();
        for (int i = 0; i < nrOfThreads; i++) {
            B.add(new ArrayList<>(A.subList(i * m, (i + 1) * m)));
        }
        return B;
    }
}

class Worker implements Runnable {
    private final ArrayList<ArrayList<Integer>> A;
    private final ArrayList<ArrayList<Integer>> B;
    private final int index;
    private final ArrayList<int[][]> result;
    private final int m;
    private final int n;
    private final Matrix mat;
    private final CountDownLatch latch;

    public Worker(Matrix mat, CountDownLatch latch, ArrayList<ArrayList<Integer>> A,
                  ArrayList<ArrayList<Integer>> B, int index,
                  ArrayList<int[][]> result) {
        this.A = A;
        this.B = B;
        this.index = index;
        this.result = result;
        this.m = A.size();
        this.n = B.size();
        this.mat = mat;
        this.latch = latch;
    }

    @Override
    public void run() {
        this.result.set(this.index, mat.matrixMultiplication(this.A, this.B,
                this.m, this.n));
        latch.countDown();
    }
}

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 0)
@Measurement(iterations = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)

public class TestJavaMatrix {
    @Param({"1-Thread", "2-Thread", "4-Thread", "8-Thread", "16-Thread"})
    public String arg;

    static Matrix matrix;

    static ExecutorService executorService;

    static int NTHREADS;

    @Setup
    public void setup() {
        switch (arg) {
            case "1-Thread":
                NTHREADS = 1;
                break;
            case "2-Thread":
                NTHREADS = 2;
                break;
            case "4-Thread":
                NTHREADS = 4;
                break;
            case "8-Thread":
                NTHREADS = 8;
                break;
            case "16-Thread":
                NTHREADS = 16;
                break;
        }

        matrix = new Matrix();
        executorService = Executors.newFixedThreadPool(NTHREADS);
    }

    @TearDown
    public void tearDown() {
        executorService.shutdown();
        System.gc();
    }

    @Benchmark
    public void Multiply() throws InterruptedException {
        List<ArrayList<ArrayList<Integer>>> matrices = matrix.genMatrix();
        ArrayList<ArrayList<Integer>> A = matrices.get(0);
        ArrayList<ArrayList<Integer>> B = matrices.get(1);

        if (NTHREADS == 1) {
            int n = A.size();
            CountDownLatch latch = new CountDownLatch(NTHREADS);
            executorService.execute(() -> {
                matrix.matrixMultiplication(A, B, n, n);
                latch.countDown();
            });
            latch.await();
        } else {
            ArrayList<int[][]> result = new ArrayList<>();
            int[][] empty = new int[][]{{}};
            for (int i = 0; i < NTHREADS; i++) {
                result.add(empty);
            }

            CountDownLatch latch = new CountDownLatch(NTHREADS);
            ArrayList<ArrayList<ArrayList<Integer>>> workerMatrices = matrix.splitMatrix(A, NTHREADS);

            for (int i = 0; i < NTHREADS; i++) {
                executorService.execute(new Worker(matrix, latch, workerMatrices.get(i), B, i, result));
            }

            latch.await();
        }
    }
}
