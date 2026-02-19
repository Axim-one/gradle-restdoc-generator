# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A **Gradle plugin** (`gradle-restdoc-generator`) that automatically generates REST API documentation from Spring Boot `@RestController` classes. It scans annotated controllers via reflection, produces JSON metadata (API definitions + model definitions), and can:

- Deploy documentation to a remote API doc server
- Sync collections/environments to **Postman** via their API

## Build Commands

```bash
./gradlew build          # Build and run tests
./gradlew test           # Run tests only (JUnit 5)
./gradlew publishToMavenLocal  # Publish to local Maven repo for testing
```

## Architecture

### Plugin Entry Point

`DocumentPlugin` (implements `Plugin<Project>`) registers the `restMetaGenerator` Gradle task, which depends on `compileJava`.

### Task Configuration (`RestMetaGeneratorTask`)

The `restMetaGenerator` task accepts these DSL properties:

**Core:**
- `documentPath`, `basePackage` — where to output docs and which packages to scan
- `postmanApiKey`, `postmanWorkSpaceId` — Postman integration
- `debug` — flag

**Service info (replaces former `x_service_info.json`):**
- `serviceName` — display name of the service
- `apiServerUrl` — base URL for the API server
- `serviceVersion` — API version prefix (default `v1.0`)
- `introductionFile` — path to a markdown file whose content becomes the service introduction

**Error code/response (two-tier resolution: DSL override → framework default):**
- `errorCodeClass` — FQCN of the ErrorCode class to scan (default: `one.axim.framework.rest.exception.ErrorCode`)
- `errorResponseClass` — FQCN of the error response DTO (default: `one.axim.framework.rest.model.ApiError`) *(v2.0.5+)*

**Nested DSL methods:**
- `auth { type, headerKey, value, descriptionFile }` — authentication configuration
- `header(name, defaultValue, description)` — add a common header
- `environment(name) { variable(key, value) }` — Postman environment variables

### Pipeline (executed in `RestMetaGeneratorTask.deplyTask()`)

1. **Error Code Scan** — `ErrorCodeScanner.scanAndReturn()` scans exception classes for `ErrorCode` fields and returns `List<ErrorGroupDefinition>` (no file I/O). This runs **before** API generation so error groups are available for per-API linking.

2. **Scan & Generate JSON** — `RestApiDocGenerator` uses Guava `ClassPath` + reflection to find `@RestController` classes, then parses their Java source files with `JavaSourceParser` (backed by `javaparser`) to extract Javadoc comments, parameter names, and type info. During generation, `@error`/`@throws` tags and method `throws` clauses are matched against the error group map to populate `APIDefinition.errors` and `responseStatus`. Outputs per-controller API JSON and per-model JSON under `documentPath/api/` and `documentPath/model/`.

3. **Error Code Write** — `ErrorCodeScanner.writeResults()` writes `errors.json` and `error-response.json` under `documentPath/error/`.

4. **OpenAPI / SpecBundle / Postman sync** — `OpenApiSpecConverter`, `SpecBundleGenerator`, and `PostmanSpecConverter` consume the generated JSON to produce OpenAPI 3.0 spec, bundled JSON, and Postman collections.

### Key Packages

- `one.axim.gradle` — Plugin, task, and top-level generators (`ErrorCodeScanner` exposes `scanAndReturn()` / `writeResults()` split for two-phase execution)
- `one.axim.gradle.data` — Domain data classes (`APIDefinition`, `APIParameter`, `ServiceDefinition`, etc.)
- `one.axim.gradle.generator` — `ModelGenerator` (builds model definitions for Postman markdown), `LanguageType` enum
- `one.axim.gradle.generator.data` — Data models (`FieldData`, `ModelData`, `ModelDefinition`, etc.)
- `one.axim.gradle.generator.utils` — `TypeMapUtils` for type mapping
- `one.axim.gradle.postman.data` — Postman API data structures
- `one.axim.gradle.utils` — `ClassUtils` (classloader/source resolution), `JavaSourceParser` (Javadoc extraction), `NamingConvert`, `AnnotationHelper`, `XPageSchema`, etc.

### Service Definition

Service metadata is configured directly in the `restMetaGenerator` Gradle DSL block (properties like `serviceName`, `apiServerUrl`, `serviceVersion`, `auth {}`, `header()`, `environment() {}`). Rich-content fields such as introduction and auth description support markdown files via `introductionFile` and `auth { descriptionFile }`. The DSL helper classes are in `one.axim.gradle.dsl`.

### FreeMarker Templates (`src/main/resources/template/`)

- `postmanBodyMarkdown.template` — Postman request/response body description markdown
- `postmanParameterMarkdown.template` — Postman parameter description markdown

### Dependencies on Internal Framework (runtime only)

The plugin references `one.axim.framework` annotations and types via string-based checks at runtime (no compile-time dependency). These include:
- `@XRestGroupName`, `@XPageNationDefault`, `@XQueryParams` — annotations
- `XPage`, `XPageNation`, `XOrder` — data types (mirrored by `XPageSchema` for reflection)

## Javadoc Comment Tags

The generator extracts custom Javadoc tags from controller methods:
- `@param` — parameter descriptions
- `@return` — return value description
- `@response {statusCode} {description}` — HTTP response status documentation
- `@error {ExceptionClassName}` — links an error group to this API (populates `errors` field and `responseStatus`)
- `@throws {ExceptionClassName}` — standard Javadoc tag, also links error group (same as `@error`)
- `@group` — API grouping name
- `@auth true` — marks endpoint as requiring authentication
- `@header {name} {description}` — custom header documentation
- `@className` — override for generated class name

Additionally, method `throws` clauses in the Java signature are **auto-detected** via reflection and matched against scanned error groups — no Javadoc tag needed.
