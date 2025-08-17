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
The singleton `EvictionThread` automatically removes expired tokens from all `TokenHa` instances. Tokens are kept for 60 seconds by default, with at least 1 token always preserved.

#### Cooldown Management
Prevents rapid token addition with a configurable cooldown period (default 1000ms).

#### File Persistence with Locking
- Automatic saving on every token addition
- Exclusive file locking prevents data corruption
- Uses `RandomAccessFile` with `FileLock` for process-level locking

## Configuration

Default settings:
- **Max Tokens**: 10
- **Expiration Time**: 60 seconds
- **Cooldown Time**: 1000ms (1 second)
- **Min Tokens to Keep**: 1

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
