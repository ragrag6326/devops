package com.tkb.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.springframework.util.StringUtils;

/**
 * 將 AI / N8N 回傳的 String 或 JSON 陣列統一轉成 Markdown 字串
 */
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

    /** 陣列元素已是 "- xxx" 開頭則不重複加項目符號 */
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

    /** 供 Jackson 反序列化：解析 JSON 陣列字串 */
    public static String parseJsonArrayStringIfNeeded(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("[")) {
            return raw;
        }
        try {
            JSONArray arr = JSON.parseArray(trimmed);
            return jsonArrayToMarkdown(arr);
        } catch (Exception e) {
            return raw;
        }
    }
}
