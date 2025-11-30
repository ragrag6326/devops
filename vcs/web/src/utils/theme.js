// src/utils/theme.js

// 1. 切換主題的函數
export const toggleTheme = (isDark) => {
  const html = document.documentElement;
  
  if (isDark) {
    html.setAttribute('data-theme', 'dark');
    // 如果有用 Element Plus 的暗黑模式，也要加上 class="dark"
    html.classList.add('dark'); 
    localStorage.setItem('theme', 'dark');
  } else {
    html.setAttribute('data-theme', 'light');
    html.classList.remove('dark');
    localStorage.setItem('theme', 'light');
  }
};

// 2. 初始化函數 (放在 main.js 或 App.vue onMounted 呼叫)
export const initTheme = () => {
  const savedTheme = localStorage.getItem('theme');
  // 如果沒有儲存過，可以檢查系統偏好
  const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  
  if (savedTheme === 'dark' || (!savedTheme && systemPrefersDark)) {
    toggleTheme(true);
  } else {
    toggleTheme(false);
  }
};