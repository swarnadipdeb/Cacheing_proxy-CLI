package com.example.cachingproxy;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import com.example.cachingproxy.server.ProxyServer;
import com.example.cachingproxy.cache.CacheManager;

@Command(
        name = "caching-proxy",
        mixinStandardHelpOptions = true,
        description = "CLI tool to start a caching proxy server"
)
public class CachingProxyCLI implements Runnable {

    @Option(names = "--port", description = "Proxy port")
    Integer port;

    @Option(names = "--origin", description = "Origin server URL")
    String origin;

    @Option(names = "--clear-cache", description = "Clear cache and exit")
    boolean clearCache;

    @Override
    public void run() {

        if (clearCache) {
            System.out.println("Cleaning Up...");
            CacheManager.clear();
            return;
        }

        if (port == null || origin == null) {
            System.err.println("Error: --port and --origin are required");
            return;
        }

        System.out.println("Starting proxy...");
        System.out.println("Port   : " + port);
        System.out.println("Origin : " + origin);

         

    try {
        ProxyServer server = new ProxyServer(origin);
        server.start(port);
        
    } catch (Exception e) {
        System.err.println("Failed to start server: " + e.getMessage());
    }

    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CachingProxyCLI()).execute(args);
        System.exit(exitCode);
    }
}
