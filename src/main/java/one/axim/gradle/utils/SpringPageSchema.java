package one.axim.gradle.utils;

import java.util.List;

/**
 * Schema mirror of Spring Data's Page interface.
 * Provides field metadata (name, type) for code generation and Postman spec conversion
 * without requiring the spring-data compile-time dependency.
 */
public class SpringPageSchema {
    public List content;
    public long totalElements;
    public int totalPages;
    public int size;
    public int number;
    public int numberOfElements;
    public boolean first;
    public boolean last;
    public boolean empty;
}
