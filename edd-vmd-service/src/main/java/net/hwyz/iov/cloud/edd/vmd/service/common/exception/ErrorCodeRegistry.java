package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 错误码注册表
 * <p>
 * 负责VMD模块错误码的唯一性校验、正则校验和查询。
 * </p>
 *
 * @author hwyz_leo
 */
public class ErrorCodeRegistry {

    /**
     * 错误码正则表达式：806开头，后跟3位数字
     */
    private static final Pattern CODE_PATTERN = Pattern.compile("^806\\d{3}$");

    /**
     * 按code索引的错误码映射
     */
    private static final Map<String, VmdErrorCode> CODE_MAP = new HashMap<>();

    static {
        Stream.of(VmdErrorCode.values()).forEach(errorCode -> {
            if (!CODE_PATTERN.matcher(errorCode.getCode()).matches()) {
                throw new IllegalStateException(
                        String.format("Invalid error code format: %s for %s. Must match pattern: %s",
                                errorCode.getCode(), errorCode.name(), CODE_PATTERN.pattern()));
            }

            VmdErrorCode existing = CODE_MAP.put(errorCode.getCode(), errorCode);
            if (existing != null) {
                throw new IllegalStateException(
                        String.format("Duplicate error code: %s found in %s and %s",
                                errorCode.getCode(), existing.name(), errorCode.name()));
            }
        });
    }

    /**
     * 根据code查询错误码
     *
     * @param code 错误码（806xxx格式）
     * @return 对应的VmdErrorCode枚举值
     */
    public static Optional<VmdErrorCode> findByCode(String code) {
        return Optional.ofNullable(CODE_MAP.get(code));
    }

    /**
     * 验证错误码格式是否正确
     *
     * @param code 待验证的错误码
     * @return 是否符合格式要求
     */
    public static boolean isValidCode(String code) {
        return code != null && CODE_PATTERN.matcher(code).matches();
    }

    /**
     * 获取所有注册的错误码数量
     *
     * @return 错误码数量
     */
    public static int getRegisteredCount() {
        return CODE_MAP.size();
    }

    /**
     * 获取所有注册的错误码
     *
     * @return 错误码集合
     */
    public static java.util.Set<String> getAllCodes() {
        return java.util.Collections.unmodifiableSet(CODE_MAP.keySet());
    }
}
