<template>
  <router-view />
</template>

<style>
/* 引入现代字体定义 */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

/* 全局基准样式：完全对标图二的现代 Font-Family 与背景配色 */
:root {
  --font-family-sans: "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  --bg-main: #f3f4f6;
  --bg-card: #ffffff;
  --border-color: #e5e7eb;
  --text-main: #111827;
  --text-muted: #6b7280;
  --primary-color: #2563eb;
}

body {
  margin: 0;
  padding: 0;
  background-color: var(--bg-main);
  color: var(--text-main);
  font-family: var(--font-family-sans);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 全局卡片圆角与质感（图二核心） */
.el-card, .column, .role-card, .response-card {
  border-radius: 12px !important;
  border: 1px solid var(--border-color) !important;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.05), 0 1px 2px 0 rgba(0, 0, 0, 0.03) !important;
}

/* 全局按钮圆角与字体强化 */
.el-button {
  font-family: var(--font-family-sans) !important;
  font-weight: 600 !important;
  border-radius: 8px !important;
}

/* 输入框与选择框的圆角与字体调整 */
.el-input__wrapper, .el-textarea__inner {
  border-radius: 8px !important;
  font-family: var(--font-family-sans) !important;
}
</style>