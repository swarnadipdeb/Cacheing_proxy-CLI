package com.example.cachingproxy.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.example.cachingproxy.cache.CacheEntry;
import com.example.cachingproxy.cache.CacheKeyUtil;
import com.example.cachingproxy.cache.CacheManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class ProxyHandler implements HttpHandler {

    private final String origin;
    private final HttpClient httpClient;

    public ProxyHandler(String origin) {
        this.origin = origin.endsWith("/")
                ? origin.substring(0, origin.length() - 1)
                : origin;

        // Force HTTP/1.1 and add timeout
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)  // ← Fixes RST_STREAM error
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private Map<String, List<String>> filterResponseHeaders(
        Map<String, List<String>> headers) {

    Map<String, List<String>> filtered = new java.util.HashMap<>();

    headers.forEach((key, values) -> {
        if (!key.equalsIgnoreCase("Transfer-Encoding")
                && !key.equalsIgnoreCase("Content-Length")
                && !key.equalsIgnoreCase("Connection")) {
            filtered.put(key, values);
        }
    });

    return filtered;
}

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod();

        if (method.equals("POST")
        && exchange.getRequestURI().getPath().equals("/__admin/clear-cache")) {

         CacheManager.clear();

        byte[] resp = "Cache cleared".getBytes();
         exchange.sendResponseHeaders(200, resp.length);
         exchange.getResponseBody().write(resp);
         exchange.close();
         return;
            }
       
        //  Cache only GET requests
        if ("GET".equalsIgnoreCase(method)) { 
            String cacheKey = CacheKeyUtil.generate(exchange);
            CacheEntry cached = CacheManager.get(cacheKey);

            if (cached != null) {
                //  CACHE HIT
                cached.getHeaders()
                        .forEach((k, v) -> exchange.getResponseHeaders().put(k, v));
                exchange.getResponseHeaders().add("X-Cache", "HIT");

                exchange.sendResponseHeaders(
                        cached.getStatusCode(),
                        cached.getBody().length
                );
                exchange.getResponseBody().write(cached.getBody());
                exchange.close();
                return;
            }
        }

        try {
            String pathWithQuery = exchange.getRequestURI().toString();
            String targetUrl = origin + pathWithQuery;

            URI targetUri = URI.create(targetUrl);

            System.out.println("→ Proxying: " + method + " " + targetUri);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(targetUri)
                    .timeout(Duration.ofSeconds(30));

            // Handle request body
            if (method.equalsIgnoreCase("GET")
                    || method.equalsIgnoreCase("DELETE")
                    || method.equalsIgnoreCase("HEAD")) {

                requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());

            } else {
                byte[] requestBody = exchange.getRequestBody().readAllBytes();
                requestBuilder.method(
                        method,
                        HttpRequest.BodyPublishers.ofByteArray(requestBody)
                );
            }

            // Copy request headers (skip restricted ones)
            for (Map.Entry<String, List<String>> header :
                    exchange.getRequestHeaders().entrySet()) {

                String name = header.getKey();
            
                if (name.equalsIgnoreCase("Host")
                        || name.equalsIgnoreCase("Content-Length")
                        || name.equalsIgnoreCase("Transfer-Encoding")
                        || name.equalsIgnoreCase("Connection")
                        ) {
                    continue;
                }

                 for (String value : header.getValue()) {
                      requestBuilder.header(name, value);
                 }
                
            }
             

            // Send request to origin
            HttpResponse<byte[]> originResponse =
                    httpClient.send(
                            requestBuilder.build(),
                            HttpResponse.BodyHandlers.ofByteArray()
                    );


            // Copy response headers
            originResponse.headers().map()
                    .forEach((key, values) -> {
                        // Skip problematic headers
                        if (!key.equalsIgnoreCase("Transfer-Encoding")
                                && !key.equalsIgnoreCase("Content-Length")
                                && !key.equalsIgnoreCase("Connection")) {
                            exchange.getResponseHeaders().put(key, values);
                        }
                    });

            // Store in cache (GET only)
            if ("GET".equalsIgnoreCase(method)) {
                CacheEntry entry = new CacheEntry(
                        originResponse.statusCode(),
                        filterResponseHeaders(originResponse.headers().map()),
                        originResponse.body()
                );
                CacheManager.put(
                        CacheKeyUtil.generate(exchange),
                        entry
                );
            }
            
             exchange.getResponseHeaders().add("X-Cache", "MISS");

            //  Send response back to client
            byte[] responseBody = originResponse.body();
            exchange.sendResponseHeaders(
                    originResponse.statusCode(),
                    responseBody.length
            );

            exchange.getResponseBody().write(responseBody);
            exchange.close();

        } catch (Exception e) {
            System.err.println("❌ ERROR proxying request:");
            System.err.println("   Method: " + exchange.getRequestMethod());
            System.err.println("   Path: " + exchange.getRequestURI());
            System.err.println("   Exception: " + e.getClass().getName());
            System.err.println("   Message: " + e.getMessage());
            e.printStackTrace();

            try {
                String msg = "502 Bad Gateway: " + e.getMessage();
                exchange.sendResponseHeaders(502, msg.length());
                exchange.getResponseBody().write(msg.getBytes());
            } catch (IOException ioException) {
                System.err.println("Failed to send error response: " + ioException.getMessage());
            } finally {
                exchange.close();
            }
        }
    }
}