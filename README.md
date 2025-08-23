# TokenHa - Simple Token Management Library

A lightweight Java library for managing tokens with automatic eviction, cooldown periods, and file persistence with locking mechanisms.

## Features

- **Token Queue Management**: FIFO queue with configurable size limits
- **Automatic Eviction**: Background thread automatically removes expired tokens
- **Cooldown Protection**: Configurable cooldown period between token additions
- **File Persistence**: Automatic saving/loading with exclusive file locking
- **Thread-Safe**: All operations are synchronized for concurrent access
- **Memory Efficient**: Uses WeakReference for automatic cleanup

## Quick Start

```java
// Create a TokenHa instance
TokenHa tokenHa = new TokenHa();

// Set custom persistence file (optional)
tokenHa.setPersistenceFilePath("my-tokens.json");

// Add tokens (respects size limits and cooldown)
tokenHa.addIfAvailable("user123");
tokenHa.addIfAvailable("user456");

// Check queue status
System.out.println("Queue size: " + tokenHa.getQueueSize());
System.out.println("Can add more: " + tokenHa.availableToAdd());

// Get newest token
TokenElement newest = tokenHa.newestToken();

// Export to JSON
String json = tokenHa.toJson();

// Load from file
tokenHa.loadFromFile();

// Always close when done
tokenHa.close();
```

## Architecture

### Core Classes

- **TokenHa**: Main class for token management
- **TokenElement**: Individual token with timestamp
- **EvictionThread**: Singleton background thread for automatic cleanup
- **FilePersistence**: Handles file I/O with exclusive locking

### Key Features

#### Automatic Eviction
The singleton `EvictionThread` automatically removes expired tokens from all `TokenHa` instances. The eviction timing is configurable through `EvictionThreadConfig`:
- **Initial Delay**: How long to wait before first eviction run (default: 1000ms)
- **Interval**: How often to run eviction checks (default: 10000ms)
- At least 1 token is always preserved regardless of expiration

#### Cooldown Management
Prevents rapid token addition with a configurable cooldown period (default 1000ms).

#### File Persistence with Locking
- Automatic saving on every token addition
- Exclusive file locking prevents data corruption
- Uses `RandomAccessFile` with `FileLock` for process-level locking

## Configuration

### Builder Pattern Configuration

Use the `TokenHaConfig.Builder` for comprehensive configuration:

```java
TokenHaConfig config = new TokenHaConfig.Builder()
    .maxTokens(20)
    .expirationTimeMillis(30000)    // 30 seconds
    .cooldownTimeMillis(2000)       // 2 seconds
    .minTokensToKeep(2)
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
tokenha.cooldown.time.millis=1500
tokenha.min.tokens.to.keep=2
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
- **Cooldown Time**: 1000ms (1 second)
- **Min Tokens to Keep**: 1
- **Eviction Initial Delay**: 1000ms (1 second)
- **Eviction Interval**: 10000ms (10 seconds)

## Thread Safety

All public methods are synchronized, making the library safe for concurrent access across multiple threads.

## File Locking

The library uses exclusive file locking to prevent data corruption when multiple instances access the same persistence file. Only one process can write to a file at a time.

## Running the Demo

```bash
mvn compile exec:java -Dexec.mainClass="com.github.tsutomunakamura.tokenha.Demo"
```

This will demonstrate:
- Token addition with cooldown enforcement
- Queue size management
- JSON serialization
- File persistence with locking
- Automatic eviction thread lifecycle

## Testing

Run all tests:
```bash
mvn test
```

The test suite covers:
- Basic token operations
- Eviction thread functionality  
- File persistence with locking
- Thread safety
- Edge cases and error conditions

A Java library for token handling with Maven wrapper support.

## Building the Project

This project includes the Maven wrapper, so you don't need to install Maven to build it.

### Prerequisites
- Java 17 or higher

### Build Commands

```bash
# Compile the project
./mvnw compile

# Run tests
./mvnw test

# Package into JAR
./mvnw package

# Clean build artifacts
./mvnw clean

# Full clean build and test
./mvnw clean compile test
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
├── src/
│   ├── main/java/          # Source code
│   └── test/java/          # Test code
├── .mvn/wrapper/           # Maven wrapper files
├── mvnw                    # Maven wrapper script (Unix/Linux/macOS)
├── mvnw.cmd               # Maven wrapper script (Windows)
├── pom.xml                # Maven project file
└── .gitignore             # Git ignore rules
```
