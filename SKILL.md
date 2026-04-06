---
name: axim-restdoc-generator
description: Auto-generate REST API documentation from Spring Boot controllers using the Axim Gradle RestDoc Generator plugin. Use when configuring restMetaGenerator DSL, writing Javadoc tags (@response, @group, @auth, @error, @header), generating OpenAPI 3.0.3 specs, spec-bundle.json, error code scanning, Postman sync, @XApiIgnore exclusion, or troubleshooting the gradle-restdoc-generator plugin.
---

# Axim Gradle RestDoc Generator

A Gradle plugin that auto-generates REST API documentation by scanning Spring Boot `@RestController` classes and their Javadoc comments.

**Version:** 2.1.7
**Requirements:** Java 17+, Gradle 7.0+, Spring Boot
**Repository:** https://github.com/Axim-one/gradle-restdoc-generator

## Installation

### build.gradle (buildscript)

```groovy
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'com.github.Axim-one:gradle-restdoc-generator:2.1.7'
    }
}

apply plugin: 'gradle-restdoc-generator'
```

### settings.gradle (Plugin DSL)

```groovy
pluginManagement {
    repositories {
        maven { url 'https://jitpack.io' }
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == 'gradle-restdoc-generator') {
                useModule("com.github.Axim-one:gradle-restdoc-generator:${requested.version}")
            }
        }
    }
}
```

```groovy
plugins {
    id 'gradle-restdoc-generator' version '2.1.7'
}
```

## Configuration DSL

```groovy
restMetaGenerator {
    // Required
    documentPath = 'build/docs'
    basePackage = 'com.example'
    serviceId = 'my-service'

    // Service info (optional)
    serviceName = 'My Service'
    apiServerUrl = 'https://api.example.com'
    serviceVersion = 'v1.0'
    introductionFile = 'docs/introduction.md'

    // Error code / response class (optional)
    errorCodeClass = 'com.example.exception.ErrorCode'
    errorResponseClass = 'com.example.dto.ApiErrorResponse'

    // Exclusion (v2.1.1+)
    excludePackages = ['com.example.internal']
    excludeClasses = ['HealthCheckController']

    // Authentication — API Key
    auth {
        type = 'apiKey'              // or 'token' (backward compat alias)
        headerKey = 'Access-Token'
        value = '{{accessToken}}'
        in = 'header'                // 'header', 'query', 'cookie'
        descriptionFile = 'docs/auth.md'
    }

    // Authentication — Bearer (v2.0.7+)
    // auth {
    //     type = 'http'
    //     scheme = 'bearer'
    //     bearerFormat = 'JWT'
    //     value = '{{accessToken}}'
    // }

    // Authentication — Basic (v2.0.7+)
    // auth {
    //     type = 'http'
    //     scheme = 'basic'
    //     value = '{{username}}:{{password}}'
    // }

    // Common headers (optional)
    header('X-Custom-Header', 'default-value', 'Header description')

    // Postman environments (optional)
    environment('DEV') {
        variable('base_url', 'https://dev.api.example.com')
        variable('token', 'dev-token')
    }

    // Postman sync (optional)
    postmanApiKey = ''
    postmanWorkSpaceId = ''

    debug = false
}
```

### DSL Property Reference

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `documentPath` | **Yes** | — | Output directory (relative to project root) |
| `basePackage` | **Yes** | — | Package(s) to scan (comma-separated for multiple) |
| `serviceId` | **Yes** | — | Service unique ID (used as JSON filename) |
| `serviceName` | No | `""` | Display name (OpenAPI title) |
| `apiServerUrl` | No | `""` | API base URL (OpenAPI servers) |
| `serviceVersion` | No | `"v1.0"` | API version (OpenAPI version) |
| `introductionFile` | No | `""` | Markdown file path for service description |
| `errorCodeClass` | No | `""` | ErrorCode class FQCN (fallback: framework default) |
| `errorResponseClass` | No | `""` | Error Response DTO FQCN (fallback: ApiError) |
| `excludePackages` | No | `[]` | Package prefixes to exclude from doc generation (v2.1.1+) |
| `excludeClasses` | No | `[]` | Controller simple names to exclude (v2.1.1+) |
| `postmanApiKey` | No | `""` | Postman API Key (empty = skip sync) |
| `postmanWorkSpaceId` | No | `""` | Postman Workspace ID |
| `debug` | **Yes** | `false` | Enable debug logging |

## API Exclusion (v2.1.1+)

### @XApiIgnore Annotation

Exclude controllers or individual methods from documentation:

```java
// Exclude entire controller
@XApiIgnore
@RestController
public class InternalController { ... }

// Exclude single method
@RestController
public class UserController {
    @XApiIgnore
    @GetMapping("/debug")
    public String debug() { ... }
}
```

The annotation is auto-added to project classpath when the plugin is applied. No extra dependency needed.

### DSL-based Exclusion

```groovy
restMetaGenerator {
    excludePackages = ['com.example.internal', 'com.example.admin']
    excludeClasses = ['HealthCheckController', 'ActuatorController']
}
```

## Javadoc Tags

Write Javadoc on controller methods to enrich the generated documentation:

```java
/**
 * 사용자 정보를 조회합니다.
 *
 * @param userId 사용자 ID
 * @return 사용자 정보
 * @response 200 성공
 * @response 404 사용자를 찾을 수 없음
 * @group 사용자 관리
 * @auth true
 * @header X-Request-Id 요청 추적 ID
 * @error UserNotFoundException
 */
@GetMapping("/users/{userId}")
public UserDto getUser(@PathVariable Long userId) { ... }
```

