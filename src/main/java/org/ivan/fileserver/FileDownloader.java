package org.ivan.fileserver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FileDownloader {

    public static void main(String[] args) throws IOException {
        String host = System.getProperty("host", "localhost");
        int port = Integer.getInteger("port", 8888);
        int requests = Integer.getInteger("requests", 100);
        URL fileUrl = new URL("http", host, port, "/");
        URL statsUrl = new URL("http", host, port, "/stats");
        statsUrl.openConnection().getInputStream().close();
        System.out.printf("Sending %d requests for %s...%n", requests, fileUrl);
        ExecutorService executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
        byte[] buffer = new byte[1024];
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            futures.add(executor.submit(() -> {
                try (InputStream in = fileUrl.openStream()) {
                    while (in.read(buffer) != -1) ;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
        }
        futures.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        executor.shutdown();
        statsUrl.openConnection().getInputStream().close();
    }
}
