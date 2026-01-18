package com.tkb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.tkb.entity.LogAnalysisEntity;
import com.tkb.entity.UserEntity;
import com.tkb.mapper.LogAnalysisMapper;
import com.tkb.mapper.UserMapper;
import com.tkb.service.LogAnalysisService;
import com.tkb.service.UserService;
import com.tkb.utils.JwtUtils;
import com.tkb.vo.PageBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogAnalysisServiceImpl extends ServiceImpl<LogAnalysisMapper, LogAnalysisEntity> implements LogAnalysisService {


    @Override
    public PageBean page(Integer page, Integer pageSize, String serviceName) {
        PageHelper.startPage(page, pageSize);

        List<LogAnalysisEntity> list = this.lambdaQuery()
                .like(LogAnalysisEntity::getServiceName, serviceName)
                .list();

        Page<LogAnalysisEntity> pageList = (Page<LogAnalysisEntity>) list;

        // 3. 封裝 pageBean 對象
        return new PageBean(pageList.getTotal(), pageList.getResult());
    }
}
