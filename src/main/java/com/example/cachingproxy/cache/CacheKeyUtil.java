package com.example.cachingproxy.cache;

import com.sun.net.httpserver.HttpExchange;

public class CacheKeyUtil {

    public static String generate(HttpExchange exchange) {
        return exchange.getRequestMethod() + ":" +
               exchange.getRequestURI().toString();
    }
}
