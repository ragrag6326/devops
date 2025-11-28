package com.tkb.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tkb.entity.GitlabMrEntity;
import com.tkb.entity.VersionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GitlabMrMapper extends BaseMapper<GitlabMrEntity> {
}
