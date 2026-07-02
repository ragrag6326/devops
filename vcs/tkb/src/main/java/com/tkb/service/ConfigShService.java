package com.tkb.service;

import com.tkb.dto.ConfigShDTO;
import com.tkb.dto.ConfigSyncResult;

public interface ConfigShService {

    /**
     * 讀取 config.sh → DTO（JSON）
     *
     * @param projectName DB project_config.name
     */
    ConfigShDTO read(String projectName);

    /**
     * 將 DTO 寫回 config.sh（覆寫 KEY= 行，保留原始註解與順序）
     *
     * @param projectName DB project_config.name
     * @param dto         修改後的內容
     */
    void write(String projectName, ConfigShDTO dto);

    /**
     * 同步前本機欄位驗證（不 SSH，快速回應）
     * hasProd=1 時驗證 PROD 必填欄位；hasDev=1 時驗證 DEV 必填欄位
     *
     * @param projectName DB project_config.name
     * @param dto         當前表單值（尚未存檔）
     */
    ConfigSyncResult checkSync(String projectName, ConfigShDTO dto);

    /**
     * 將已存檔的 config.sh 同步至遠端機器（SSH）
     * 同步後做遠端路徑驗證，非 SSH 錯誤回傳 warning
     *
     * @param projectName DB project_config.name
     */
    ConfigSyncResult syncToRemote(String projectName);
}
