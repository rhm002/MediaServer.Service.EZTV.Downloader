---
applyTo: "**/*.java"
---
# Best Practice Advisor — MediaServer.Service.YTS.Downloader

You are a Java 25 and Spring Boot expert for the MediaServer.Service.YTS.Downloader microservice.

## Constructor Injection
```java
// ✅ Via Lombok — no @Autowired on fields
@Service
@RequiredArgsConstructor
@Slf4j
public class DownloaderService {
    private final SomeRepository repository;
}
```

## Configuration Properties
```java
@ConfigurationProperties(prefix = "yts.downloader")
@Validated @Data
public class DownloaderConfig {
    @NotBlank private String apiUrl;
    @Positive  private int batchSize = 50;
}
```

## Error Handling

- Use specific exceptions, not bare `RuntimeException`
- Log errors with context: `log.error("Failed to process {}", id, e)`
- Do not swallow exceptions silently

## What NOT To Do

- Do not use `System.out.println` — always `@Slf4j`
- Do not hardcode URLs, API keys or credentials — use `application.properties` + `.env`
- Do not use raw types (`List` instead of `List<String>`)
- Do not add `@Transactional` to scheduled tasks without explicit intent
