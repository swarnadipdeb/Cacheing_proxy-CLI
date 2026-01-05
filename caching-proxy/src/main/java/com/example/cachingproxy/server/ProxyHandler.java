package com.example.cachingproxy.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class ProxyHandler implements HttpHandler {

    private final String origin;
    private final HttpClient httpClient;

    public ProxyHandler(String origin) {
        this.origin = origin;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        try {
            // 1️⃣ Build target URL
            String targetUrl = origin + exchange.getRequestURI().toString();
            URI uri = URI.create(targetUrl);

            System.out.println(
                    exchange.getRequestMethod() + " " + exchange.getRequestURI()
            );

            // 2️⃣ Build forwarded request
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .method(
                            exchange.getRequestMethod(),
                            getBodyPublisher(exchange)
                    );

            // 3️⃣ Copy request headers
            for (Map.Entry<String, List<String>> header :
            exchange.getRequestHeaders().entrySet()) {

            String name = header.getKey();

            // ❌ Skip restricted headers
            if (name.equalsIgnoreCase("Host")
            || name.equalsIgnoreCase("Content-Length")
            || name.equalsIgnoreCase("Transfer-Encoding")
            || name.equalsIgnoreCase("Connection")) {
                continue;
            }

            for (String value : header.getValue()) {
                requestBuilder.header(name, value);
            }
                }


            HttpRequest forwardedRequest = requestBuilder.build();

            // 4️⃣ Send request to origin
            HttpResponse<byte[]> originResponse =
                    httpClient.send(
                            forwardedRequest,
                            HttpResponse.BodyHandlers.ofByteArray()
                    );

            // 5️⃣ Copy response headers
            for (Map.Entry<String, List<String>> header :
                    originResponse.headers().map().entrySet()) {

                exchange.getResponseHeaders().put(
                        header.getKey(),
                        header.getValue()
                );
            }

            // 6️⃣ Send response back to client
            exchange.sendResponseHeaders(
                    originResponse.statusCode(),
                    originResponse.body().length
            );

            OutputStream os = exchange.getResponseBody();
            os.write(originResponse.body());
            os.close();

        } catch (Exception e) {
            String error = "Proxy error: " + e.getMessage();
            exchange.sendResponseHeaders(500, error.length());
            exchange.getResponseBody().write(error.getBytes());
            exchange.close();
        }
    }

    /**
     * Handles request body safely
     */
    private static HttpRequest.BodyPublisher getBodyPublisher(HttpExchange exchange)
            throws IOException {

        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())
                || "DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            return HttpRequest.BodyPublishers.noBody();
        }

        InputStream is = exchange.getRequestBody();
        byte[] body = is.readAllBytes();
        return HttpRequest.BodyPublishers.ofByteArray(body);
    }
}
