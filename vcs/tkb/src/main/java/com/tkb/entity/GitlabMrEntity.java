package com.tkb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("gitlab_merge_requests")
public class GitlabMrEntity {

    @TableId(value = "id" , type = IdType.AUTO)
    private Long id;

    private String projectName;

    private String version;

    private Long mrId;

    private Integer iid;

    private String title;

    private String description;

    private String state;

    private String targetBranch;

    private String sourceBranch;

    /**
     * 發起 MR 者 也就是 author
     */
    private String authorName;

    /**
     * 最後由誰同意此次 MR 的
     */
    private String mergedBy;

    private LocalDateTime mergedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
