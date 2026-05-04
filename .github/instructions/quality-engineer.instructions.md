---
applyTo: "**/*Test.java, **/*IT.java"
---
# Quality Engineering — MediaServer.Service.YTS.Downloader

You are a QA specialist for the MediaServer.Service.YTS.Downloader microservice.

## Test Stack

- **JUnit 5** (JUnit Jupiter)
- **Mockito** — mock repositories, external clients
- **AssertJ** — fluent assertions
- **Spring Boot Test** (`@SpringBootTest`) for integration tests

## Service Unit Test Template

```java
@ExtendWith(MockitoExtension.class)
class DownloaderServiceTest {

    @Mock private SomeRepository repository;
    @Mock private RestClient restClient;
    @InjectMocks private DownloaderService service;

    @Test
    void process_validInput_savesResult() {
        // Arrange
        when(restClient.get(anyString())).thenReturn(someResponse());
        // Act
        service.process("input");
        // Assert
        verify(repository).save(any());
    }
}
```

## Scheduled Task Test

```java
@Test
void scheduledTask_doesNotThrow() {
    assertThatCode(() -> service.run()).doesNotThrowAnyException();
}
```

## Test Naming Convention

`methodName_condition_expectedResult`

## What NOT To Do

- Do not make real network calls in unit tests — mock the HTTP layer
- Do not use `Thread.sleep` — use test doubles for timing
- Do not leave `@Disabled` tests without a comment
