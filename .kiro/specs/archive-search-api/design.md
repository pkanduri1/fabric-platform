# Archive Search API Design Document

## Overview

The Archive Search API provides comprehensive REST endpoints for searching, downloading, and uploading files from both regular directories and archive files. The API includes LDAP-based user authentication, secure file operations, and comprehensive audit logging. This feature will be integrated into the existing database-script-watcher project (interfaces-utils) and will be restricted to non-production environments only. The API will be fully documented and testable through Swagger UI.

## Architecture

### High-Level Architecture

```mermaid
graph TB
    A[Web UI] --> B[Authentication Controller]
    B --> C[LDAP Service]
    C --> D[Active Directory]
    
    A --> E[Archive Search Controller]
    E --> F[Archive Search Service]
    E --> G[File Upload Service]
    
    F --> H[File System Service]
    F --> I[Archive Handler Service]
    F --> J[Content Search Service]
    
    G --> K[Upload Validator]
    G --> H
    
    H --> L[Directory Scanner]
    I --> M[ZIP Handler]
    I --> N[TAR Handler]
    I --> O[Other Archive Handlers]
    J --> P[Text Search Engine]
    
    B --> Q[Audit Service]
    E --> Q
    G --> Q
    Q --> R[Audit Log File]
    
    S[Environment Guard] --> E
    S --> B
    T[Security Validator] --> E
    T --> B
    
    subgraph "External Dependencies"
        U[File System]
        V[Archive Files]
        W[Linux Server]
    end
    
    L --> U
    M --> V
    N --> V
    O --> V
    G --> W
```

### Integration with Existing Project

The Archive Search API will be integrated into the database-script-watcher project as follows:

- **Package Structure**: `com.fabric.watcher.archive`
- **Configuration**: Extend existing application properties
- **Security**: Leverage existing security framework
- **Monitoring**: Integrate with existing metrics and health checks

## Components and Interfaces

### 1. REST Controller Layer

#### AuthenticationController
```java
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(name = "archive.search.enabled", havingValue = "true")
public class AuthenticationController {
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody AuthRequest request);
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request);
    
    @GetMapping("/validate")
    public ResponseEntity<ValidationResponse> validateSession();
}
```

#### ArchiveSearchController
```java
@RestController
@RequestMapping("/api/v1/archive")
@ConditionalOnProperty(name = "archive.search.enabled", havingValue = "true")
public class ArchiveSearchController {
    
    @GetMapping("/search")
    public ResponseEntity<FileSearchResponse> searchFiles(
        @RequestParam String path,
        @RequestParam String pattern
    );
    
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(
        @RequestParam String filePath
    );
    
    @PostMapping("/content-search")
    public ResponseEntity<ContentSearchResponse> searchContent(
        @RequestBody ContentSearchRequest request
    );
    
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadFile(
        @RequestParam MultipartFile file,
        @RequestParam String targetPath
    );
}
```

### 2. Service Layer

#### LdapAuthenticationService
```java
@Service
public class LdapAuthenticationService {
    
    public AuthenticationResult authenticate(String userId, String password);
    public UserDetails getUserDetails(String userId);
    public boolean isUserAuthorized(String userId, String operation);
}
```

#### ArchiveSearchService
```java
@Service
public class ArchiveSearchService {
    
    public FileSearchResponse searchFiles(String path, String pattern, String userId);
    public InputStream downloadFile(String filePath, String userId);
    public ContentSearchResponse searchContent(String filePath, String searchTerm, String userId);
    private boolean isPathAllowed(String path);
    private List<FileInfo> searchInArchive(Path archivePath, String pattern);
}
```

#### FileUploadService
```java
@Service
public class FileUploadService {
    
    public UploadResult uploadFile(MultipartFile file, String targetPath, String userId);
    public boolean validateUploadPath(String path);
    public boolean validateFileType(String fileName);
    private void transferToServer(MultipartFile file, String targetPath);
}
```

#### FileSystemService
```java
@Service
public class FileSystemService {
    
    public List<FileInfo> scanDirectory(Path directory, String pattern);
    public boolean isValidPath(String path);
    public FileMetadata getFileMetadata(Path file);
}
```

#### ArchiveHandlerService
```java
@Service
public class ArchiveHandlerService {
    
    public List<FileInfo> extractAndSearch(Path archivePath, String pattern);
    public InputStream extractFile(Path archivePath, String fileName);
    public boolean isArchiveFile(Path file);
    private ArchiveHandler getHandler(String fileExtension);
}
```

#### ContentSearchService
```java
@Service
public class ContentSearchService {
    
    public ContentSearchResponse searchInFile(Path filePath, String searchTerm);
    public ContentSearchResponse searchInArchiveFile(Path archivePath, String fileName, String searchTerm);
    private List<String> findMatchingLines(InputStream content, String searchTerm);
}
```

