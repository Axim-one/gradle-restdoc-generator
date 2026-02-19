package one.axim.gradle.utils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.javadoc.Javadoc;

import java.io.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public class JavaSourceParser {
    private CompilationUnit cu;

    private JavaSourceParser(File file) throws FileNotFoundException {
        JavaParser parser = new JavaParser();
        cu = parser.parse(file).getResult().get();
    }

    public static JavaSourceParser parse(File file) throws Exception {
        return new JavaSourceParser(file);
    }

    public CompilationUnit getCompilationUnit() {
        return cu;
    }

    /**
     * 문서의 코멘트를 가져온다.
     */
    public String getDocComment() {
        return cu.getComment()
                .filter(c -> c instanceof JavadocComment)
                .map(c -> ((JavadocComment) c).parse().getDescription().toText().trim())
                .orElse(null);
    }

    /**
     * 클래스의 코멘트를 가져온다.
     */
    public String getClassComment() {
        TypeDeclaration<?> type = cu.getType(0);
        if (type != null) {
            return type.getJavadoc()
                    .map(jd -> jd.getDescription().toText().trim())
                    .orElse(null);
        }
        return null;
    }

    /**
     * Method의 코멘트를 가져온다.
     */
    public MethodComment getMethodComment(Method method) {
        MethodDeclaration methodDeclaration = getMethodDeclaration(method);
        if (methodDeclaration != null) {
            Optional<Javadoc> javadoc = methodDeclaration.getJavadoc();
            if (javadoc.isPresent()) {
                return MethodComment.wrap(javadoc.get());
            }
        }
        return null;
    }

    /**
     * Field의 코멘트를 가져온다.
     */
    public String getFieldComment(final String field) {
        TypeDeclaration<?> type = cu.getType(0);
        if (type != null) {

            if (type.isEnumDeclaration()) {
                for (EnumConstantDeclaration entry : ((EnumDeclaration) type).getEntries()) {
                    if (entry.getName().toString().equals(field)) {
                        return entry.getJavadoc()
                                .map(jd -> jd.getDescription().toText().trim())
                                .orElse(null);
                    }
                }
                return null;
            } else {
                Optional<FieldDeclaration> optField = type.getFieldByName(field);
                if (optField.isPresent()) {
                    return optField.get().getJavadoc()
                            .map(jd -> jd.getDescription().toText().trim())
                            .orElse(null);
                }
            }
        }
        return null;
    }

    /**
     * 주어진 메서드로 Method Declaration 을 가져온다.
     */
    public MethodDeclaration getMethodDeclaration(Method method) {
        List<TypeDeclaration<?>> types = cu.getTypes();
        for (TypeDeclaration<?> type : types) {
            List<BodyDeclaration<?>> members = type.getMembers();
            for (BodyDeclaration<?> member : members) {
                if (member instanceof MethodDeclaration) {
                    MethodDeclaration methodDeclaration = (MethodDeclaration) member;
                    if (method.getName().equals(methodDeclaration.getNameAsString())
                            && method.getParameterCount() == methodDeclaration.getParameters().size()) {
                        return methodDeclaration;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 주어진 메서드 이름으로 파라미터들을 가져온다.
     */
    public String getMethodParameterName(Method method, int index) {
        MethodDeclaration md = getMethodDeclaration(method);
        return md.getParameters().get(index).getNameAsString();
    }
}
