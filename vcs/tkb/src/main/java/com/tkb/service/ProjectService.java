package com.tkb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tkb.entity.ProjectEntity;
import com.tkb.utils.result.Result;

import java.util.List;

public interface ProjectService extends IService<ProjectEntity> {

    /** 取得所有啟用中的專案（依 sort_order 排序） */
    List<ProjectEntity> listActive();

    /** 取得全部專案（含停用，管理用） */
    List<ProjectEntity> listAll();

    /** 依 name 查單筆 (找不到回傳 null) */
    ProjectEntity findByName(String name);

    Result<String> addProject(ProjectEntity project);

    Result<String> updateProject(ProjectEntity project);

    Result<String> deleteProject(Long id);
}
