package com.tkb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@TableName("user")
@Schema(description = "使用者實體類")
public class UserEntity {

    @Schema(description = "使用者 ID", example = "1")
    @TableId(value = "id" , type = IdType.AUTO)
    private Long id;

    @Schema(description = "帳號", example = "admin")
    private String username;

    @Schema(description = "密碼", example = "$2a$10$...")
    private String password;

    @Schema(description = "姓名/暱稱", example = "系統管理員")
    private String name;

    @Schema(description = "角色權限 (admin/user)", example = "admin")
    private String role;

    @Schema(description = "建立時間")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @Schema(description = "更新時間")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
}