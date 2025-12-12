package com.tkb.utils.Constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GitlabReleaseState {
    // 是否已發布到 dev 或 prod 版
    FALSE(0, "False"),
    TRUE(1, "True");

    private Integer code;
    private String message;

    public static GitlabReleaseState fromCode(int code) {
        for (GitlabReleaseState s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown GitlabReleaseState code: " + code);
    }
}
