# TokenHa - Simple Token Management Library

A lightweight Java library for managing tokens with automatic eviction, cooldown periods, file persistence with locking mechanisms, and adaptive logging support.

## Features

- **Token Queue Management**: FIFO queue with configurable size limits
- **Automatic Eviction**: Background thread automatically removes expired tokens
- **Cooldown Protection**: Configurable cooldown period between token additions
- **File Persistence**: Automatic saving/loading with exclusive file locking
- **Thread-Safe**: All operations are synchronized for concurrent access
- **Memory Efficient**: Uses WeakReference for automatic cleanup
- **Adaptive Logging**: Automatically adapts to your application's logging framework via SLF4J
- **Zero-Allocation Reads**: Optimized `getDescList()` with cached unmodifiable views

## Quick Start

```java
// Create TokenHa with default configuration
TokenHa tokenHa = new TokenHa();

// Or create with custom configuration
TokenHaConfig config = new TokenHaConfig.Builder()
    .maxTokens(20)
    .expirationTimeMillis(30000)
    .coolTimeToAddMillis(2000)
    .persistenceFilePath("my-tokens.json")
    .build();

TokenHa tokenHa = new TokenHa(config);

// Add tokens (respects size limits and cooldown)
boolean result1 = tokenHa.addIfAvailable("user123");
System.out.println("First token added: " + result1); // true

// Wait for cooldown period to pass
try {
    Thread.sleep(1100); // Wait longer than default cooldown (1000ms)
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}

boolean result2 = tokenHa.addIfAvailable("user456");
System.out.println("Second token added: " + result2); // true

// Check queue status
System.out.println("Queue size: " + tokenHa.getQueueSize());
System.out.println("Can add more: " + tokenHa.availableToAdd());

// Get newest token
TokenElement newest = tokenHa.newestToken();

// Get all tokens in descending order (newest to oldest)
List<TokenElement> tokens = tokenHa.getDescList(); // Zero allocation, returns unmodifiable view

// Export to JSON
String json = tokenHa.toJson();

// Load from file
tokenHa.loadFromFile();

// Always close when done
tokenHa.close();
```

## Installation

### Maven
```xml
<dependency>
    <groupId>com.github.tsutomunakamura</groupId>
    <artifactId>token-ha</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle
```gradle
implementation 'com.github.tsutomunakamura:token-ha:1.0.0'
```

## Architecture

### Core Classes

- **TokenHa**: Main class for token management
- **TokenElement**: Individual token with timestamp
- **TokenHaConfig**: Configuration using builder pattern
- **EvictionThread**: Singleton background thread for automatic cleanup
- **FilePersistence**: Handles file I/O with exclusive locking
- **TokenHaLogger**: Adaptive logging utility using SLF4J

### Key Features

#### Automatic Eviction
The singleton `EvictionThread` automatically removes expired tokens from all `TokenHa` instances:
- **Initial Delay**: How long to wait before first eviction run (default: 1000ms)
- **Interval**: How often to run eviction checks (default: 10000ms)
- Configurable minimum tokens to preserve regardless of expiration

#### Cooldown Management
Prevents rapid token addition with a configurable cooldown period (default 1000ms).

#### File Persistence with Locking
- Automatic saving on every token addition
- Exclusive file locking prevents data corruption
- Uses `RandomAccessFile` with `FileLock` for process-level locking
- JSON serialization using Gson

#### Adaptive Logging
- Uses SLF4J facade for flexible logging
- Automatically adapts to your application's logging framework (Logback, Log4j2, JUL, etc.)
- No configuration required - respects your existing logging setup
- Fallback to simple console logging if no framework is configured

#### Performance Optimizations
- **Cached Unmodifiable Lists**: `getDescList()` returns pre-computed views with zero allocation
- **Atomic Snapshot Updates**: Thread-safe snapshot replacement prevents inconsistent states
- **Lazy Evaluation**: Logging arguments only evaluated when log level is enabled

## Configuration

### Builder Pattern Configuration

Use the `TokenHaConfig.Builder` for comprehensive configuration:

```java
TokenHaConfig config = new TokenHaConfig.Builder()
    .maxTokens(20)
    .expirationTimeMillis(30000)        // 30 seconds
    .coolTimeToAddMillis(2000)          // 2 seconds cooldown
    .numberOfLastTokens(2)               // Keep at least 2 tokens
    .persistenceFilePath("custom-tokens.json")
    .build();

TokenHa tokenHa = new TokenHa(config);
```

### Eviction Thread Configuration

Configure the background eviction thread timing:

```java
EvictionThreadConfig evictionConfig = new EvictionThreadConfig.Builder()
    .initialDelayMillis(500)     // Start after 500ms
    .intervalMillis(5000)        // Run every 5 seconds
    .build();

TokenHaConfig config = new TokenHaConfig.Builder()
    .evictionThreadConfig(evictionConfig)
    .build();
