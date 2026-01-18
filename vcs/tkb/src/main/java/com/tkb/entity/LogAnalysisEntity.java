package com.tkb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("log_analysis")
@Schema(description = "Log AI 分析結果實體")
public class LogAnalysisEntity {

    @Schema(description = "主鍵 ID", example = "100")
    @TableId(value = "id" , type = IdType.AUTO)
    private Long id;

    @Schema(description = "原始 Log 發生時間", example = "2023-12-25 10:00:00")
    private String logTime;

    @Schema(description = "服務名稱", example = "tkb-user-service")
    private String serviceName;

    @Schema(description = "報錯類別 (Logger Class)", example = "com.tkb.controller.UserController")
    private String loggerClass;

    @Schema(description = "錯誤摘要/原因", example = "NullPointerException: User ID cannot be null")
    private String errorReason;

    @Schema(description = "AI 分析摘要", example = "用戶 ID 為空導致的空指針異常")
    private String aiSummary;

    @Schema(description = "AI 判斷根因", example = "前端請求參數未傳遞 ID")
    private String aiRootCause;

    @Schema(description = "AI 建議解決方案", example = "在 Controller層增加 @NotNull 驗證")
    private String aiSolution;

    @Schema(description = "嚴重程度 (1=低 ~ 5=高)", example = "3")
    private Integer severity;

    @Schema(description = "AI 提供商", example = "OpenAI")
    private String aiProvider;

    @Schema(description = "AI 模型名稱", example = "gpt-4-turbo")
    private String aiModel;

}