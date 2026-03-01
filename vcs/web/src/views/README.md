# Views 樣式指南

## 深淺主題支援

所有 views 應優先使用 **CSS 變數**，以自動支援深淺主題切換。

### 建議使用的變數

| 變數 | 說明 |
|------|------|
| `var(--text)` | 主要文字顏色 |
| `var(--muted)` | 次要/弱化文字 |
| `var(--bg)` | 頁面背景 |
| `var(--panel)` | 卡片/面板背景 |
| `var(--panel-alt)` | 次要區塊背景 |
| `var(--border-color)` | 邊框顏色 |
| `var(--brand)` | 品牌主色 |
| `var(--primary-color)` | 主色（與 brand 類似） |
| `var(--danger)` | 危險/錯誤色 |
| `var(--success)` | 成功色 |
| `var(--accent)` | 強調色 |
| `var(--glass-bg)` | 玻璃擬態背景 |
| `var(--glass-border)` | 玻璃擬態邊框 |
| `var(--text-main)` | 主要文字（Layout 用） |
| `var(--text-sub)` | 次要文字（Layout 用） |
| `var(--table-text-color)` | 表格文字 |
| `var(--table-header-text-color)` | 表頭文字 |
| `var(--table-border-color)` | 表格邊框 |
| `var(--table-hover-bg)` | 表格 hover 背景 |

### 範例

```vue
<style scoped>
.my-card {
  background: var(--panel);
  border: 1px solid var(--border-color);
  color: var(--text);
}

.my-muted {
  color: var(--muted);
}
</style>
```

### 避免

- 硬編碼顏色：`#1e293b`、`#0f172a`、`rgba(30, 41, 59, 0.7)` 等
- 僅適用深色主題的樣式

### 特殊情況

若元件需固定顏色（如終端機視窗、程式碼區塊），可保留硬編碼，但建議加上註解說明。
