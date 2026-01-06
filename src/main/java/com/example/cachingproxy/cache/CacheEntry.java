package com.example.cachingproxy.cache;

import java.util.List;
import java.util.Map;

public class CacheEntry {

    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final byte[] body;
    private final long createdAt;

    public CacheEntry(int statusCode,
                      Map<String, List<String>> headers,
                      byte[] body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
        this.createdAt = System.currentTimeMillis();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
