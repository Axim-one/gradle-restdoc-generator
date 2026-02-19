package one.axim.gradle.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class JavaSourceParserTest {

    private static JavaSourceParser classParser;
    private static JavaSourceParser enumParser;

    @BeforeAll
    static void setUp() throws Exception {
        File classFile = new File(Objects.requireNonNull(
                JavaSourceParserTest.class.getClassLoader().getResource("fixtures/SampleClass.java")).toURI());
        File enumFile = new File(Objects.requireNonNull(
                JavaSourceParserTest.class.getClassLoader().getResource("fixtures/SampleEnum.java")).toURI());

        classParser = JavaSourceParser.parse(classFile);
        enumParser = JavaSourceParser.parse(enumFile);
    }

    @Test
    void testGetClassComment() {
        String comment = classParser.getClassComment();
        assertNotNull(comment);
        assertEquals("사용자 정보를 담는 클래스", comment);
    }

    @Test
    void testGetFieldComment() {
        assertEquals("사용자 이름", classParser.getFieldComment("name"));
        assertEquals("사용자 나이", classParser.getFieldComment("age"));
    }

    @Test
    void testGetFieldComment_lineComment_returnsNull() {
        // 라인 코멘트(// ...)는 Javadoc이 아니므로 null
        assertNull(classParser.getFieldComment("address"));
    }

    @Test
    void testGetFieldComment_notExists_returnsNull() {
        assertNull(classParser.getFieldComment("nonExistentField"));
    }

    @Test
    void testGetEnumFieldComment() {
        assertEquals("활성 상태", enumParser.getFieldComment("ACTIVE"));
        assertEquals("비활성 상태", enumParser.getFieldComment("INACTIVE"));
        assertNull(enumParser.getFieldComment("UNKNOWN"));
    }

    @Test
    void testGetMethodComment_description() throws Exception {
        // SampleClass.getUser(String, boolean) — 2 parameters
        Method method = findMethod("getUser", 2);
        MethodComment mc = classParser.getMethodComment(method);
        assertNotNull(mc);
        assertTrue(mc.getDescription().contains("사용자 정보를 조회한다."));
    }

    @Test
    void testGetMethodComment_tags() throws Exception {
        Method method = findMethod("getUser", 2);
        MethodComment mc = classParser.getMethodComment(method);
        assertNotNull(mc);

        List<CommentTag> tags = mc.getTags();
        assertFalse(tags.isEmpty());

        // param tags
        CommentTag paramUserId = findTag(tags, "param", "userId");
        assertNotNull(paramUserId, "param userId tag should exist");
        assertEquals("사용자 ID", paramUserId.getValue());

        CommentTag paramDetail = findTag(tags, "param", "includeDetail");
        assertNotNull(paramDetail, "param includeDetail tag should exist");
        assertEquals("상세 포함 여부", paramDetail.getValue());

        // return tag
        CommentTag returnTag = findTag(tags, "return", null);
        assertNotNull(returnTag, "return tag should exist");
        assertEquals("사용자 정보 객체", returnTag.getValue());

        // response tags
        CommentTag res200 = findTag(tags, "response", "200");
        assertNotNull(res200, "response 200 tag should exist");
        assertEquals("성공", res200.getValue());

        CommentTag res404 = findTag(tags, "response", "404");
        assertNotNull(res404, "response 404 tag should exist");
        assertEquals("사용자를 찾을 수 없음", res404.getValue());

        // group tag
        CommentTag group = findTag(tags, "group", null);
        assertNotNull(group, "group tag should exist");
        assertEquals("사용자 관리", group.getValue());

        // auth tag
        CommentTag auth = findTag(tags, "auth", null);
        assertNotNull(auth, "auth tag should exist");
        assertEquals("true", auth.getValue());

        // header tag
        CommentTag header = findTag(tags, "header", "X-Custom-Token");
        assertNotNull(header, "header tag should exist");
        assertEquals("커스텀 인증 토큰", header.getValue());
    }

    @Test
    void testGetMethodComment_noJavadoc_returnsNull() throws Exception {
        Method method = findMethod("noJavadocMethod", 0);
        MethodComment mc = classParser.getMethodComment(method);
        assertNull(mc);
    }

    // --- helpers ---

    /**
     * Stub method lookup: creates a "fake" Method that matches by name & param count.
     * JavaSourceParser.getMethodDeclaration matches on name + parameter count.
     */
    private static Method findMethod(String name, int paramCount) throws Exception {
        // We need a real Method object whose getName() and getParameterCount() match.
        // Use a helper inner class to provide matching signatures.
        for (Method m : SampleMethodHolder.class.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == paramCount) {
                return m;
            }
        }
        throw new NoSuchMethodException(name + " with " + paramCount + " params");
    }

    private static CommentTag findTag(List<CommentTag> tags, String tagName, String name) {
        for (CommentTag tag : tags) {
            if (tagName.equals(tag.getTag())) {
                if (name == null || name.equals(tag.getName())) {
                    return tag;
                }
            }
        }
        return null;
    }

    /**
     * Mirror of fixture method signatures so we can obtain real Method objects.
     */
    @SuppressWarnings("unused")
    private static class SampleMethodHolder {
        public Object getUser(String userId, boolean includeDetail) { return null; }
        public void noJavadocMethod() {}
    }
}
