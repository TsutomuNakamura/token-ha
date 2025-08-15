# token-ha

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
