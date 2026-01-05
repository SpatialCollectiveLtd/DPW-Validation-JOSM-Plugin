# JUnit Testing Setup

## Required Dependencies

To run the tests, you need to download the following JAR files and place them in the `lib/` directory:

### JUnit 5 (Jupiter)
- **junit-jupiter-api-5.10.1.jar**
  - Download: https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.10.1/junit-jupiter-api-5.10.1.jar
  
- **junit-jupiter-engine-5.10.1.jar**
  - Download: https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-engine/5.10.1/junit-jupiter-engine-5.10.1.jar

- **junit-platform-launcher-1.10.1.jar**
  - Download: https://repo1.maven.org/maven2/org/junit/platform/junit-platform-launcher/1.10.1/junit-platform-launcher-1.10.1.jar

### Mockito (for mocking)
- **mockito-core-5.8.0.jar**
  - Download: https://repo1.maven.org/maven2/org/mockito/mockito-core/5.8.0/mockito-core-5.8.0.jar

- **byte-buddy-1.14.10.jar** (Mockito dependency)
  - Download: https://repo1.maven.org/maven2/net/bytebuddy/byte-buddy/1.14.10/byte-buddy-1.14.10.jar

- **objenesis-3.3.jar** (Mockito dependency)
  - Download: https://repo1.maven.org/maven2/org/objenesis/objenesis/3.3/objenesis-3.3.jar

## Quick Download Script (PowerShell)

Run this in PowerShell from the plugin root directory:

```powershell
# Create lib directory if it doesn't exist
New-Item -ItemType Directory -Force -Path "lib"

# Download JUnit 5
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.10.1/junit-jupiter-api-5.10.1.jar" -OutFile "lib/junit-jupiter-api-5.10.1.jar"
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-engine/5.10.1/junit-jupiter-engine-5.10.1.jar" -OutFile "lib/junit-jupiter-engine-5.10.1.jar"
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-launcher/1.10.1/junit-platform-launcher-1.10.1.jar" -OutFile "lib/junit-platform-launcher-1.10.1.jar"

# Download Mockito and dependencies
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/mockito/mockito-core/5.8.0/mockito-core-5.8.0.jar" -OutFile "lib/mockito-core-5.8.0.jar"
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/net/bytebuddy/byte-buddy/1.14.10/byte-buddy-1.14.10.jar" -OutFile "lib/byte-buddy-1.14.10.jar"
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/objenesis/objenesis/3.3/objenesis-3.3.jar" -OutFile "lib/objenesis-3.3.jar"

Write-Host "✓ All test dependencies downloaded successfully!" -ForegroundColor Green
```

## Running Tests

### Compile Tests
```bash
ant compile-tests
```

### Run All Tests
```bash
ant test
```

### Build Plugin (skips tests)
```bash
ant dist
```

### Clean Build Artifacts
```bash
ant clean
```

## Test Structure

- `test/` - Test source files (mirrors `src/` structure)
- `test-reports/` - JUnit XML test reports
- `build/test/` - Compiled test classes

## Writing Tests

All test classes should:
- Be in the same package as the class they test
- End with `Test` suffix (e.g., `InputValidatorTest.java`)
- Use JUnit 5 annotations: `@Test`, `@DisplayName`, `@BeforeEach`, etc.
- Follow AAA pattern: Arrange, Act, Assert

Example:
```java
@Test
@DisplayName("Should validate correct input")
void testValidInput() throws Exception {
    // Arrange
    String input = "valid-input";
    
    // Act
    String result = InputValidator.validateTaskId(input);
    
    // Assert
    assertEquals("valid-input", result);
}
```

## Current Test Coverage

- ✅ `ValidationConstantsTest` - 10 tests covering constants validation
- ✅ `InputValidatorTest` - 38 tests covering all input validation methods
- ⏳ `DPWAPIClientTest` - TODO: Mock HTTP requests
- ⏳ `DialogHelperTest` - TODO: Mock Swing components

## CI/CD Integration

Test reports are generated in `test-reports/` in JUnit XML format, compatible with:
- GitHub Actions
- Jenkins
- GitLab CI
- Azure DevOps

## Troubleshooting

### "Cannot find JUnit classes"
- Ensure all JAR files are in `lib/` directory
- Run the PowerShell download script above

### "Tests not running"
- Verify test classes end with `Test`
- Check Java version is 21+
- Ensure `ant compile-tests` completes without errors

### "OutOfMemoryError"
- Increase Ant heap size: `export ANT_OPTS="-Xmx1024m"`
