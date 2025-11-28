package com.tkb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class UserEntity {

    @TableId(value = "id" , type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String name;

    private String role;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}