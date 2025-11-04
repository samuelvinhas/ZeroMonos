#!/bin/bash

# ZeroMonos - Start Script
# This script starts both the database and backend application

echo "========================================"
echo "  ZeroMonos Garbage Collection System  "
echo "========================================"
echo ""

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    echo "Error: Please run this script from the backend/HW1 directory"
    exit 1
fi

# Start PostgreSQL containers
echo "Starting PostgreSQL databases..."
docker-compose up -d

if [ $? -ne 0 ]; then
    echo "Error: Failed to start Docker containers"
    exit 1
fi

echo "Waiting for databases to be ready..."
sleep 5

# Check database health
echo "Checking database health..."
docker-compose ps

echo ""
echo "Starting Spring Boot application..."
./mvnw spring-boot:run

# Note: The application will run in the foreground
# Press Ctrl+C to stop
