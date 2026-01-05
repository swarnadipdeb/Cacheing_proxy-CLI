package com.example.cachingproxy.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.example.cachingproxy.server.ProxyHandler;

public class ProxyServer {

    private HttpServer server;
    private final String origin;

    public ProxyServer(String origin) {
        this.origin = origin;
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
        System.out.println("Proxy server started on port " + port);
    }

    /**
     * Stop the server gracefully
     */
    public void stop() {
        if (server != null) {
            server.stop(1);
            System.out.println("Proxy server stopped");
        }
    }
}
