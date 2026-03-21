package one.axim.gradle.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 문서 생성에서 제외할 컨트롤러 또는 메서드에 사용합니다.
 *
 * <p>클래스에 적용하면 해당 컨트롤러의 모든 API가 제외되고,
 * 메서드에 적용하면 해당 엔드포인트만 제외됩니다.
 *
 * <pre>{@code
 * @XApiIgnore
 * @RestController
 * public class InternalController { ... }
 *
 * @RestController
 * public class UserController {
 *     @XApiIgnore
 *     @GetMapping("/debug")
 *     public String debug() { ... }
 * }
 * }</pre>
 *
 * @since 2.1.1
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface XApiIgnore {
}