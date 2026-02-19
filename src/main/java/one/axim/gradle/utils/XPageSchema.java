package one.axim.gradle.utils;

import java.util.List;

/**
 * Schema mirror of framework's XPage class.
 * Provides field metadata (name, type) for code generation and Postman spec conversion
 * without requiring the framework-core compile-time dependency.
 */
public class XPageSchema {
    public int page;
    public int size;
    public int offset;
    public boolean hasNext;
    public long totalCount;
    public String sort;
    public List orders;
    public List pageRows;
}
