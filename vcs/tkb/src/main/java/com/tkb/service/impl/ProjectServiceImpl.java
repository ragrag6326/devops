package com.tkb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tkb.entity.ProjectEntity;
import com.tkb.mapper.ProjectMapper;
import com.tkb.service.ProjectService;
import com.tkb.utils.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, ProjectEntity> implements ProjectService {

    @Override
    public List<ProjectEntity> listActive() {
        return this.lambdaQuery()
                .eq(ProjectEntity::getIsActive, 1)
                .orderByAsc(ProjectEntity::getSortOrder)
                .orderByAsc(ProjectEntity::getId)
                .list();
    }

    @Override
    public List<ProjectEntity> listAll() {
        return this.lambdaQuery()
                .orderByAsc(ProjectEntity::getSortOrder)
                .orderByAsc(ProjectEntity::getId)
                .list();
    }

    @Override
    public ProjectEntity findByName(String name) {
        return this.lambdaQuery().eq(ProjectEntity::getName, name).one();
    }

    @Override
    public Result<String> addProject(ProjectEntity project) {
        if (project.getName() == null || project.getName().isBlank()) {
            return Result.error("專案名稱不可為空");
        }

        boolean exists = this.lambdaQuery()
                .eq(ProjectEntity::getName, project.getName())
                .exists();
        if (exists) {
            return Result.error("專案名稱已存在: " + project.getName());
        }

        if (project.getCategory() == null) {
            project.setCategory("backend");
        }
        if (project.getIsActive() == null) {
            project.setIsActive(1);
        }
        if (project.getSortOrder() == null) {
            project.setSortOrder(0);
        }

        project.setCreatedTime(LocalDateTime.now());
        project.setUpdatedTime(LocalDateTime.now());

        return this.save(project) ? Result.success("新增成功") : Result.error("新增失敗");
    }

    @Override
    public Result<String> updateProject(ProjectEntity project) {
        if (project.getId() == null) {
            return Result.error("缺少 ID");
        }

        // 若修改了 name，確認不與其他筆重複
        if (project.getName() != null) {
            boolean conflict = this.lambdaQuery()
                    .eq(ProjectEntity::getName, project.getName())
                    .ne(ProjectEntity::getId, project.getId())
                    .exists();
            if (conflict) {
                return Result.error("專案名稱已被其他專案使用: " + project.getName());
            }
        }

        project.setUpdatedTime(LocalDateTime.now());
        return this.updateById(project) ? Result.success("更新成功") : Result.error("更新失敗");
    }

    @Override
    public Result<String> deleteProject(Long id) {
        return this.removeById(id) ? Result.success("刪除成功") : Result.error("刪除失敗");
    }
}
