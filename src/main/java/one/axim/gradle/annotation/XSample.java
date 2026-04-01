package one.axim.gradle.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DTO 필드에 JSON 샘플 값을 지정합니다.
 *
 * <p>spec-bundle.json의 {@code requestSample}/{@code responseSample} 생성 시
 * 이 어노테이션의 값이 최우선으로 사용됩니다.
 *
 * <pre>{@code
 * public class WithdrawalRequest {
 *     @XSample("user-001")
 *     private String partnerUserId;
 *
 *     @XSample("100.00")
 *     private BigDecimal amount;
 *
 *     @XSample("BSC")
 *     private String chainType;
 * }
 * }</pre>
 *
 * <h3>값 해석 규칙:</h3>
 * <ul>
 *   <li>String, Enum, LocalDateTime, BigDecimal → 문자열 그대로 (JSON에서 따옴표 감싸기)</li>
 *   <li>Long, Integer → 숫자 변환</li>
 *   <li>Boolean → boolean 변환</li>
 * </ul>
 *
 * @since 2.1.5
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface XSample {
    /** 샘플 값 (JSON 직렬화 후의 문자열 표현) */
    String value();
}