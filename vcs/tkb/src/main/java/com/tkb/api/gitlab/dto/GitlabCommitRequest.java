package com.tkb.api.gitlab.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GitLab Commit API 請求
 * POST /projects/:id/repository/commits
 * 一個 commit 可帶多個 actions（新增專案精靈用此特性原子寫入多個檔案）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitlabCommitRequest {
    private String branch;
    private String commit_message;
    private List<Action> actions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Action {
        /** create / update / delete / move */
        private String action;
        private String file_path;
        /** 純文字內容，未指定 encoding 時 GitLab 預設當作 text 處理 */
        private String content;
        /**
         * 樂觀鎖（選填）：帶上讀取當下檔案的 last_commit_id，
         * 若期間檔案已被別人 commit 過，GitLab 會回 400，避免互相覆蓋
         */
        private String last_commit_id;

        public Action(String action, String filePath, String content) {
            this(action, filePath, content, null);
        }
    }
}
