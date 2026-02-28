package com.tkb.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新image請求參數物件")
public class RenewImageDTO {

    @Schema(description = "操作人員", example = "admin")
    private String opertaionName;

    @Schema(description = "專案名稱", example = "go_nuxt")
    private String projectName;

    @Schema(description = "類型", example = "prod|backup")
    private String nodeType;

    @Schema(description = "部屬版號", example = "1.0.5")
    private String version;

}