#### ArchiveSearchAuditService
```java
@Service
public class ArchiveSearchAuditService {
    
    public void logAuthentication(String userId, boolean success, String ipAddress);
    public void logFileUpload(String userId, String fileName, String targetPath, boolean success);
    public void logFileDownload(String userId, String fileName, boolean success);
    public void logFileSearch(String userId, String searchTerm, int resultCount);
    public void logSecurityEvent(String userId, String eventType, String details);
    private void writeAuditEntry(AuditEntry entry);
}
```

### 3. Security and Validation Layer

#### SecurityValidator
```java
@Component
public class SecurityValidator {
    
    public boolean isPathAllowed(String path);
    public boolean isFileAccessible(Path file);
    public String sanitizePath(String path);
    private boolean isPathTraversalAttempt(String path);
}
```

#### EnvironmentGuard
```java
@Component
@ConditionalOnProperty(name = "archive.search.enabled", havingValue = "true")
public class EnvironmentGuard {
    
    @PostConstruct
    public void validateEnvironment();
    public boolean isNonProductionEnvironment();
}
```

## Data Models

### Request/Response Models

#### Authentication Models
```java
public class AuthRequest {
    private String userId;
    private String password;
}

public class AuthResponse {
    private String token;
    private String userId;
    private long expiresIn;
    private List<String> permissions;
}

public class AuthenticationResult {
    private boolean success;
    private String userId;
    private String errorMessage;
    private UserDetails userDetails;
}
```

#### File Operation Models
```java
public class FileSearchResponse {
    private List<FileInfo> files;
    private int totalCount;
    private String searchPath;
    private String searchPattern;
    private long searchTimeMs;
}

public class FileInfo {
    private String fileName;
    private String fullPath;
    private String relativePath;
    private long size;
    private LocalDateTime lastModified;
    private FileType type; // REGULAR, ARCHIVE_ENTRY
    private String archivePath; // null for regular files
}

public class UploadRequest {
    private MultipartFile file;
    private String targetPath;
    private boolean overwrite;
}

public class UploadResponse {
    private boolean success;
    private String fileName;
    private String targetPath;
    private long fileSize;
    private String message;
}

public class ContentSearchRequest {
    private String filePath;
    private String searchTerm;
    private boolean caseSensitive = false;
    private boolean wholeWord = false;
}

public class ContentSearchResponse {
    private List<SearchMatch> matches;
    private int totalMatches;
    private boolean truncated;
    private String downloadSuggestion;
    private long searchTimeMs;
}

public class SearchMatch {
    private int lineNumber;
    private String lineContent;
    private int columnStart;
    private int columnEnd;
}
```

#### Audit Models
```java
public class AuditEntry {
    private LocalDateTime timestamp;
    private String userId;
    private String operation;
    private String resource;
    private boolean success;
    private String details;
    private String ipAddress;
}
```

### Configuration Models

#### ArchiveSearchProperties
```java
@ConfigurationProperties(prefix = "archive.search")
public class ArchiveSearchProperties {
    private boolean enabled = false;
    private List<String> allowedPaths = new ArrayList<>();
    private List<String> excludedPaths = new ArrayList<>();
    private int maxFileSize = 100 * 1024 * 1024; // 100MB
    private int maxSearchResults = 100;
    private int searchTimeoutSeconds = 30;
    private List<String> supportedArchiveTypes = Arrays.asList("zip", "tar", "tar.gz", "jar");
    
    // LDAP Configuration
    private LdapConfig ldap = new LdapConfig();
    
    // Upload Configuration
    private UploadConfig upload = new UploadConfig();
    
    // Audit Configuration
    private AuditConfig audit = new AuditConfig();
    
    public static class LdapConfig {
        private String url;
        private String baseDn;
        private String userSearchBase;
        private String userSearchFilter;
        private int connectionTimeout = 5000;
        private int readTimeout = 10000;
    }
    
    public static class UploadConfig {
        private String uploadDirectory;
        private List<String> allowedExtensions;
        private long maxUploadSize = 100 * 1024 * 1024; // 100MB
        private String tempDirectory;
    }
    
    public static class AuditConfig {
        private String logFile;
        private String maxFileSize = "10MB";
        private int maxHistory = 30;
    }
}
```

## Error Handling

### Exception Hierarchy

```java
public class ArchiveSearchException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String details;
}

public enum ErrorCode {
    PATH_NOT_ALLOWED("ARCH001", "Path access denied"),
    FILE_NOT_FOUND("ARCH002", "File not found"),
    ARCHIVE_CORRUPTED("ARCH003", "Archive file is corrupted"),
    SEARCH_TIMEOUT("ARCH004", "Search operation timed out"),
    UNSUPPORTED_FORMAT("ARCH005", "Unsupported file format"),
    ENVIRONMENT_RESTRICTED("ARCH006", "Feature disabled in production");
}
```

