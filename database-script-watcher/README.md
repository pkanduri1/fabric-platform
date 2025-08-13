# Database Script Watcher

A Spring Boot service that provides automated file processing through configurable file system monitoring with pluggable file processors.

## Project Structure

```
database-script-watcher/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/fabric/watcher/
│   │   │       ├── DatabaseScriptWatcherApplication.java
│   │   │       ├── config/
│   │   │       │   └── WatchConfig.java
│   │   │       ├── controller/
│   │   │       │   └── FileWatcherController.java
│   │   │       ├── model/
│   │   │       │   ├── ExecutionStatus.java
│   │   │       │   ├── ProcessingResult.java
│   │   │       │   └── ProcessingStatistics.java
│   │   │       ├── processor/
│   │   │       │   └── FileProcessor.java
│   │   │       └── service/
│   │   │           ├── FileProcessorRegistry.java
│   │   │           └── FileWatcherService.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/
│           └── com/fabric/watcher/
│               ├── DatabaseScriptWatcherApplicationTest.java
│               ├── config/
│               │   └── WatchConfigTest.java
│               └── model/
│                   └── ProcessingResultTest.java
├── pom.xml
└── README.md
```

## Core Interfaces

### FileProcessor
The main interface for processing different types of files. Implementations handle specific file types (SQL scripts, logs, etc.).

### WatchConfig
Configuration class that defines what folder to watch, how to process files, and where to move them after processing.

### ProcessingResult
Result object that contains execution details, status, and any metadata from file processing.

## Dependencies

- Spring Boot Starter Web
- Spring Boot Starter JDBC
- Spring Boot Starter Actuator
- H2 Database (for testing)
- Spring Boot Test

## Getting Started

1. Build the project:
   ```bash
   mvn clean compile
   ```

2. Run tests:
   ```bash
   mvn test
   ```

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

The application will start on port 8080 by default.

## Next Steps

This is the foundation setup. Future tasks will implement:
- Configuration management
- File watcher service
- SQL script processor
- File management utilities
- REST API endpoints
- Comprehensive logging and monitoring