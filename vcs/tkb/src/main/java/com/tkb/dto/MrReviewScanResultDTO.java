package com.tkb.dto;

import lombok.Data;

@Data
public class MrReviewScanResultDTO {

    private String projectName;
    private int scanned;
    private int submitted;
    private int skipped;
    private String message;
}
