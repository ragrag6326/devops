package com.tkb.api.gitlab.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class GitlabDto {
    private Long id; // 唯獨
    private Integer iid;
    private String title;
    private String description;
    private String state;
    private String target_branch;
    private String source_branch;

    /**
     *  MR 同意人
     */
    private GitlabMergedDto merged_by;

    /**
     * 發起 MR  author
     */
    private GitlabMergedDto author;

    private OffsetDateTime merged_at;
    private OffsetDateTime  created_at;
    private OffsetDateTime  updated_at;
}
