package com.tkb.vo;


/**
 * 分頁結果封裝類
 * @param <T> 數據列表中元素的類型
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分頁結果封裝類
 * @param <> 數據列表中元素的類型
 */
@Data // 包含 Getter, Setter, toString, equals, hashCode (Lombok)
@NoArgsConstructor // 無參構造 (Lombok)
@AllArgsConstructor // 全參構造 (Lombok)
public class PageBean {
    private Long total; // 總記錄數 (Total records)
    private List rows; // 當前頁數據列表 (Current page data list)`


}
