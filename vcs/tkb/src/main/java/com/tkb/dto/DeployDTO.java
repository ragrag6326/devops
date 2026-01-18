package com.tkb.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * （Jenkins / Shell 專用）
 */

@Data
@Schema(description = "部屬請求參數物件")
public class DeployDTO {

    @Schema(description = "專案名稱", example = "tkbtv 、 tkbgoapi")
    private String projectName;

    @Schema(description = "專案環境 (dev / prod)", example = "prod")
    private String projectEnv; // dev / prod

    @Schema(description = "部屬版號", example = "1.0.5")
    private String version;

    @Schema(description = "執行部屬的人員/帳號", example = "jenkins")
    private String user;

    @Schema(description = "發布備註/更動內容", example = "修復登入逾時問題")
    private String releaseNote;
}
