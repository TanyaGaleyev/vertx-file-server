package org.ivan.fileserver;

import io.vertx.core.Vertx;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.Pump;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FileServer {

    public static void main(String[] args) throws IOException {
        Integer port = Integer.getInteger("port", 8888);
        System.out.printf("Starting server on port %d...%n", port);
        startServer(port);
        new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

    public static FileServer startServer(int port) throws IOException {
        return new FileServer(port);
    }

    private List<Long> requestDurations = new ArrayList<>();

    private FileServer(int port) throws IOException {
        prepareFile("file");
        Vertx vertx = Vertx.vertx();
        CompletableFuture<Void> serverStarted = new CompletableFuture<>();
        vertx.createHttpServer()
                .requestHandler(request -> {
                    if (request.uri().equals("/stats")) {
                        System.out.println(requestDurations.stream().mapToLong(Long::longValue).average());
                        System.out.println(requestDurations.stream().mapToLong(Long::longValue).max());
                        requestDurations = new ArrayList<>();
                        request.response().end();
                        return;
                    }
                    long t0 = System.currentTimeMillis();
                    vertx.fileSystem().open("file", new OpenOptions(), ar -> {
                        if (ar.succeeded()) {
                            request.response().setChunked(true);
                            Pump.pump(ar.result(), request.response()).start();
                            ar.result()
                                    .endHandler(v -> {
                                        requestDurations.add(System.currentTimeMillis() - t0);
                                        request.response().end();
                                    })
                                    .exceptionHandler(e -> request.response().close());
                        } else {
                            request.response().setStatusCode(500).end();
                        }
                    });
                })
                .listen(port, ar -> {
                    if (ar.succeeded()) {
                        requestDurations = new ArrayList<>();
                        serverStarted.complete(null);
                    } else {
                        serverStarted.completeExceptionally(ar.cause());
                    }
                });
        serverStarted.join();
    }

    public List<Long> getRequestDurations() {
        return requestDurations;
    }

    private static void prepareFile(String filename) throws IOException {
        byte[] kb = new byte[1024];
        try (OutputStream out = Files.newOutputStream(Paths.get(filename))) {
            for (int i = 0; i < 100 * 1024; i++) {
                out.write(kb);
            }
        }
    }
}
