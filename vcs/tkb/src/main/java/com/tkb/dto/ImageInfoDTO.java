package com.tkb.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "go_nuxt 取得版號參數物件")
public class ImageInfoDTO {
    private String type;
    private List<String> versions;
}
