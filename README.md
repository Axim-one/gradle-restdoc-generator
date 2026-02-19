# gradle-restdoc-generator

[![](https://jitpack.io/v/Axim-one/gradle-restdoc-generator.svg)](https://jitpack.io/#Axim-one/gradle-restdoc-generator)

Spring Boot `@RestController`에서 Javadoc 기반으로 REST API 문서를 자동 생성하는 Gradle 플러그인입니다.

## Features

- `@RestController` 클래스를 스캔하여 API 메타데이터(JSON) 자동 생성
- Javadoc 커스텀 태그(`@response`, `@group`, `@auth`, `@header`)로 풍부한 문서화
- Postman Collection v2.1 자동 동기화 (기존 값 merge 지원)
- Postman Environment 변수 관리
- API Doc 서버 배포 지원

## Installation

### build.gradle

```groovy
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'com.github.Axim-one:gradle-restdoc-generator:2.0.1'
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
    id 'gradle-restdoc-generator' version '2.0.1'
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

    // 인증 설정
    auth {
        type = 'token'
        headerKey = 'Authorization'
        value = 'Bearer {{token}}'
        descriptionFile = 'docs/auth.md'         // 인증 설명 마크다운
    }

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

    // API Doc 서버 배포
    deploy = false
    serverUrl = ''

    debug = false
}
```

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
 */
@GetMapping("/users/{userId}")
public UserDto getUser(@PathVariable Long userId) { ... }
```

| Tag | Description |
|-----|-------------|
| `@param` | 파라미터 설명 |
| `@return` | 반환값 설명 |
| `@response {code} {desc}` | HTTP 응답 상태 코드와 설명 |
| `@group` | API 그룹 이름 (Postman 폴더로 매핑) |
| `@auth true` | 인증이 필요한 엔드포인트 표시 |
| `@header {name} {desc}` | 커스텀 헤더 문서화 |
| `@className` | 생성되는 클래스명 오버라이드 |

## Usage

```bash
./gradlew restMetaGenerator
```

실행하면 `documentPath` 디렉토리에 다음 파일들이 생성됩니다:

```
build/docs/
├── {serviceId}.json          # 서비스 정의
├── api/
│   └── {ControllerName}.json # 컨트롤러별 API 정의
└── model/
    └── {ClassName}.json      # 모델(DTO) 정의
```

## Requirements

- Java 17+
- Gradle 7.0+
- Spring Boot (`@RestController`, `@RequestMapping` 등)

## License

MIT
