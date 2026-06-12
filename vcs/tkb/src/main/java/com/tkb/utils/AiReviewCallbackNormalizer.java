package com.tkb.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tkb.dto.MrReviewCallbackDTO;
import org.springframework.util.StringUtils;

public final class AiReviewCallbackNormalizer {

    private AiReviewCallbackNormalizer() {
    }

    public static void normalize(MrReviewCallbackDTO callback) {
        if (callback == null || StringUtils.hasText(callback.getSummary())) {
            return;
        }

        String raw = firstNonBlank(callback.getFullReview(), callback.getSuggestions());
        if (!StringUtils.hasText(raw)) {
            return;
        }

        String aiText = extractAiText(raw);
        if (!StringUtils.hasText(aiText)) {
            aiText = extractAiTextFromJsonString(raw);
        }
        if (!StringUtils.hasText(aiText)) {
            return;
        }

        aiText = stripMarkdownFence(aiText);
        try {
            applyParsed(callback, JSON.parseObject(aiText));
        } catch (Exception ignored) {
        }
    }

    private static String extractAiText(String raw) {
        String trimmed = raw.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return trimmed;
        }
        try {
            if (trimmed.startsWith("[")) {
                JSONArray arr = JSON.parseArray(trimmed);
                if (arr != null && !arr.isEmpty()) {
                    return extractAiTextFromObject(arr.getJSONObject(0));
                }
            }
            return extractAiTextFromObject(JSON.parseObject(trimmed));
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractAiTextFromJsonString(String raw) {
        try {
            if (raw.trim().startsWith("[")) {
                JSONArray arr = JSON.parseArray(raw);
                if (arr != null && !arr.isEmpty()) {
                    return extractAiTextFromObject(arr.getJSONObject(0));
                }
            }
            return extractAiTextFromObject(JSON.parseObject(raw));
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractAiTextFromObject(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        JSONObject choice = obj.getJSONArray("choices") != null && !obj.getJSONArray("choices").isEmpty()
                ? obj.getJSONArray("choices").getJSONObject(0) : null;
        if (choice != null && choice.getJSONObject("message") != null) {
            String content = choice.getJSONObject("message").getString("content");
            if (StringUtils.hasText(content)) {
                return content;
            }
        }
        JSONObject content = obj.getJSONObject("content");
        if (content != null && content.getJSONArray("parts") != null && !content.getJSONArray("parts").isEmpty()) {
            String text = content.getJSONArray("parts").getJSONObject(0).getString("text");
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        if (obj.containsKey("summary") || obj.containsKey("suggestions")) {
            return obj.toJSONString();
        }
        return null;
    }

    private static void applyParsed(MrReviewCallbackDTO callback, JSONObject parsed) {
        if (parsed.containsKey("summary")) {
            callback.setSummary(parsed.getString("summary"));
        }
        if (parsed.containsKey("suggestions")) {
            callback.setSuggestions(FlexibleFieldConverter.toMarkdownText(parsed.get("suggestions")));
        }
        if (parsed.containsKey("fullReview")) {
            callback.setFullReview(parsed.getString("fullReview"));
        }
        if (parsed.containsKey("severity") && parsed.get("severity") != null) {
            callback.setSeverity(parsed.getInteger("severity"));
        }
    }

    private static String stripMarkdownFence(String text) {
        return text.trim()
                .replaceFirst("^```json\\s*", "")
                .replaceFirst("^```\\s*", "")
                .replaceFirst("```\\s*$", "");
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v;
            }
        }
        return null;
    }
}
