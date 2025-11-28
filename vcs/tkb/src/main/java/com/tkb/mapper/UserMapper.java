package com.tkb.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tkb.entity.GitlabMrEntity;
import com.tkb.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
