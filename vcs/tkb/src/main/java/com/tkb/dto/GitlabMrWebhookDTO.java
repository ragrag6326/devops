package com.tkb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitlabMrWebhookDTO {

    @JsonProperty("object_kind")
    private String objectKind;

    @JsonProperty("object_attributes")
    private ObjectAttributes objectAttributes;

    private Project project;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ObjectAttributes {
        private Long id;
        private Integer iid;
        private String title;
        private String state;
        private String action;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Project {
        private Long id;
    }
}