```

### Properties File Configuration

Create a `tokenha.properties` file:

```properties
tokenha.max.tokens=15
tokenha.expiration.time.millis=45000
tokenha.cool.time.to.add.millis=1500
tokenha.number.of.last.tokens=2
tokenha.persistence.file.path=app-tokens.json
tokenha.eviction.initial.delay.millis=1000
tokenha.eviction.interval.millis=8000
```

Load configuration from properties:
```java
TokenHaConfig config = TokenHaConfig.fromProperties("tokenha.properties");
```

### Environment Variables Configuration

Set environment variables with `TOKENHA_` prefix:

```bash
export TOKENHA_MAX_TOKENS=25
export TOKENHA_EXPIRATION_TIME_MILLIS=60000
export TOKENHA_COOL_TIME_TO_ADD_MILLIS=1500
export TOKENHA_EVICTION_INITIAL_DELAY_MILLIS=2000
export TOKENHA_EVICTION_INTERVAL_MILLIS=15000
```

Load from environment:
```java
TokenHaConfig config = TokenHaConfig.fromEnvironment();
```

### Default Settings
- **Max Tokens**: 10
- **Expiration Time**: 60000ms (60 seconds)
- **Cool Time to Add**: 1000ms (1 second)
- **Number of Last Tokens**: 1 (minimum to keep)
- **Persistence File Path**: "tokenha-data.json"
- **Eviction Initial Delay**: 1000ms (1 second)
- **Eviction Interval**: 10000ms (10 seconds)

## API Reference

### Main Methods

```java
// Token Management
boolean addIfAvailable(String token)       // Add token if cooldown passed and not full
TokenElement newestToken()                  // Get the most recent token
List<TokenElement> getDescList()           // Get unmodifiable list (newest to oldest)
int getQueueSize()                         // Get current number of tokens
boolean availableToAdd()                   // Check if can add new token
boolean isFilled()                         // Check if queue is at max capacity
boolean passedCoolTimeToAdd()             // Check if cooldown period has passed

// Persistence
String toJson()                           // Export tokens as JSON
void loadFromFile()                       // Load tokens from persistence file
String getPersistenceFilePath()          // Get current persistence file path
boolean persistenceFileExists()          // Check if persistence file exists
boolean deletePersistenceFile()          // Delete persistence file

// Lifecycle
void close()                              // Clean up resources and unregister from eviction
```

## Logging Configuration

TokenHa uses SLF4J for adaptive logging. Configure logging levels in your application's logging configuration:

### Logback Example
```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- Control TokenHa logging levels -->
    <logger name="com.github.tsutomunakamura.tokenha" level="WARN"/>
    <logger name="com.github.tsutomunakamura.tokenha.persistence" level="DEBUG"/>
    
    <root level="info">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

### Log4j2 Example
```xml
<Configuration>
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        </Console>
    </Appenders>
    <Loggers>
        <Logger name="com.github.tsutomunakamura.tokenha" level="warn"/>
        <Root level="info">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

## Thread Safety

All public methods are synchronized, making the library safe for concurrent access across multiple threads. The library uses:
- Synchronized methods for all public APIs
- Atomic snapshot updates to prevent race conditions
- Thread-safe singleton pattern with double-checked locking
- WeakReferences for automatic cleanup of unused instances

## Performance Characteristics

- **Token Addition**: O(1) amortized
- **Token Retrieval**: O(1) for newest, O(n) for full list
- **List Access (`getDescList()`)**: O(1) with zero allocation
- **Eviction**: O(n) where n is number of expired tokens
- **JSON Serialization**: O(n) where n is queue size

## Running the Demo

```bash
mvn compile exec:java -Dexec.mainClass="com.github.tsutomunakamura.tokenha.Demo"
```

This will demonstrate:
- Token addition with cooldown enforcement
- Queue size management
- JSON serialization with Gson
- File persistence with locking
- Automatic eviction thread lifecycle
- Adaptive logging in action

## Testing

Run all tests:
```bash
mvn test
```

Run specific test suites:
```bash
# Run long-running demo test
mvn test -Dtest=LongRunDemo#longRunDemo -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition

# Run demo tests
mvn test -Dtest=DemoTest -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition
```

The test suite includes:
- **110+ unit tests** covering all functionality
- Thread safety and concurrency tests
- Singleton pattern and double-checked locking tests
- File persistence with locking scenarios
- Edge cases and error conditions
- Long-running stability tests

## Building the Project

This project includes the Maven wrapper, so you don't need to install Maven to build it.

### Prerequisites
- Java 17 or higher
- (Optional) GPG for signing artifacts when releasing to Maven Central

### Build Commands

```bash
# Compile the project
./mvnw compile

# Run tests
./mvnw test

# Package into JAR
./mvnw package

# Install to local Maven repository
./mvnw install

# Clean build artifacts
./mvnw clean

# Full clean build and test
./mvnw clean compile test

# Generate Javadoc
./mvnw javadoc:javadoc

# Deploy to Maven Central (requires configuration)
./mvnw clean deploy -P release
```

### Windows Users
Use `mvnw.cmd` instead of `./mvnw`:
```cmd
mvnw.cmd compile
mvnw.cmd test
mvnw.cmd package
```

## Project Structure

```
token-ha/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/github/tsutomunakamura/tokenha/
│   │           ├── TokenHa.java                 # Main API class
│   │           ├── config/                      # Configuration classes
│   │           │   ├── TokenHaConfig.java
│   │           │   └── EvictionThreadConfig.java
│   │           ├── data/                        # Data models
│   │           │   └── TokenData.java
│   │           ├── element/                     # Token element
│   │           │   └── TokenElement.java
│   │           ├── eviction/                    # Eviction thread
│   │           │   └── EvictionThread.java
│   │           ├── logging/                     # Logging utilities
│   │           │   └── TokenHaLogger.java
│   │           └── persistence/                 # File persistence
│   │               └── FilePersistence.java
│   └── test/
│       └── java/                                # Test classes
├── .mvn/wrapper/                                # Maven wrapper files
├── mvnw                                         # Maven wrapper script (Unix/Linux/macOS)
├── mvnw.cmd                                     # Maven wrapper script (Windows)
├── pom.xml                                      # Maven project file
├── README.md                                    # This file
├── ADAPTIVE_LOGGING_SUMMARY.md                 # Logging implementation details
└── .gitignore                                   # Git ignore rules
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues, questions, or suggestions, please use the [GitHub Issues](https://github.com/tsutomunakamura/token-ha/issues) page.
