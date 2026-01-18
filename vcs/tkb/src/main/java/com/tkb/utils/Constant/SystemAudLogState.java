package com.tkb.utils.Constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SystemAudLogState {
    SUCCESS(0, "成功"),
    FAILED(1, "失敗");

    private Integer code;
    private String message;

    public static SystemAudLogState fromCode(int code) {
        for (SystemAudLogState s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown status code: " + code);
    }
}
