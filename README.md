# Caching Proxy CLI

A simple command-line tool that starts an HTTP caching proxy server. The proxy forwards requests to an origin server and caches the responses. If the same request is made again, it returns the cached response instead of forwarding the request to the origin.

## Features

- Start a lightweight HTTP proxy on any port
- Forward requests to a configurable origin server (e.g. `http://dummyjson.com`)
- Cache responses from the origin so repeated requests are served faster
- Add headers indicating whether a response was served from cache or fetched from the origin:
  - `X-Cache: HIT` when the response is served from the cache
  - `X-Cache: MISS` when the response is fetched from the origin server and then cached
- CLI command to clear the in-memory cache

## Requirements

- Java 17 or higher
- Maven 3.8+ (for building from source)

## Building from Source

From the project root, run:

```bash path=null start=null
mvn clean package
```

This will produce a shaded JAR at:

- `target/caching-proxy.jar`

You can run the CLI using `java -jar` or by using the helper scripts in `caching-proxy-dist/bin`.

## Running via JAR directly

If you prefer not to use the helper scripts, you can run the shaded JAR directly after building:

```bash
java -jar target/caching-proxy.jar --port 3000 --origin http://dummyjson.com
```

And to clear the cache (if supported from the JAR entry point in your environment):

```bash
java -jar target/caching-proxy.jar --clear-cache
```

## Installation (optional)

### Unix-like systems (Linux, macOS)

```bash 
sudo chmod +x caching-proxy-dist/bin/caching-proxy
sudo ln -s "$(pwd)/caching-proxy-dist/bin/caching-proxy" /usr/local/bin/caching-proxy
```

### Windows (PowerShell)

copy the `caching-proxy-dist` folder to this path `C:\Program Files`

1. Press Win + R → sysdm.cpl

2. Advanced → Environment Variables

3. Under User variables → select Path

3. Click Edit

4. Add:
```bash
  C:\Program Files\caching-proxy-dist\bin
```
5. OK → OK


## Usage

The basic command to start the caching proxy is:

```bash path=null start=null
caching-proxy --port <number> --origin <url>
```

- `--port` – the port on which the caching proxy server will run (e.g. `3000`).
- `--origin` – the base URL of the origin server to which requests will be forwarded (e.g. `http://dummyjson.com`).

### Example

Start the proxy on port `3000` and forward to `http://dummyjson.com`:

```bash
caching-proxy --port 3000 --origin http://dummyjson.com
```

Now, if you make a request to:

```bash
curl -i http://localhost:3000/products
```

The proxy will:

1. Forward the request to `http://dummyjson.com/products`.
2. Return the response from the origin to the client.
3. Cache that response for subsequent identical requests.

### Cache headers

On the first request for a given resource, the response will include:

```text
X-Cache: MISS
```

On subsequent identical requests (same method and URL), the response will be served from cache and will include:

```text path=null start=null
X-Cache: HIT
```

## Clearing the Cache

You can clear the in-memory cache using the CLI:

```bash path=null start=null
caching-proxy --clear-cache
```

This command removes all cached entries. The next request to any resource will again be a cache miss and will be fetched from the origin.


## Development

- Build: `mvn clean package`
- Test: `mvn test`

You can extend the project by adding features such as configurable cache eviction policies (TTL, LRU), persistent cache storage, or logging of requests and cache statistics.

## License
- This project is licensed under the MIT License.

## Inspiration
- This project was inspired by the URL Shortening Service guide from [roadmap.sh](https://roadmap.sh/projects/caching-server).
