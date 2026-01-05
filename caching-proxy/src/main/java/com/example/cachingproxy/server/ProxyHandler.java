package com.example.cachingproxy.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

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

        // ✅ Force HTTP/1.1 and add timeout
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)  // ← Fixes RST_STREAM error
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        try {
            String method = exchange.getRequestMethod();
            String pathWithQuery = exchange.getRequestURI().toString();
            String targetUrl = origin + pathWithQuery;

            URI targetUri = URI.create(targetUrl);

            System.out.println("→ Proxying: " + method + " " + targetUri);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(targetUri)
                    .timeout(Duration.ofSeconds(30));

            // ✅ Handle request body
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

            // ✅ Copy request headers (skip restricted ones)
            for (Map.Entry<String, List<String>> header :
                    exchange.getRequestHeaders().entrySet()) {

                String name = header.getKey();

                // Skip headers that HttpClient sets automatically
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

            // ✅ Set required headers
            requestBuilder.header("User-Agent", "CachingProxy/1.0");
            requestBuilder.header("Accept", "*/*");

            // ✅ Send request to origin
            HttpResponse<byte[]> originResponse =
                    httpClient.send(
                            requestBuilder.build(),
                            HttpResponse.BodyHandlers.ofByteArray()
                    );

            System.out.println("← Origin responded: " + originResponse.statusCode());

            // ✅ Copy response headers
            originResponse.headers().map()
                    .forEach((key, values) -> {
                        // Skip problematic headers
                        if (!key.equalsIgnoreCase("Transfer-Encoding")
                                && !key.equalsIgnoreCase("Content-Length")
                                && !key.equalsIgnoreCase("Connection")) {
                            exchange.getResponseHeaders().put(key, values);
                        }
                    });

            // ✅ Send response back to client
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