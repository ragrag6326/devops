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
@TableName("system_aud_log")
@Schema(description = "系統日誌實體類")
public class SystemAudLogEntity {

    @Schema(description = "主鍵ID", example = "1")
    @TableId(value = "id" , type = IdType.AUTO)
    private Long id;

    @Schema(description = "專案名稱", example = "tkvtv")
    private String projectName;

    @Schema(description = "操作", example = "blue -> green")
    private String action;

    @Schema(description = "操作人員", example = "admin")
    private String operator;

    @Schema(description = "狀態", example = "0成功  1失敗")
    private int status;

    @Schema(description = "操作時間")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operationTime;
}