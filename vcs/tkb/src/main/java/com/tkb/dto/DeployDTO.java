package com.tkb.dto;


import lombok.Data;

/**
 * （Jenkins / Shell 專用）
 */

@Data
public class DeployDTO {
    private String projectName;
    private String projectEnv; // dev / prod
    private String version;
    private String user;
    private String releaseNote;
}
