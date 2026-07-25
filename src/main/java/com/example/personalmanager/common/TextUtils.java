package com.example.personalmanager.common;

import org.springframework.util.StringUtils;

/**
 * 文本处理公共工具。
 */
public final class TextUtils {

    private TextUtils() {
    }

    /**
     * 将可空文本规整化：空白字符串归一为 null，否则去除首尾空白。
     */
    public static String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