### Global Exception Handler

```java
@ControllerAdvice
public class ArchiveSearchExceptionHandler {
    
    @ExceptionHandler(ArchiveSearchException.class)
    public ResponseEntity<ErrorResponse> handleArchiveSearchException(ArchiveSearchException ex);
    
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex);
}
```

## Testing Strategy

### Unit Testing
- **Controller Tests**: Mock service dependencies, test request/response mapping
- **Service Tests**: Test business logic with mocked dependencies
- **Security Tests**: Validate path traversal prevention and access controls
- **Archive Handler Tests**: Test different archive formats and edge cases

### Integration Testing
- **End-to-End API Tests**: Test complete request flow through REST endpoints
- **File System Integration**: Test with real files and directories
- **Archive Processing**: Test with various archive formats and sizes
- **Environment Configuration**: Test production vs non-production behavior

### Performance Testing
- **Large File Handling**: Test with files approaching size limits
- **Concurrent Requests**: Test multiple simultaneous search operations
- **Memory Usage**: Monitor memory consumption during archive processing
- **Timeout Handling**: Verify proper timeout behavior for long operations

## Security Considerations

### Path Traversal Prevention
- Validate all input paths against allowed directories
- Sanitize path parameters to prevent "../" attacks
- Use canonical path resolution to detect traversal attempts

### Access Control
- Implement role-based access if authentication is available
- Log all file access attempts for audit purposes
- Restrict file types that can be accessed

### Resource Protection
- Implement file size limits to prevent memory exhaustion
- Set operation timeouts to prevent resource starvation
- Limit concurrent operations per user/session

### Environment Isolation
- Automatically detect production environment
- Disable API endpoints in production
- Provide clear error messages when disabled

## Configuration

### Application Properties

```yaml
archive:
  search:
    enabled: ${ARCHIVE_SEARCH_ENABLED:false}
    allowed-paths:
      - "/data/archives"
      - "/tmp/search"
    excluded-paths:
      - "/data/archives/sensitive"
    max-file-size: 104857600  # 100MB
    max-search-results: 100
    search-timeout-seconds: 30
    supported-archive-types:
      - zip
      - tar
      - tar.gz
      - jar
    
    # LDAP Configuration
    ldap:
      url: ${LDAP_URL:ldap://your-ad-server:389}
      base-dn: ${LDAP_BASE_DN:dc=company,dc=com}
      user-search-base: ${LDAP_USER_SEARCH_BASE:ou=users}
      user-search-filter: ${LDAP_USER_SEARCH_FILTER:(sAMAccountName={0})}
      connection-timeout: 5000
      read-timeout: 10000
    
    # Upload Configuration
    upload:
      upload-directory: ${UPLOAD_DIRECTORY:/opt/uploads}
      allowed-extensions: [.txt, .sql, .xml, .json, .properties, .yml, .yaml]
      max-upload-size: 104857600  # 100MB
      temp-directory: ${TEMP_DIRECTORY:/tmp/file-uploads}
    
    # Audit Configuration
    audit:
      log-file: ${AUDIT_LOG_FILE:/var/log/archive-search/audit.log}
      max-file-size: 10MB
      max-history: 30

# Environment detection
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

# Security Configuration
security:
  session-timeout: 30m
  max-login-attempts: 3
  lockout-duration: 15m

# Swagger configuration
springdoc:
  api-docs:
    groups:
      enabled: true
  group-configs:
    - group: archive-search
      paths-to-match: /api/v1/archive/**,/api/v1/auth/**
      display-name: Archive Search API with Authentication
```

### Environment Variables

- `ARCHIVE_SEARCH_ENABLED`: Enable/disable the API (default: false)
- `SPRING_PROFILES_ACTIVE`: Environment profile (prod disables API)
- `ARCHIVE_ALLOWED_PATHS`: Comma-separated list of allowed base paths
- `ARCHIVE_MAX_FILE_SIZE`: Maximum file size for processing

## Monitoring and Observability

### Metrics
- Request count and response times for each endpoint
- File search operation duration and result counts
- Archive processing times by format
- Error rates by error type

### Health Checks
- Archive search service availability
- File system access validation
- Configuration validation

### Logging
- All API requests with parameters (excluding sensitive data)
- File access attempts and results
- Security violations and blocked requests
- Performance metrics for large operations

## Deployment Considerations

### Environment Detection
The application will automatically detect the environment using:
1. `SPRING_PROFILES_ACTIVE` environment variable
2. System properties
3. Default to production (disabled) if detection fails

### Resource Requirements
- Additional memory for archive processing (recommend +512MB)
- Temporary disk space for archive extraction
- Network bandwidth for file downloads

### Monitoring Integration
- Integrate with existing Prometheus metrics
- Add Grafana dashboard panels for archive search metrics
- Configure alerts for high error rates or performance issues