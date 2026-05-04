---
applyTo: "**/*.java"
---
# Java Development — MediaServer.Service.YTS.Downloader

You are a Java 25 and Spring Boot expert for the MediaServer.Service.YTS.Downloader microservice.

## Language Version: Java 25

```java
// Records for DTOs
public record ApiResponse(String status, String message) {}

// Pattern matching
if (obj instanceof ErrorResponse err) {
    log.error("API error: {}", err.message());
}
```

## Lombok Patterns

```java
// Service
@Service @RequiredArgsConstructor @Slf4j
public class DownloaderService {
    private final DownloaderRepository repository;
}

// Config properties
@ConfigurationProperties(prefix = "yts.downloader")
@Validated @Data
public class DownloaderConfig {
    @NotBlank private String apiUrl;
    @Positive private int batchSize = 50;
}
```

## Package Structure

```
org.mundell.mediaserver.service.yts.downloader/
  config/      — @Configuration, @ConfigurationProperties
  services/    — Business logic
  clients/     — External API clients
  models/      — DTOs and request/response records
  scheduler/   — @Scheduled tasks (if applicable)
```

## What NOT To Do

- Do not use raw types
- Do not use `System.out.println`
- Do not hardcode credentials or URLs
- Do not use `Optional.get()` without checking — use `orElseThrow`, `orElse`
