package com.tkb.utils.Constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DeployState {
    DEPLOYING(0, "部署中"),
    SUCCESS(1, "成功"),
    FAILED(2, "失敗"),
    ROLLED_BACK(3, "回滾");

    private Integer code;
    private String message;

    public static DeployState fromCode(int code) {
        for (DeployState s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown DeployState code: " + code);
    }
}
