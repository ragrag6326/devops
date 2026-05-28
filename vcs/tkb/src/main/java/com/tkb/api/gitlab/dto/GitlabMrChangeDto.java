package com.tkb.api.gitlab.dto;

import lombok.Data;

import java.util.List;

@Data
public class GitlabMrChangeDto {

    private Long id;
    private Integer iid;
    private String title;
    private String state;
    private List<ChangeItem> changes;

    @Data
    public static class ChangeItem {
        private String old_path;
        private String new_path;
        private Boolean new_file;
        private Boolean renamed_file;
        private Boolean deleted_file;
        private String diff;
    }
}
