package com.tkb.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tkb.jackson.FlexibleMarkdownFieldDeserializer;
import lombok.Data;

@Data
public class MrReviewCallbackDTO {

    private Long reviewId;
    private String status;
    private String summary;

    @JsonDeserialize(using = FlexibleMarkdownFieldDeserializer.class)
    private String suggestions;

    private String fullReview;
    private Integer severity;
    private String errorMessage;
}
