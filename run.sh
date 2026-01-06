#!/bin/bash

# Build the project
echo "Building the project..."
mvn clean package

# Check if build was successful
if [ $? -ne 0 ]; then
    echo "Build failed! Exiting..."
    exit 1
fi

# Run the application
echo "Starting the caching proxy server..."
java -jar target/caching-proxy-1.0-SNAPSHOT.jar \
    --port 3000 \
    --origin https://url-shortner-prod.up.railway.app/
