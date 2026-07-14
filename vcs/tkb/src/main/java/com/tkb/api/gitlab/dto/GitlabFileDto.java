package com.tkb.api.gitlab.dto;

import lombok.Data;

/**
 * GitLab Repository File API 回應
 * GET /projects/:id/repository/files/:file_path
 * content 為 base64 編碼過的檔案內容（依 encoding 欄位而定，預設 base64）
 */
@Data
public class GitlabFileDto {
    private String file_name;
    private String file_path;
    private Long size;
    private String encoding;
    private String content_sha256;
    private String ref;
    private String blob_id;
    private String commit_id;
    private String last_commit_id;
    private String content;
    private Boolean execute_filemode;
}
