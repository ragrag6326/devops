package com.tkb.api.gitlab.dto;

import lombok.Data;

@Data
public class GitlabMergedDto {
    private Long id;
    private String username;
    private String public_email;
    private String name;
    private String state;
    private Boolean locked;
    private String web_url;
}
