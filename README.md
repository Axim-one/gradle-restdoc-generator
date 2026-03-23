# gradle-restdoc-generator

[![](https://jitpack.io/v/Axim-one/gradle-restdoc-generator.svg)](https://jitpack.io/#Axim-one/gradle-restdoc-generator)

Spring Boot `@RestController`에서 Javadoc 기반으로 REST API 문서를 자동 생성하는 Gradle 플러그인입니다.

## Features

- `@RestController` 클래스를 스캔하여 API 메타데이터(JSON) 자동 생성
- Javadoc 커스텀 태그(`@response`, `@group`, `@auth`, `@header`, `@error`, `@throws`)로 풍부한 문서화
- Spring `Pageable` 파라미터 자동 인식 → `page`, `size`, `sort` 쿼리 파라미터 생성
- Spring `Page<T>` 리턴 타입 자동 인식 → 페이지네이션 응답 모델 생성
- `ApiResult<T>` 래퍼 타입 자동 언래핑 (단건, List, Page 모두 지원)
- **에러 코드 스캐닝** — Exception 클래스의 ErrorCode 필드를 자동 수집하여 문서화
- **에러 응답 모델** — Error Response DTO 구조를 자동 생성 (커스텀 클래스 지정 가능)
- **복합 쿼리 파라미터 자동 전개** — `@RequestParam` 없이 바인딩되는 DTO 객체를 개별 쿼리 파라미터로 분해
- **의미있는 operationId** — 컨트롤러 메서드명 기반 (코드 생성기 호환)
- **enum 값 자동 추출** — enum 필드에 `"enum": ["ACTIVE", "SUSPENDED", ...]` 배열 자동 생성
- **required 필드 자동 추출** — `@NotNull`, `@NotBlank`, `@NotEmpty`, primitive 타입 감지
- **에러 응답 스키마 자동 첨부** — 4xx/5xx 응답에 `ApiError` 스키마 참조 자동 추가
- **날짜/시간 인라인 처리** — `LocalDateTime` → `date-time`, `LocalDate` → `date`, `LocalTime` → `time`
- **타입 기반 example 자동 생성** — Swagger UI "Try it out" 시 기본값 제공
- **API 제외** — `@XApiIgnore` 어노테이션 또는 DSL(`excludePackages`, `excludeClasses`)로 문서 생성 제외
- OpenAPI 3.0.3 스펙 (`openapi.json`) 자동 생성
- 번들 JSON (`spec-bundle.json`) 생성 — API 문서 UI 연동용
- Postman Collection v2.1 자동 동기화 (기존 값 merge 지원)
- Postman Environment 변수 관리

## Installation

### build.gradle

```groovy
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'com.github.Axim-one:gradle-restdoc-generator:2.1.3'
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
    id 'gradle-restdoc-generator' version '2.1.3'
}
```

## Configuration

```groovy
restMetaGenerator {
    // 필수
    documentPath = 'build/docs'
    basePackage = 'com.example'
    serviceId = 'my-service'

    // 서비스 정보
    serviceName = 'My Service'
    apiServerUrl = 'https://api.example.com'
    serviceVersion = 'v1.0'
    introductionFile = 'docs/introduction.md'    // 마크다운 파일 경로

    // 에러 코드 / 에러 응답 클래스
    errorCodeClass = 'com.example.exception.ErrorCode'         // ErrorCode 클래스 FQCN
    errorResponseClass = 'com.example.dto.ApiErrorResponse'    // Error Response DTO FQCN (v2.0.5+)

    // 인증 설정 — API Key (type='token'도 하위 호환)
    auth {
        type = 'apiKey'                           // 또는 'token' (하위 호환 alias)
        headerKey = 'Access-Token'
        value = '{{accessToken}}'
        in = 'header'                             // 'header' (기본값), 'query', 'cookie'
        descriptionFile = 'docs/auth.md'          // 인증 설명 마크다운
    }

    // 인증 설정 — Bearer (v2.1.0+)
    // auth {
    //     type = 'http'
    //     scheme = 'bearer'
    //     bearerFormat = 'JWT'                    // 선택사항
    //     value = '{{accessToken}}'
    //     descriptionFile = 'docs/auth.md'
    // }

    // 인증 설정 — Basic (v2.1.0+)
    // auth {
    //     type = 'http'
    //     scheme = 'basic'
    //     value = '{{username}}:{{password}}'
    //     descriptionFile = 'docs/auth.md'
    // }

    // 공통 헤더
    header('X-Custom-Header', 'default-value', 'Header description')

    // Postman 환경변수
    environment('DEV') {
        variable('base_url', 'https://dev.api.example.com')
        variable('token', 'dev-token')
    }
    environment('PROD') {
        variable('base_url', 'https://api.example.com')
        variable('token', '')
    }

    // Postman 동기화
    postmanApiKey = ''           // Postman API Key
    postmanWorkSpaceId = ''      // Postman Workspace ID

    debug = false
}
```

### DSL 프로퍼티 레퍼런스

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `documentPath` | **Yes** | — | 문서 출력 디렉토리 (상대 경로는 프로젝트 기준) |
| `basePackage` | **Yes** | — | 스캔 대상 패키지 (쉼표로 복수 지정 가능) |
| `serviceId` | **Yes** | — | 서비스 고유 ID (JSON 파일명으로 사용) |
| `serviceName` | No | `""` | 서비스 표시명 (OpenAPI title) |
| `apiServerUrl` | No | `""` | API 서버 기본 URL (OpenAPI servers) |
| `serviceVersion` | No | `"v1.0"` | API 버전 (OpenAPI version). null 또는 빈 값이면 URL에 버전 prefix를 추가하지 않음 |
| `introductionFile` | No | `""` | 서비스 소개 마크다운 파일 경로 |
| `errorCodeClass` | No | `""` | ErrorCode 클래스 FQCN (미지정 시 프레임워크 기본값) |
| `errorResponseClass` | No | `""` | Error Response DTO FQCN (미지정 시 프레임워크 기본값) |
| `postmanApiKey` | No | `""` | Postman API Key (비어있으면 Postman 동기화 스킵) |
| `postmanWorkSpaceId` | No | `""` | Postman Workspace ID |
| `excludePackages` | No | `[]` | 문서 생성에서 제외할 패키지 목록 (v2.1.3+) |
| `excludeClasses` | No | `[]` | 문서 생성에서 제외할 컨트롤러 클래스명 목록 (simple name, v2.1.3+) |
| `debug` | **Yes** | `false` | 디버그 로깅 활성화 |

### Auth DSL 프로퍼티 (v2.1.0+)

| Property | Default | Description |
|----------|---------|-------------|
| `type` | `""` | 인증 타입: `"token"`/`"apiKey"` (API Key) 또는 `"http"` (Bearer/Basic) |
| `headerKey` | `""` | 헤더/파라미터 이름 (apiKey 타입 전용) |
| `value` | `""` | 인증 값 (토큰 값, Basic의 경우 `"user:pass"`) |
| `descriptionFile` | `""` | 인증 설명 마크다운 파일 경로 |
| `in` | `"header"` | apiKey 위치: `"header"`, `"query"`, `"cookie"` |
| `scheme` | `""` | HTTP 인증 스킴: `"bearer"`, `"basic"` (http 타입 전용) |
| `bearerFormat` | `""` | 토큰 포맷 힌트, 예: `"JWT"` (bearer 전용) |

**OpenAPI 3.0 securitySchemes 매핑:**
- `type='apiKey'` → `{ type: "apiKey", in: "header", name: "Access-Token" }`
- `type='http'`, `scheme='bearer'` → `{ type: "http", scheme: "bearer", bearerFormat: "JWT" }`
- `type='http'`, `scheme='basic'` → `{ type: "http", scheme: "basic" }`

**Postman 인증 타입 매핑:**
- `apiKey`/`token` → Postman `"apikey"`
- `http` + `bearer` → Postman `"bearer"`
- `http` + `basic` → Postman `"basic"`

## Javadoc Tags

컨트롤러 메서드에 다음 Javadoc 태그를 사용하여 문서를 보강할 수 있습니다:

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

| Tag | Description |
|-----|-------------|
| `@param` | 파라미터 설명 |
| `@return` | 반환값 설명 |
| `@response {code} {desc}` | HTTP 응답 상태 코드와 설명 |
| `@error {ExceptionClass}` | 에러 그룹 연결 — errors 필드와 responseStatus에 자동 반영 |
| `@throws {ExceptionClass}` | `@error`와 동일 (표준 Javadoc 태그) |
| `@group` | API 그룹 이름 (Postman 폴더로 매핑) |
| `@auth true` | 인증이 필요한 엔드포인트 표시 |
| `@header {name} {desc}` | 커스텀 헤더 문서화 |
| `@className` | 생성되는 클래스명 오버라이드 |

> **Note:** 메서드 시그니처의 `throws` 절도 자동 감지되어 에러 그룹에 연결됩니다 (Javadoc 태그 불필요).

## Error Code Documentation

Exception 클래스에 `public static final ErrorCode` 필드가 있으면 자동으로 스캐닝됩니다:

```java
public class UserNotFoundException extends RuntimeException {
    public static final ErrorCode USER_NOT_FOUND =
        new ErrorCode("USER_001", "error.user.notfound");
    public static final ErrorCode USER_DELETED =
        new ErrorCode("USER_002", "error.user.deleted");

    public UserNotFoundException(ErrorCode errorCode) {
        super(HttpStatus.NOT_FOUND, errorCode);
    }
}
```

### 에러 코드/응답 클래스 지정

`errorCodeClass`와 `errorResponseClass`는 모두 **2단계 우선순위**를 따릅니다:

1. **DSL에서 명시적 지정** — 프레임워크 없이 자체 클래스를 사용하는 프로젝트
2. **프레임워크 기본값** — `one.axim.framework.rest.exception.ErrorCode` / `one.axim.framework.rest.model.ApiError`

```groovy
restMetaGenerator {
    // 자체 ErrorCode 클래스 사용
    errorCodeClass = 'com.example.exception.ErrorCode'

    // 자체 Error Response DTO 사용 (v2.0.5+)
    errorResponseClass = 'com.example.dto.ApiErrorResponse'
}
```

Error Response DTO의 필드 구조가 자동으로 `error/error-response.json`에 출력됩니다:

```java
// errorResponseClass로 지정한 클래스
public class ApiErrorResponse {
    private int status;
    private String code;
    private String message;
}
```

생성되는 JSON:
```json
{
  "name": "ApiErrorResponse",
  "type": "Object",
  "fields": [
    { "name": "status", "type": "int" },
    { "name": "code", "type": "String" },
    { "name": "message", "type": "String" }
  ]
}
```

### API와 에러 그룹 연결

`@error` / `@throws` Javadoc 태그 또는 메서드 `throws` 절로 에러 그룹을 API에 연결합니다:

```java
/**
 * 사용자 상세 조회
 * @error UserNotFoundException
 */
@GetMapping("/users/{id}")
public UserDto getUser(@PathVariable Long id) { ... }

// 또는 throws 절로 자동 감지
@GetMapping("/users/{id}/status")
public UserStatus getUserStatus(@PathVariable Long id) throws AuthException { ... }
```

연결된 에러 그룹은 API JSON의 `errors` 필드와 `responseStatus`에 자동 반영됩니다.

## API 제외 (v2.1.3+)

### DSL 기반 제외

패키지 또는 클래스 단위로 문서 생성에서 제외할 수 있습니다:

```groovy
restMetaGenerator {
    excludePackages = ['com.example.internal', 'com.example.admin']
    excludeClasses = ['HealthCheckController', 'ActuatorController']
}
```

### 어노테이션 기반 제외

`@XApiIgnore`를 컨트롤러 클래스 또는 개별 메서드에 사용하여 세밀하게 제어할 수 있습니다:

```java
// 컨트롤러 전체 제외
@XApiIgnore
@RestController
public class InternalController { ... }

// 특정 메서드만 제외
@RestController
public class UserController {

    @GetMapping(name = "사용자 조회", value = "/{id}")
    public UserDto getUser(@PathVariable Long id) { ... }

    @XApiIgnore
    @GetMapping(name = "디버그", value = "/debug")
    public String debug() { ... }
}
```

> **Note:** `@XApiIgnore` 어노테이션은 플러그인 적용 시 자동으로 프로젝트 클래스패스에 추가됩니다. 별도 의존성 설정이 필요 없습니다.

## Query Parameter Object 지원 (v2.1.0+)

복합 객체를 쿼리 파라미터로 바인딩하면 자동으로 개별 파라미터로 전개됩니다:

```java
public class UserSearchRequest {
    private String name;
    @NotNull
    private UserStatus status;    // enum → 드롭다운
    private Long networkId;
    private boolean active;       // primitive → required
}

@GetMapping(name = "사용자 검색", value = "/search")
public List<UserDto> searchUsers(UserSearchRequest search) { ... }
```

생성되는 OpenAPI 쿼리 파라미터:
- `name` — `type: string`, optional
- `status` — `type: string, enum: [ACTIVE, SUSPENDED, ...]`, required (`@NotNull`)
- `networkId` — `type: integer, format: int64`, optional
- `active` — `type: boolean`, required (primitive)

> **Note:** `@XQueryParams` 어노테이션 없이도 복합 객체가 자동 전개됩니다. `@RequestBody`나 `@PathVariable`이 아닌 Object 타입 파라미터가 대상입니다.

## Pagination Support

Spring Data의 `Pageable`/`Page<T>`를 자동으로 인식합니다:

```java
@GetMapping(name = "사용자 페이징 조회", value = "/paged")
public Page<UserDto> getUsers(Pageable pageable) { ... }
```

자동 생성되는 내용:
- **쿼리 파라미터**: `page` (default: 0), `size` (default: 20), `sort`
- **응답 모델**: `content`, `totalElements`, `totalPages`, `size`, `number`, `numberOfElements`, `first`, `last`, `empty`, `sort`
- **API JSON**: `isPaging: true`, `pagingType: "spring"`

## ApiResult Wrapper 지원

`ApiResult<T>` 래퍼 타입을 자동으로 언래핑합니다:

```java
// 단건 → returnClass: "UserDto"
public ApiResult<UserDto> getUser() { ... }

// 리스트 → returnClass: "UserDto", isArrayReturn: true
public ApiResult<List<UserDto>> getUsers() { ... }

// 페이징 → returnClass: "UserDto", isPaging: true, pagingType: "spring"
public ApiResult<Page<UserDto>> getUsersPaged(Pageable pageable) { ... }
```

## Usage

```bash
./gradlew restMetaGenerator
```

실행하면 `documentPath` 디렉토리에 다음 파일들이 생성됩니다:

```
build/docs/
├── {serviceId}.json          # 서비스 정의
├── openapi.json              # OpenAPI 3.0.3 스펙
├── spec-bundle.json          # 통합 번들 (서비스 + API + 모델 + 에러)
├── api/
│   └── {ControllerName}.json # 컨트롤러별 API 정의
├── model/
│   └── {ClassName}.json      # 모델(DTO) 정의
└── error/
    ├── errors.json           # 에러 코드 그룹 목록
    └── error-response.json   # 에러 응답 모델 구조
```

## OpenAPI & Spec Bundle

`restMetaGenerator` 실행 시 별도 설정 없이 자동 생성됩니다:

- **`openapi.json`** — OpenAPI 3.0.3 표준 스펙. Swagger UI, Redoc 등 외부 도구와 호환.
- **`spec-bundle.json`** — 서비스 정보 + 전체 API + 전체 모델 + 에러를 하나의 파일로 합친 번들. API 문서 UI에서 단일 HTTP 요청으로 로드할 수 있도록 설계.

```json
// spec-bundle.json 구조
{
  "service": {
    "serviceId": "my-service",
    "name": "My Service",
    "apiServerUrl": "https://api.example.com",
    "version": "v1.0",
    "introduction": "...",
    "auth": { "type": "apiKey", "headerKey": "Access-Token", "in": "header", ... },
    "headers": [...]
  },
  "apis": [
    {
      "id": "getUser",
      "name": "사용자 조회",
      "method": "GET",
      "urlMapping": "/users/{id}",
      "returnClass": "com.example.dto.UserDto",
      "parameters": [...],
      "responseStatus": { "200": "성공", "404": "사용자를 찾을 수 없음" },
      "errors": [{ "exception": "UserNotFoundException", "status": 404, "codes": [...] }]
    }
  ],
  "models": {
    "com.example.dto.UserDto": {
      "name": "UserDto",
      "type": "Object",
      "fields": [{ "name": "id", "type": "Long" }, { "name": "name", "type": "String" }]
    }
  },
  "errors": [
    {
      "group": "User Not Found",
      "exception": "UserNotFoundException",
      "status": 404,
      "codes": [{ "code": "USER_001", "name": "USER_NOT_FOUND", "message": "사용자를 찾을 수 없습니다" }]
    }
  ],
  "errorResponse": {
    "name": "ApiErrorResponse",
    "type": "Object",
    "fields": [{ "name": "status", "type": "int" }, { "name": "code", "type": "String" }, { "name": "message", "type": "String" }]
  }
}
```

## Execution Pipeline

`restMetaGenerator` 태스크의 실행 순서:

1. **Error Code Scan** — Exception 클래스에서 ErrorCode 필드 수집 (`ErrorCodeScanner.scanAndReturn()`)
2. **API Document Generation** — `@RestController` 스캔 → 컨트롤러별/모델별 JSON 생성 (`RestApiDocGenerator`)
3. **Error JSON Write** — `errors.json` + `error-response.json` 출력 (`ErrorCodeScanner.writeResults()`)
4. **OpenAPI Spec** — `openapi.json` 생성 (`OpenApiSpecConverter`)
5. **Spec Bundle** — `spec-bundle.json` 생성 (`SpecBundleGenerator`)
6. **Postman Sync** — Postman Collection/Environment 동기화 (설정된 경우)

## Context7 — AI 코딩 도구 연동

이 프로젝트는 [Context7](https://context7.com)에 등록되어 있어, AI 코딩 도구(Claude Code, Cursor, Copilot 등)에서 최신 문서를 자동으로 참조할 수 있습니다.

[![Context7](https://img.shields.io/badge/Context7-Available-brightgreen)](https://context7.com/axim-one/gradle-restdoc-generator)

### MCP 서버 연동

AI 코딩 도구에서 Context7 MCP 서버를 설정하면, 이 플러그인의 사용법을 자동으로 제공받을 수 있습니다:

```bash
# Claude Code에서 Context7 MCP 추가
claude mcp add context7 https://mcp.context7.com/mcp
```

### API로 문서 조회

```bash
# 라이브러리 검색
curl "https://context7.com/api/v2/libs/search?libraryName=gradle-restdoc-generator"

# 문서 조회
curl "https://context7.com/api/v2/context?libraryId=/axim-one/gradle-restdoc-generator&topic=configuration"
```

### AI 에이전트에서 사용 예시

```
# resolve-library-id로 라이브러리 ID 조회
resolve-library-id("gradle-restdoc-generator")
→ /axim-one/gradle-restdoc-generator

# get-library-docs로 문서 가져오기
get-library-docs("/axim-one/gradle-restdoc-generator", topic="error code")
```

## Requirements

- Java 17+
- Gradle 7.0+
- Spring Boot (`@RestController`, `@RequestMapping` 등)

## Changelog

### v2.1.3
- 복합 쿼리 파라미터 DTO의 enum 필드 모델이 생성되지 않던 문제 수정 — 이제 enum 드롭다운이 정상 표시
- `@Size(min=1)` 어노테이션 → required 필드 자동 감지 추가
- 외부 모듈 클래스를 쿼리 파라미터 DTO로 사용 시 소스 파일 없어도 NPE 없이 정상 처리

### v2.1.2
- `basePackage` 외부의 참조 클래스(Entity/DTO) 모델 생성 누락 수정 — 다른 모듈의 클래스를 반환해도 `$ref`가 깨지지 않음
- 모델 생성 시 JDK/프레임워크 클래스만 제외하고, 사용자 클래스는 패키지 무관하게 schema 생성

### v2.1.1
- `@XApiIgnore` 어노테이션 추가 — 컨트롤러 클래스 또는 메서드 단위로 문서 생성 제외
- `excludePackages` / `excludeClasses` DSL 프로퍼티 추가 — 패키지/클래스 단위 일괄 제외
- 플러그인 적용 시 `@XApiIgnore` 어노테이션을 프로젝트 클래스패스에 자동 추가

### v2.1.0
- `@RequestParam` 복합 객체를 개별 쿼리 파라미터로 자동 전개
- operationId를 컨트롤러 메서드명으로 생성 (MD5 해시 → `getUser`, `searchUsers` 등)
- enum 필드 감지 강화 및 `"enum"` 배열 자동 생성
- required 필드 자동 추출: `@NotNull`, `@NotBlank`, `@NotEmpty`, primitive 타입
- 4xx/5xx 에러 응답에 `ApiError` 스키마 자동 첨부 + components/schemas 등록
- `LocalDateTime` → `date-time`, `LocalDate` → `date`, `LocalTime` → `time` 인라인 변환
- `BigDecimal` 필드에 `"format": "decimal"` 추가
- 타입 기반 example 값 자동 생성 (Long→1, Boolean→true, Enum→첫 번째 값, BigDecimal→"100.00")

### v2.0.7
- Auth DSL 확장: OpenAPI 3.0 securitySchemes 완전 지원
  - `apiKey` 타입: `in` 프로퍼티 추가 (`"header"`, `"query"`, `"cookie"`)
  - `http` 타입: `scheme` (`"bearer"`, `"basic"`), `bearerFormat` 프로퍼티 추가
  - `type='token'`은 `'apiKey'`로 자동 정규화 (하위 호환)
- Postman 인증 타입 매핑: Bearer (`"bearer"`), Basic (`"basic"`) 지원
- OpenAPI securitySchemes에서 `"token"` 하드코딩 제거, 동적 스킴 이름 생성

### v2.0.6
- `serviceVersion`이 null 또는 빈 값일 때 버전 prefix를 생략하도록 수정 (`//path` → `/path`)
- `@GetMapping` 등에 path를 지정하지 않은 메서드에서 `ArrayIndexOutOfBoundsException` 발생하던 버그 수정

### v2.0.5
- `errorResponseClass` DSL 프로퍼티 추가 — Error Response 모델 클래스를 명시적으로 지정 가능
- 미지정 시 프레임워크 기본값(`ApiError`) 자동 폴백 (하위호환)
- Javadoc 전면 보강

### v2.0.4
- OpenAPI 3.0.3 스펙 자동 생성
- `spec-bundle.json` 통합 번들 생성
- 에러 코드 스캐닝 및 `@error`/`@throws` 태그 지원

### v2.0.3
- Inner class enum ClassNotFoundException 수정
- 메서드별 예외 격리

### v2.0.2
- Spring `Pageable`/`Page<T>` 자동 인식
- `ApiResult<T>` 래퍼 언래핑

## License

MIT
