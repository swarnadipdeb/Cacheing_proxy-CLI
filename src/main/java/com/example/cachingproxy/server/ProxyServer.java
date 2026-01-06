package com.example.cachingproxy.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.nio.file.Files;
import java.util.Map;

import com.example.cachingproxy.server.ProxyHandler;
import com.example.cachingproxy.util.RuntimeInfo;

public class ProxyServer {

    private HttpServer server;
    private final String origin;

    public ProxyServer(String origin) {
        this.origin = origin;
    }

    private void writeRuntimeInfo(int port) throws IOException {
    String json = """
        {
          "port": %d
        }
        """.formatted(port);
    Files.deleteIfExists(RuntimeInfo.INFO_FILE);
    Files.writeString(RuntimeInfo.INFO_FILE, json);
}

    /**
     * Start the HTTP server on the given port
     */
    public void start(int port) throws IOException {

        // 1️⃣ Create server bound to port
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // 2️⃣ Register handler for all paths
        server.createContext("/", new ProxyHandler(origin));
;

        // 3️⃣ Use a thread pool (important)
        server.setExecutor(Executors.newFixedThreadPool(10));

        // 4️⃣ Start server

        server.start();
        try {
        writeRuntimeInfo(port);
        }catch (IOException ignored) {}

        System.out.println("Proxy server started on port " + port);

        

        try {
            Thread.currentThread().join();
        } catch (InterruptedException ignored) {}
    }

    /**
     * Stop the server gracefully
     */
    public void stop() {
        if (server != null) {
            try {
            Files.deleteIfExists(RuntimeInfo.INFO_FILE);
            } catch (IOException ignored) {}
            server.stop(1);
            System.out.println("Proxy server stopped");
        }
    }

     
}
