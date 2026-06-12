package com.tkb.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.springframework.util.StringUtils;

public final class FlexibleFieldConverter {

    private FlexibleFieldConverter() {
    }

    public static String toMarkdownText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            return str.isBlank() ? null : str;
        }
        if (value instanceof JSONArray arr) {
            return jsonArrayToMarkdown(arr);
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder sb = new StringBuilder();
            for (Object item : iterable) {
                appendMarkdownItem(sb, item != null ? item.toString() : "");
            }
            return sb.length() > 0 ? sb.toString() : null;
        }
        return value.toString();
    }

    public static String jsonArrayToMarkdown(JSONArray arr) {
        if (arr == null || arr.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.size(); i++) {
            Object item = arr.get(i);
            String text = item == null ? "" : item.toString();
            if (StringUtils.hasText(text)) {
                appendMarkdownItem(sb, text.trim());
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private static void appendMarkdownItem(StringBuilder sb, String text) {
        if (sb.length() > 0) {
            sb.append("\n\n");
        }
        if (text.startsWith("- ") || text.startsWith("* ")) {
            sb.append(text);
        } else {
            sb.append("- ").append(text);
        }
    }

    public static String parseJsonArrayStringIfNeeded(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("[")) {
            return raw;
        }
        try {
            return jsonArrayToMarkdown(JSON.parseArray(trimmed));
        } catch (Exception e) {
            return raw;
        }
    }
}
