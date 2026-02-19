package one.axim.gradle;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class RestApiDocGeneratorTest {

    private static RestApiDocGenerator instance;

    @BeforeAll
    static void setUp() throws Exception {
        // RestApiDocGenerator의 생성자는 Gradle ClassUtils에 의존하므로
        // Unsafe.allocateInstance로 생성자 없이 인스턴스를 만든다.
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        instance = (RestApiDocGenerator) unsafe.allocateInstance(RestApiDocGenerator.class);
    }

    // ========== getAllFields ==========

    @Test
    void testGetAllFields_includesInherited() throws Exception {
        Method getAllFields = RestApiDocGenerator.class.getDeclaredMethod("getAllFields", Class.class);
        getAllFields.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Field> fields = (List<Field>) getAllFields.invoke(instance, GrandChild.class);

        List<String> names = fields.stream().map(Field::getName).toList();
        assertEquals(3, names.size());
        assertTrue(names.contains("grandChildField"));
        assertTrue(names.contains("childField"));
        assertTrue(names.contains("parentField"));
    }

    @Test
    void testGetAllFields_excludesObjectFields() throws Exception {
        Method getAllFields = RestApiDocGenerator.class.getDeclaredMethod("getAllFields", Class.class);
        getAllFields.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Field> fields = (List<Field>) getAllFields.invoke(instance, Parent.class);

        // Object 클래스 필드(없음)가 포함되지 않아야 함
        List<String> names = fields.stream().map(Field::getName).toList();
        assertEquals(1, names.size());
        assertEquals("parentField", names.get(0));
    }

    @Test
    void testGetAllFields_childFieldFirst() throws Exception {
        Method getAllFields = RestApiDocGenerator.class.getDeclaredMethod("getAllFields", Class.class);
        getAllFields.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Field> fields = (List<Field>) getAllFields.invoke(instance, GrandChild.class);

        // 하위 클래스 필드가 먼저 오고, 부모 클래스 필드가 뒤에
        List<String> names = fields.stream().map(Field::getName).toList();
        assertEquals("grandChildField", names.get(0));
        assertEquals("childField", names.get(1));
        assertEquals("parentField", names.get(2));
    }

    // ========== extractInnerType ==========

    @Test
    void testExtractInnerType_list() throws Exception {
        Method extractInnerType = RestApiDocGenerator.class.getDeclaredMethod("extractInnerType", String.class);
        extractInnerType.setAccessible(true);

        String result = (String) extractInnerType.invoke(instance, "java.util.List<com.example.Dto>");
        assertEquals("com.example.Dto", result);
    }

    @Test
    void testExtractInnerType_noGeneric() throws Exception {
        Method extractInnerType = RestApiDocGenerator.class.getDeclaredMethod("extractInnerType", String.class);
        extractInnerType.setAccessible(true);

        String result = (String) extractInnerType.invoke(instance, "com.example.Dto");
        assertEquals("com.example.Dto", result);
    }

    @Test
    void testExtractInnerType_null() throws Exception {
        Method extractInnerType = RestApiDocGenerator.class.getDeclaredMethod("extractInnerType", String.class);
        extractInnerType.setAccessible(true);

        String result = (String) extractInnerType.invoke(instance, (String) null);
        assertNull(result);
    }

    @Test
    void testExtractInnerType_nested() throws Exception {
        Method extractInnerType = RestApiDocGenerator.class.getDeclaredMethod("extractInnerType", String.class);
        extractInnerType.setAccessible(true);

        String result = (String) extractInnerType.invoke(instance, "Wrapper<List<Dto>>");
        assertEquals("List<Dto>", result);
    }

    // ========== unwrapGenericType ==========

    @Test
    void testUnwrapGenericType_list_stopsAtList() throws Exception {
        Method unwrapGenericType = RestApiDocGenerator.class.getDeclaredMethod("unwrapGenericType", Type.class);
        unwrapGenericType.setAccessible(true);

        // GenericFixture.listReturn()의 제네릭 반환 타입: List<String>
        java.lang.reflect.Method listMethod = GenericFixture.class.getDeclaredMethod("listReturn");
        Type genericReturnType = listMethod.getGenericReturnType();

        Type result = (Type) unwrapGenericType.invoke(instance, genericReturnType);
        // List에서 멈춰야 함 — 결과는 ParameterizedType(List<String>)
        assertTrue(result.getTypeName().startsWith("java.util.List"));
    }

    @Test
    void testUnwrapGenericType_wrappedList() throws Exception {
        Method unwrapGenericType = RestApiDocGenerator.class.getDeclaredMethod("unwrapGenericType", Type.class);
        unwrapGenericType.setAccessible(true);

        // GenericFixture.wrappedListReturn()의 타입: Optional<List<String>>
        java.lang.reflect.Method wrappedMethod = GenericFixture.class.getDeclaredMethod("wrappedListReturn");
        Type genericReturnType = wrappedMethod.getGenericReturnType();

        Type result = (Type) unwrapGenericType.invoke(instance, genericReturnType);
        // Optional을 벗기고 List<String>에서 멈춰야 함
        assertTrue(result.getTypeName().startsWith("java.util.List"));
    }

    @Test
    void testUnwrapGenericType_plainClass() throws Exception {
        Method unwrapGenericType = RestApiDocGenerator.class.getDeclaredMethod("unwrapGenericType", Type.class);
        unwrapGenericType.setAccessible(true);

        Type result = (Type) unwrapGenericType.invoke(instance, String.class);
        // Class 타입은 그대로 반환
        assertEquals(String.class, result);
    }

    // ========== Test Fixtures ==========

    static class Parent {
        String parentField;
    }

    static class Child extends Parent {
        String childField;
    }

    static class GrandChild extends Child {
        String grandChildField;
    }

    @SuppressWarnings("unused")
    static class GenericFixture {
        List<String> listReturn() { return null; }
        Optional<List<String>> wrappedListReturn() { return null; }
    }
}
