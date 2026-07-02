package com.tkb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * config.sh 內容 DTO（JSON 雙向轉換用）
 * <p>
 * 陣列型別（*_CHECK_PORTS）以空格分隔字串儲存，寫回時還原為 (val1 val2) bash array。
 * 其他值以裸字串儲存（不含外層引號）。
 */
@Data
@Schema(description = "config.sh 內容 DTO")
public class ConfigShDTO {

    @Schema(description = "DB project_config.name")
    private String projectName;

    @Schema(description = "tools/ 下的目錄名（scriptName，null 時同 name）")
    private String scriptName;

    @Schema(description = "正式機（PROD_*）欄位")
    private Map<String, String> prod = new LinkedHashMap<>();

    @Schema(description = "測試機（DEV_*）欄位")
    private Map<String, String> dev = new LinkedHashMap<>();

    @Schema(description = "共用欄位（IMAGE_KEYWORD 等）")
    private Map<String, String> shared = new LinkedHashMap<>();
}