### Tag Reference

| Tag | Description |
|-----|-------------|
| `@param` | Parameter description |
| `@return` | Return value description |
| `@response {code} {desc}` | HTTP response status code and description |
| `@error {ExceptionClass}` | Link error group — auto-populates errors and responseStatus |
| `@throws {ExceptionClass}` | Same as `@error` (standard Javadoc tag) |
| `@group` | API group name (maps to Postman folder) |
| `@auth true` | Mark endpoint as requiring authentication |
| `@header {name} {desc}` | Document a custom header |
| `@className` | Override generated class name |

NOTE: Method signature `throws` clauses are also auto-detected and linked to error groups (no Javadoc tag needed).

## Query Parameter Object (v2.1.0+)

Complex objects bound as query parameters are auto-expanded into individual parameters:

```java
public class UserSearchRequest {
    private String name;
    @NotNull
    private UserStatus status;    // enum → dropdown with values
    private Long networkId;
    private boolean active;       // primitive → required
}

@GetMapping(name = "사용자 검색", value = "/search")
public List<UserDto> searchUsers(UserSearchRequest search) { ... }
```

No `@XQueryParams` annotation needed — any Object-typed parameter without `@RequestBody`/`@PathVariable` is auto-expanded.

## OpenAPI 3.0 Features (v2.1.0+)

- **operationId**: Controller method name (e.g., `getUser`, `searchUsers`) instead of MD5 hash
- **enum values**: Auto-extracted `"enum": ["ACTIVE", "SUSPENDED", ...]` arrays with example
- **required fields**: `@NotNull`, `@NotBlank`, `@NotEmpty`, `@Size(min=1)`, primitive types
- **ApiError schema**: Auto-attached to 4xx/5xx responses with `$ref`
- **Date/time inline**: `LocalDateTime` → `date-time`, `LocalDate` → `date`, `LocalTime` → `time`
- **BigDecimal format**: `"format": "decimal"`
- **Example values**: Type-based defaults (Long→1, Boolean→true, Enum→first value, BigDecimal→"100.00")
- **Set/Collection**: `Set<T>`, `Collection<T>` → `array` + `items` (not string)
- **External models**: Classes outside `basePackage` (e.g., common module entities) are generated

## Error Code Documentation

Exception classes with `public static final ErrorCode` fields are auto-scanned:

```java
public class UserNotFoundException extends RuntimeException {
    public static final ErrorCode USER_NOT_FOUND =
        new ErrorCode("USER_001", "error.user.notfound");
}
```

## Pagination Support

Spring Data `Pageable`/`Page<T>` are auto-recognized:

```java
@GetMapping(name = "사용자 페이징 조회", value = "/paged")
public Page<UserDto> getUsers(Pageable pageable) { ... }
```

## ApiResult Wrapper Unwrapping

`ApiResult<T>` wrapper types are auto-unwrapped:

```java
public ApiResult<UserDto> getUser() { ... }           // → returnClass: "UserDto"
public ApiResult<List<UserDto>> getUsers() { ... }     // → isArrayReturn: true
public ApiResult<Page<UserDto>> getUsersPaged() { ... } // → isPaging: true
```

## Auth DSL Types (v2.0.7+)

| Type | Config | OpenAPI mapping |
|------|--------|-----------------|
| API Key | `type='apiKey'`, `in='header'`, `headerKey='X-Token'` | `{ type: "apiKey", in: "header", name: "X-Token" }` |
| Bearer | `type='http'`, `scheme='bearer'`, `bearerFormat='JWT'` | `{ type: "http", scheme: "bearer" }` |
| Basic | `type='http'`, `scheme='basic'` | `{ type: "http", scheme: "basic" }` |

## Running

```bash
./gradlew restMetaGenerator
```

## Output Structure

```
build/docs/
├── {serviceId}.json          # Service definition
├── openapi.json              # OpenAPI 3.0.3 spec
├── spec-bundle.json          # Integrated bundle
├── api/
│   └── {ControllerName}.json # Per-controller API definitions
├── model/
│   └── {ClassName}.json      # Model/DTO definitions
└── error/
    ├── errors.json           # Error code groups
    └── error-response.json   # Error response model structure
```

## Changelog

- **v2.1.7** — Fix List\<String\>/Set\<Long\> broken $ref, add missing java.lang types, skip static fields, treat java.time.* as normal type
- **v2.1.6** — Fix `hearders` typo → `headers`, add `isGlobal` flag, skip @XApiIgnore parameter types
- **v2.1.5** — JSON sample auto-generation (requestSample/responseSample in spec-bundle.json), @XSample annotation
- **v2.1.4** — Fix Set/Collection mapped as string → now correctly array + items
- **v2.1.3** — Fix query param enum model, add @Size(min=1) required, NPE guard
- **v2.1.2** — Fix missing schema for classes outside basePackage
- **v2.1.1** — `@XApiIgnore` annotation, `excludePackages`/`excludeClasses` DSL
- **v2.1.0** — 8 OpenAPI improvements (query param expansion, operationId, enum, required, ApiError, DateTime, BigDecimal, examples)
- **v2.0.7** — Auth DSL: Bearer/Basic support, OpenAPI securitySchemes
- **v2.0.6** — Fix empty version, path-less mapping bug
- **v2.0.5** — `errorResponseClass` DSL property
- **v2.0.4** — OpenAPI 3.0.3, spec-bundle.json, error code scanning
