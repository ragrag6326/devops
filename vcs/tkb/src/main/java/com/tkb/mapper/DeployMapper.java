package com.tkb.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tkb.entity.GitlabMrEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeployMapper extends BaseMapper<GitlabMrEntity> {
}
