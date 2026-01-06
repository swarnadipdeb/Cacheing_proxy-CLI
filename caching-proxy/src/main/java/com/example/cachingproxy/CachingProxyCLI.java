package com.example.cachingproxy;

import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import com.example.cachingproxy.server.ProxyServer;
import com.example.cachingproxy.cache.CacheManager;
import com.example.cachingproxy.util.RuntimeInfo;

@Command(
        name = "caching-proxy",
        mixinStandardHelpOptions = true,
        description = "CLI tool to start a caching proxy server"
)
public class CachingProxyCLI implements Runnable {

    @Option(names = "--port",required = true, description = "Proxy port")
    Integer port;

    @Option(names = "--origin",required = true, description = "Origin server URL")
    String origin;

    @Option(names = "--clear-cache", description = "Clear cache and exit")
    boolean clearCache;

    private int readRunningPort() throws Exception {
             if (!Files.exists(RuntimeInfo.INFO_FILE)) {
        throw new IllegalStateException("No running caching-proxy found");
            }

         String content = Files.readString(RuntimeInfo.INFO_FILE);

            Pattern p = Pattern.compile("\"port\"\\s*:\\s*(\\d+)");
            Matcher m = p.matcher(content);

            if (!m.find()) {
                 throw new IllegalStateException("Invalid runtime info file");
         }

              return Integer.parseInt(m.group(1));
            }

    @Override
    public void run() {

        if (clearCache) {
            try{
             int port = readRunningPort();

                URI uri = URI.create(
                "http://localhost:" + port + "/__admin/clear-cache"
            );

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder(uri)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

                client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("Cache cleared successfully");
                return;
            }catch(Exception e){
                System.out.println("Unable to clear the cache");
            }
                
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
