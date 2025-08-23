# TokenHa Configuration Strategies

This document outlines different strategies for configuring TokenHa parameters externally.

## 1. Builder Pattern (Programmatic Configuration)

```java
// Create custom configuration programmatically
TokenHaConfig config = new TokenHaConfig.Builder()
    .maxTokens(20)
    .coolTimeToAddMillis(2000)
    .numberOfLastTokens(2)
    .expirationTimeSeconds(120000)
    .persistenceFilePath("custom-tokens.json")
    .build();

TokenHa tokenHa = new TokenHa(config);
```

## 2. Properties File Configuration

Create a `tokenha.properties` file:
```properties
tokenha.max.tokens=15
tokenha.cool.time.millis=1500
tokenha.number.of.last.tokens=2
tokenha.expiration.time.seconds=90000
tokenha.persistence.file.path=app-tokens.json
```

Load configuration:
```java
Properties props = new Properties();
props.load(new FileInputStream("tokenha.properties"));
TokenHaConfig config = TokenHaConfig.fromProperties(props);
TokenHa tokenHa = new TokenHa(config);
```

## 3. Environment Variables

Set environment variables:
```bash
export TOKENHA_MAX_TOKENS=25
export TOKENHA_COOL_TIME_MILLIS=500
export TOKENHA_NUMBER_OF_LAST_TOKENS=3
export TOKENHA_EXPIRATION_TIME_SECONDS=180000
export TOKENHA_PERSISTENCE_FILE_PATH=/tmp/tokens.json
```

Load configuration:
```java
TokenHaConfig config = TokenHaConfig.fromEnvironment();
TokenHa tokenHa = new TokenHa(config);
```

## 4. System Properties

Set system properties:
```bash
java -Dtokenha.max.tokens=30 \
     -Dtokenha.cool.time.millis=800 \
     -Dtokenha.number.of.last.tokens=1 \
     -Dtokenha.expiration.time.seconds=45000 \
     -Dtokenha.persistence.file.path=system-tokens.json \
     YourMainClass
```

Load configuration:
```java
TokenHaConfig config = TokenHaConfig.fromProperties(System.getProperties());
TokenHa tokenHa = new TokenHa(config);
```

## 5. Mixed Configuration (Override Pattern)

```java
// Start with environment variables
TokenHaConfig.Builder builder = TokenHaConfig.fromEnvironment().toBuilder();

// Override specific values programmatically
TokenHaConfig config = builder
    .maxTokens(50)  // Override max tokens
    .coolTimeToAddMillis(100)  // Override cool time
    .build();

TokenHa tokenHa = new TokenHa(config);
```

## Benefits of Each Approach

### 1. Builder Pattern
- **Pros**: Type-safe, compile-time validation, IDE support
- **Cons**: Requires code changes for configuration updates
- **Best for**: Application-specific configurations, testing

### 2. Properties File
- **Pros**: External configuration, easy to modify without recompilation
- **Cons**: Runtime parsing, potential for typos
- **Best for**: Application deployment configurations

### 3. Environment Variables
- **Pros**: Container-friendly, cloud-native, OS-level configuration
- **Cons**: Limited data types, system-wide scope
- **Best for**: Docker containers, microservices, cloud deployments

### 4. System Properties
- **Pros**: JVM-level configuration, can override at runtime
- **Cons**: Java-specific, command-line can become unwieldy
- **Best for**: Development, debugging, temporary overrides

## Validation and Error Handling

All configuration methods include:
- Input validation with meaningful error messages
- Default fallback values for invalid inputs
- Comprehensive logging of configuration issues
- Builder pattern validation to ensure consistent state

## Migration Path

For existing code using the default constructor:
```java
// Old way (still works)
TokenHa tokenHa = new TokenHa();

// New way with explicit default config
TokenHa tokenHa = new TokenHa(TokenHaConfig.defaultConfig());
```

The default constructor is preserved for backward compatibility.
