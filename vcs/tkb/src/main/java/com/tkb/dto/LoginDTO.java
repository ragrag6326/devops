package com.tkb.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
@Schema(description = "登入請求參數")
public class LoginDTO {

    @Schema(description = "使用者帳號", example = "tkb000...")
    private String username;

    @Schema(description = "使用者密碼", example = "tkb000...")
    private String password;
}
