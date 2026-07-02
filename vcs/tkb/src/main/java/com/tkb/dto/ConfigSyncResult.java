package com.tkb.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * config.sh 同步前檢查 / 同步結果
 */
@Data
public class ConfigSyncResult {

    /** 阻擋性錯誤（必填欄位遺漏、格式錯誤） */
    private List<String> errors = new ArrayList<>();

    /** 警告（遠端路徑不存在等可繼續的問題） */
    private List<String> warnings = new ArrayList<>();

    /** 各 env 同步結果（sync 階段填入） */
    private Map<String, String> syncResult = new LinkedHashMap<>();

    public boolean isCanSync() {
        return errors.isEmpty();
    }

    public void addError(String msg)   { errors.add(msg); }
    public void addWarning(String msg) { warnings.add(msg); }
    public void putSync(String env, String msg) { syncResult.put(env, msg); }
}
