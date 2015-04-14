package org.cbbenchmark;

import com.couchbase.client.CouchbaseClient;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Benchmark {

    /**
     * @param args -> num of threads, num of keys, sleep time, sleep time for "get", hostname
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        final int numThreads = Integer.valueOf(args[0]);
        final int numKeys = Integer.valueOf(args[1]);
        final int sleepTime = Integer.valueOf(args[2]);
        final int sleepTimeReader = Integer.valueOf(args[3]);
        final String hostname = args[4];

        final ArrayList<URI> nodes = new ArrayList<URI>();
        nodes.add(URI.create("http://" + hostname + ":8091/pools"));

        CouchbaseClient client = null;
        try {
            client = new CouchbaseClient(nodes, "default", "");
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        final List<Future> futures = new ArrayList<Future>(numKeys);

        for (int i = 0; i < numKeys; i++) {
            final Callable<Future> worker = new Worker(i, client, true, sleepTime);
            
            futures.add(executor.submit(worker));
        }

        try {
            boolean notCompleted;
            do {
                notCompleted = false;
                for (final Future future : futures) {
                    if (!((Future) future.get()).isDone()) {
                        notCompleted = true;
                        break;
                    }
                }
            } while (notCompleted);

            System.out.println("Data populated");

            client.shutdown();
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            System.out.println("Ow ow");
        }
    }
}