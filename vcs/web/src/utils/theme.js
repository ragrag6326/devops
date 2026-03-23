// src/utils/theme.js
import { useDark, useToggle } from '@vueuse/core' // 這是 VueUse 的寫法，如果您沒用 VueUse，請用下方的原生寫法

// === 原生 JS 寫法 (與您原本的類似，但增強了對比度切換的邏輯) ===

export const toggleTheme = (isDark) => {
  const html = document.documentElement;
  
  if (isDark) {
    // 1. 設定 HTML 屬性供 CSS 變數使用
    html.setAttribute('data-theme', 'dark');
    // 2. Element Plus 需要 class="dark"
    html.classList.add('dark');
    // 3. 儲存設定
    localStorage.setItem('theme', 'dark');
  } else {
    html.setAttribute('data-theme', 'light');
    html.classList.remove('dark');
    localStorage.setItem('theme', 'light');
  }
};

export const initTheme = () => {
  const savedTheme = localStorage.getItem('theme');
  // 預設深色模式，除非使用者曾明確切換至淺色
  if (savedTheme === 'light') {
    toggleTheme(false);
  } else {
    toggleTheme(true);
  }
};