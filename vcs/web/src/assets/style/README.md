# CSS 樣式架構說明

## 目錄結構

```
assets/style/
├── README.md              # 本說明文件
├── _variables.css         # 通用變數（不分主題）
├── _theme-light.css       # 淺色主題變數
├── _theme-dark.css        # 深色主題變數
├── base.css               # 基礎樣式（Reset、Layout、通用元件）
├── theme.css              # 主題入口（匯入變數 + 基礎樣式）
├── components/            # 元件覆蓋樣式
│   ├── element-plus.css   # 按鈕、表單、輸入框、卡片、分頁
│   ├── el-table.css       # 表格
│   ├── el-layout.css      # el-container, el-row, el-col
│   ├── el-dialog.css      # Dialog
│   └── el-message.css     # Message
```

## 載入順序（main.js）

1. `theme.css` - 主題變數 + 基礎樣式
2. `components/element-plus.css` - 按鈕、表單、輸入框
3. `components/el-table.css` - 表格
4. `components/el-layout.css` - 佈局
5. `components/el-dialog.css` - Dialog
6. `components/el-message.css` - Message

## 主題切換機制

- 透過 `html[data-theme='light']` 或 `html[data-theme='dark']` 切換
- Element Plus 需 `html.dark` class
- 由 `@/utils/theme.js` 的 `toggleTheme()` 控制

## Views 中的樣式建議

- 優先使用 CSS 變數：`var(--text)`、`var(--panel)`、`var(--border-color)` 等
- 避免硬編碼顏色，以支援主題切換
- 頁面專用樣式可放在 `<style scoped>` 中
