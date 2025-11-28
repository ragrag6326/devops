package com.tkb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_versions")
public class VersionEntity {

    @TableId(value = "id" , type = IdType.AUTO)
    private Long id;

    private String projectName;

    private String projectEnv;

    private String version;

    private int state;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
