package com.example.cachingproxy.util;

import java.nio.file.Path;

public class RuntimeInfo {

    public static final Path INFO_FILE =
            Path.of(System.getProperty("java.io.tmpdir"), "caching-proxy.info");

}
