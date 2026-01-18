package com.tkb.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登入返回參數")
public class LoginVO {

    @Schema(description = "使用者密碼", example = "tkb000...")
    private Long id;

    @Schema(description = "使用者帳號", example = "tkb000...")
    private String username;

    @Schema(description = "角色", example = "ADMIN")
    private String role;

    @Schema(description = "jwt token", example = "eyJhbGci...")
    private String token;
}