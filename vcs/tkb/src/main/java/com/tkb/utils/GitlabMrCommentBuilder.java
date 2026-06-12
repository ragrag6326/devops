package com.tkb.utils;

import com.tkb.entity.MrCodeReviewEntity;

public final class GitlabMrCommentBuilder {

    private GitlabMrCommentBuilder() {
    }

    public static String build(MrCodeReviewEntity review) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 🤖 AI Code Review\n\n");

        if (review.getSeverity() != null) {
            sb.append("**嚴重度**: ").append(severityLabel(review.getSeverity()))
                    .append(" (").append(review.getSeverity()).append("/5)\n\n");
        }
        if (review.getSummary() != null && !review.getSummary().isBlank()) {
            sb.append("### 摘要\n").append(review.getSummary().trim()).append("\n\n");
        }
        if (review.getSuggestions() != null && !review.getSuggestions().isBlank()) {
            sb.append("### 建議\n").append(review.getSuggestions().trim()).append("\n\n");
        } else if (review.getFullReview() != null && !review.getFullReview().isBlank()) {
            sb.append("### 詳細審核\n").append(review.getFullReview().trim()).append("\n\n");
        }

        sb.append("---\n");
        sb.append("*由 VCS 平台自動產生 · reviewId=").append(review.getId()).append("*");
        return sb.toString();
    }

    private static String severityLabel(int level) {
        return switch (level) {
            case 5 -> "Critical";
            case 4 -> "High";
            case 3 -> "Medium";
            case 2 -> "Low";
            default -> "Info";
        };
    }
}
