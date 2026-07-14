package com.tkb.api.gitlab.dto;

import lombok.Data;

/**
 * GitLab Commit API 回應
 */
@Data
public class GitlabCommitResponse {
    private String id;
    private String short_id;
    private String title;
    private String message;
    private String web_url;
}
