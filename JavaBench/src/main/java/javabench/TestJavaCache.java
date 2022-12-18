package javabench;

import org.openjdk.jmh.annotations.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

interface MemCache {
    int get(int key);

    void put(int key, int value);
}

class ConcurrentCache implements MemCache {

    final ConcurrentHashMap<Integer, Integer> m = new ConcurrentHashMap();

    @Override
    public int get(int key) {
        return m.get(key);
    }

    @Override
    public void put(int key, int value) {
        m.put(key, value);
    }
}

class LockCache implements MemCache {

    final ReadWriteLock rw = new ReentrantReadWriteLock(false);
    final HashMap<Integer, Integer> m = new HashMap();

    @Override
    public int get(int key) {
        rw.readLock().lock();
        try {
            return m.get(key);
        } finally {
            rw.readLock().unlock();
        }
    }

    @Override
    public void put(int key, int value) {
        rw.writeLock().lock();
        try {
            m.put(key, value);
        } finally {
            rw.writeLock().unlock();
        }
    }
}

/*
note, this would crash in a real "multi" environment, but only works here since
the map is pre-populated so it is never resized. There is no easy way in jmh to restrict
certain benchmarks to certain parameters
 */
class UnsharedCache implements MemCache {
    final Map<Integer, Integer> m = new HashMap();

    @Override
    public int get(int key) {
        return m.get(key);
    }

    @Override
    public void put(int key, int value) {
        m.put(key, value);
    }
}

class IntMapCache implements MemCache {
    static class node {
        int key, value;
        node next;
    }

    private final node[] table;
    private final int mask;

    private static int nextPowerOf2(int v) {
        v--;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        v++;
        return v;
    }

    public IntMapCache(int size) {
        size = nextPowerOf2(size);
        table = new node[size];
        mask = size - 1;
    }

    @Override
    public int get(int key) {
        node n = table[key & mask];
        if (n == null) {
            return 0;
        }
        for (; n != null; n = n.next) {
            if (n.key == key) {
                return n.value;
            }
        }
        return 0;
    }

    @Override
    public void put(int key, int value) {
        node head = table[key & mask];
        for (node n = head; n != null; n = n.next) {
            if (n.key == key) {
                n.value = value;
                return;
            }
        }
        node n = new node();
        n.key = key;
        n.value = value;
        n.next = head;
        table[key & mask] = n;
    }
}

@Fork(1)
@State(Scope.Benchmark)
@Warmup(iterations = 0)
@Measurement(iterations = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)

public class TestJavaCache {
    final int Mask = (1024 * 1024) - 1;
    final int NTHREADS = 2;

    static int rand(int r) {
        /* Algorithm "xor" from p. 4 of Marsaglia, "Xorshift RNGs" */
        r ^= r << 13;
        r ^= r >> 17;
        r ^= r << 5;
        return r & 0x7fffffff;
    }

    @Param({"unshared", "concurrent", "lock", "intmap", "intmap2"})
    public String arg;

    static MemCache memCache;

    static ExecutorService executorService;

    public int Sink;

    @Setup
    public void setup() {
        switch (arg) {
            case "unshared":
                memCache = new UnsharedCache();
                break;
            case "concurrent":
                memCache = new ConcurrentCache();
                break;
            case "lock":
                memCache = new LockCache();
                break;
            case "intmap":
                memCache = new IntMapCache(256000);
                break;
            case "intmap2":
                memCache = new IntMapCache(1000000);
                break;
        }

        executorService = Executors.newFixedThreadPool(NTHREADS);
        for (int i = 0; i <= Mask; i++) {
            memCache.put(i, i);
        }
    }

    @TearDown
    public void tearDown() {
        executorService.shutdown();
        for (int i = 0; i <= Mask; i++) {
            if ((memCache.get(i) & Mask) != (i & Mask)) {
                throw new IllegalStateException("index " + i + " = " + memCache.get(i));
            }
        }
        System.gc();
    }

    @Benchmark
    @OperationsPerInvocation(1000000)
    public void Get() {
        int sum = 0;
        int r = (int) System.nanoTime();
        for (int i = 0; i < 1000000; i++) {
            r = rand(r);
            sum += memCache.get(r & Mask);
        }
        Sink = sum;
    }

    @Benchmark
    @OperationsPerInvocation(1000000)
    public void Put() {
        int r = (int) System.nanoTime();
        for (int i = 0; i < 1000000; i++) {
            r = rand(r);
            memCache.put(r & Mask, r);
        }
    }

    @Benchmark
    @OperationsPerInvocation(1000000)
    public void PutGet() {
        int r = (int) System.nanoTime();
        int sum = 0;
        for (int i = 0; i < 1000000; i++) {
            r = rand(r);
            memCache.put(r & Mask, r);
            r = rand(r);
            sum += memCache.get(r & Mask);
        }
        Sink = sum;
    }

    @Benchmark
    @OperationsPerInvocation(1000000)
    public void MultiGet() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(NTHREADS);

        Runnable run = () -> {
            Get();
            latch.countDown();
        };
        for (int i = 0; i < NTHREADS; i++) {
            executorService.execute(run);
        }
        latch.await();
    }

    @Benchmark
    @OperationsPerInvocation(1000000)
    public void MultiPut() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(NTHREADS);

        Runnable run = () -> {
            Put();
            latch.countDown();
        };
        for (int i = 0; i < NTHREADS; i++) {
            executorService.execute(run);
        }
        latch.await();
    }

    @Benchmark
    @OperationsPerInvocation(1000000)
    public void MultiPutGet() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(NTHREADS);

        Runnable run = () -> {
            PutGet();
            latch.countDown();
        };
        for (int i = 0; i < NTHREADS; i++) {
            executorService.execute(run);
        }
        latch.await();
    }
}