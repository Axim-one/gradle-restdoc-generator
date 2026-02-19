package one.axim.gradle.test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RestMetaGeneratorIntegrationTest {

    private static final Gson gson = new Gson();
    private static Path tempDir;
    private static BuildResult buildResult;

    @BeforeAll
    static void setUp() throws Exception {
        // 테스트 프로젝트를 임시 디렉토리로 복사
        tempDir = Files.createTempDirectory("restdoc-test-");
        Path source = Paths.get(RestMetaGeneratorIntegrationTest.class.getClassLoader()
                .getResource("test-project").toURI());
        copyDirectory(source, tempDir);

        // restMetaGenerator 태스크 실행
        buildResult = GradleRunner.create()
                .withProjectDir(tempDir.toFile())
                .withArguments("restMetaGenerator", "--stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .build();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (tempDir != null) {
            deleteDirectory(tempDir);
        }
    }

    @Test
    void testTaskSucceeds() {
        assertEquals(TaskOutcome.SUCCESS,
                buildResult.task(":restMetaGenerator").getOutcome());
    }

    @Test
    void testApiJsonGenerated() {
        Path apiDir = tempDir.resolve("build/docs/api");
        assertTrue(Files.isDirectory(apiDir), "api directory should exist");

        File[] jsonFiles = apiDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        assertNotNull(jsonFiles);
        assertTrue(jsonFiles.length > 0, "at least one API JSON file should be generated");

        // SampleController에 대한 JSON 파일 확인
        boolean found = false;
        for (File f : jsonFiles) {
            if (f.getName().contains("SampleController")) {
                found = true;
                break;
            }
        }
        assertTrue(found, "SampleController API JSON should exist");
    }

    @Test
    void testModelJsonGenerated() {
        Path modelDir = tempDir.resolve("build/docs/model");
        assertTrue(Files.isDirectory(modelDir), "model directory should exist");

        File[] jsonFiles = modelDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        assertNotNull(jsonFiles);
        assertTrue(jsonFiles.length > 0, "at least one model JSON file should be generated");
    }

    @Test
    void testInheritedFieldsInModel() throws Exception {
        Path modelDir = tempDir.resolve("build/docs/model");
        File userDtoJson = findFile(modelDir, "UserDto");
        assertNotNull(userDtoJson, "UserDto model JSON should exist");

        String json = Files.readString(userDtoJson.toPath());
        Map<String, Object> model = gson.fromJson(json, Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) model.get("fields");
        assertNotNull(fields, "fields should not be null");

        List<String> fieldNames = fields.stream()
                .map(f -> (String) f.get("name"))
                .toList();

        // UserDto 자체 필드
        assertTrue(fieldNames.contains("name"), "should contain UserDto.name");
        // BaseDto (부모) 상속 필드
        assertTrue(fieldNames.contains("id"), "should contain inherited BaseDto.id");
        assertTrue(fieldNames.contains("createdAt"), "should contain inherited BaseDto.createdAt");
    }

    @Test
    void testEnumModelGenerated() throws Exception {
        Path modelDir = tempDir.resolve("build/docs/model");
        File enumJson = findFile(modelDir, "UserStatus");
        assertNotNull(enumJson, "UserStatus enum model JSON should exist");

        String json = Files.readString(enumJson.toPath());
        Map<String, Object> model = gson.fromJson(json, Map.class);

        assertEquals("Enum", model.get("type"), "UserStatus type should be 'Enum'");
    }

    @Test
    void testServiceJsonGenerated() {
        Path docsDir = tempDir.resolve("build/docs");
        File[] jsonFiles = docsDir.toFile().listFiles(
                (dir, name) -> name.endsWith(".json"));
        assertNotNull(jsonFiles);

        boolean found = false;
        for (File f : jsonFiles) {
            if (f.getName().contains("test-service")) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Service JSON (test-service.json) should exist in docs root");
    }

    // --- helpers ---

    private static File findFile(Path dir, String nameContains) {
        File[] files = dir.toFile().listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.getName().contains(nameContains) && f.getName().endsWith(".json")) {
                return f;
            }
        }
        return null;
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
