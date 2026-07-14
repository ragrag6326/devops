<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { mailUnreadCount, mailConfig, isMessageVisible, refreshMailUnreadCount } from '@/utils/mailState'

// ========== GitHub 設定 ==========
const GITHUB_USERNAME = 'ragrag6326/devops'

// ========== 信箱設定 ==========
// 信件設定已集中至 src/utils/mailState.js，在此直接使用 import 的 mailConfig

// 目前使用者名稱
const currentUsername = ref('')

// 取得此使用者可見的信件（在有效期內）
const userMails = computed(() => {
  const all = [
    ...(mailConfig['__all__'] || []),
    ...(mailConfig[currentUsername.value] || []),
  ]
  return all.filter(isMessageVisible)
})

// 已讀管理（存 localStorage）
const readSet = ref(new Set())

const isRead = (id) => readSet.value.has(id)

const markAsRead = (id) => {
  readSet.value.add(id)
  const stored = JSON.parse(localStorage.getItem('_mail_read') || '[]')
  if (!stored.includes(id)) {
    stored.push(id)
    localStorage.setItem('_mail_read', JSON.stringify(stored))
  }
  // 標記已讀後同步更新 badge
  refreshMailUnreadCount()
}

const unreadCount = computed(() =>
  userMails.value.filter((m) => !isRead(m.id)).length
)

// 信箱 UI 狀態
const mailboxOpen = ref(false)
const selectedMail = ref(null)

const openMail = (mail) => {
  selectedMail.value = mail
  markAsRead(mail.id)
}

const closeMail = () => {
  selectedMail.value = null
}

const toggleMailbox = () => {
  mailboxOpen.value = !mailboxOpen.value
  if (!mailboxOpen.value) selectedMail.value = null
}

// 同步未讀數到共享狀態（layout sidebar badge 讀這個）
watch(unreadCount, (val) => { mailUnreadCount.value = val }, { immediate: true })

onMounted(() => {
  currentUsername.value = localStorage.getItem('current_username') || ''
  const stored = JSON.parse(localStorage.getItem('_mail_read') || '[]')
  readSet.value = new Set(stored)
})

// ========== 連結設定 ==========
// 同一個 title 的連結放同一個 group → 共用同一個 3 欄 grid
const linkGroups = [
  {
    title: '桌遊',
    icon: '🎲',
    links: [
      {
        name: '阿瓦隆 Avalon',
        url: 'https://avalon.korry.group',
        desc: '線上阿瓦隆，適合多人連線摸魚🐟',
        tag: '桌遊',
      },
      {
        name: '狼人殺 WereWolf',
        url: 'https://werewolf.korry.group',
        desc: '線上狼人殺，適合上班偷懶',
        tag: '桌遊',
      },
    ],
  },
]

const openLink = (url) => {
  window.open(url, '_blank', 'noopener,noreferrer')
}
</script>

<template>
  <div class="relax-page">

    <!-- ===== 維護停止橫幅 ===== -->
    <div class="deprecation-banner">
      <div class="deprecation-inner">
        <span class="dep-icon">⚠️</span>
        <span class="dep-text">此網站不再更新維護</span>
        <span class="dep-sep">·</span>
        <a
          :href="`https://github.com/${GITHUB_USERNAME}`"
          target="_blank"
          rel="noopener noreferrer"
          class="dep-github-link"
        >
          <svg viewBox="0 0 24 24" fill="currentColor" class="dep-github-icon">
            <path d="M12 0C5.374 0 0 5.373 0 12c0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23A11.509 11.509 0 0 1 12 5.803c1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576C20.566 21.797 24 17.3 24 12c0-6.627-5.373-12-12-12z"/>
          </svg>
          關注我的 GitHub
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" class="dep-ext-icon">
            <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
            <polyline points="15 3 21 3 21 9" />
            <line x1="10" y1="14" x2="21" y2="3" />
          </svg>
        </a>
      </div>
    </div>

    <!-- ===== 信箱 ===== -->
    <div v-if="userMails.length > 0" class="mailbox-card">
      <div class="mailbox-header" @click="toggleMailbox">
        <div class="mailbox-title-row">
          <span class="mailbox-emoji">📬</span>
          <span class="mailbox-label">信箱</span>
          <span v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }}</span>
        </div>
        <span class="mailbox-chevron" :class="{ open: mailboxOpen }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </span>
      </div>

      <div class="mailbox-body" :class="{ open: mailboxOpen }">
        <!-- 信件列表 -->
        <template v-if="!selectedMail">
          <div
            v-for="mail in userMails"
            :key="mail.id"
            class="mail-row"
            :class="{ unread: !isRead(mail.id) }"
            @click="openMail(mail)"
          >
            <span class="mail-dot" :class="{ unread: !isRead(mail.id) }"></span>
            <div class="mail-row-content">
              <div class="mail-row-top">
                <span class="mail-from">{{ mail.from }}</span>
                <span class="mail-start-date">{{ mail.startDate }}</span>
              </div>
              <div class="mail-subject">{{ mail.subject }}</div>
            </div>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" class="mail-row-arrow">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </div>
        </template>

        <!-- 信件內容 -->
        <template v-else>
          <button class="mail-back" @click="closeMail">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="19" y1="12" x2="5" y2="12" />
              <polyline points="12 19 5 12 12 5" />
            </svg>
            返回
          </button>
          <div class="mail-detail-subject">{{ selectedMail.subject }}</div>
          <div class="mail-detail-meta">
            <span>寄件人：{{ selectedMail.from }}</span>
            <span>{{ selectedMail.startDate }}</span>
          </div>
          <div class="mail-detail-body">{{ selectedMail.body }}</div>
        </template>
      </div>
    </div>

    <!-- ===== 頁面標題 ===== -->
    <div class="page-header">
      <h1 class="page-title">上班累了看這邊</h1>
      <p class="page-subtitle">休息一下，點卡片在新分頁開啟外部連結</p>
    </div>

    <!-- ===== 連結清單 ===== -->
    <section v-for="group in linkGroups" :key="group.title + group.links[0]?.name" class="link-group">
      <div class="group-header">
        <span class="group-icon">{{ group.icon }}</span>
        <h2 class="group-title">{{ group.title }}</h2>
      </div>

      <div class="link-grid">
        <button
          v-for="item in group.links"
          :key="item.url"
          type="button"
          class="link-card"
          @click="openLink(item.url)"
        >
          <div class="link-card-top">
            <span class="link-name">{{ item.name }}</span>
            <span class="link-tag">{{ item.tag }}</span>
          </div>
          <p class="link-desc">{{ item.desc }}</p>
          <div class="link-url">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
              <polyline points="15 3 21 3 21 9" />
              <line x1="10" y1="14" x2="21" y2="3" />
            </svg>
            <span>{{ item.url.replace(/^https?:\/\//, '') }}</span>
          </div>
        </button>
      </div>
    </section>

    <p class="relax-hint">已不提供更新新增連結。</p>
  </div>
</template>

<style scoped>
.relax-page {
  padding: 0 4px;
  max-width: 960px;
}

/* ===== 維護橫幅 ===== */
.deprecation-banner {
  margin-bottom: 20px;
  border-radius: 12px;
  border: 1px solid color-mix(in srgb, var(--accent) 40%, transparent);
  background: color-mix(in srgb, var(--accent) 10%, transparent);
  padding: 14px 20px;
}

.deprecation-inner {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.dep-icon {
  font-size: 18px;
  line-height: 1;
  flex-shrink: 0;
}

.dep-text {
  font-size: 14px;
  font-weight: 700;
  color: var(--accent);
  letter-spacing: 0.01em;
}

.dep-sep {
  color: var(--accent);
  opacity: 0.5;
}

.dep-github-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--accent);
  text-decoration: none;
  padding: 5px 12px;
  border-radius: 8px;
  border: 1px solid color-mix(in srgb, var(--accent) 35%, transparent);
  background: color-mix(in srgb, var(--accent) 12%, transparent);
  transition: background 0.2s, transform 0.15s;
}

.dep-github-link:hover {
  background: color-mix(in srgb, var(--accent) 22%, transparent);
  transform: translateY(-1px);
}

.dep-github-icon {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
}

.dep-ext-icon {
  width: 13px;
  height: 13px;
  flex-shrink: 0;
}

/* ===== 信箱 ===== */
.mailbox-card {
  margin-bottom: 20px;
  border-radius: 12px;
  border: 1px solid color-mix(in srgb, var(--brand) 50%, var(--border-color));
  background: color-mix(in srgb, var(--brand) 10%, var(--panel));
  box-shadow: var(--shadow), 0 0 16px color-mix(in srgb, var(--brand) 18%, transparent);
  overflow: hidden;
  position: relative;
}

/* 左側彩色邊條 */
.mailbox-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, var(--brand) 0%, var(--secondary-color, #a78bfa) 100%);
  border-radius: 12px 0 0 12px;
}

.mailbox-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  cursor: pointer;
  user-select: none;
  transition: background 0.18s;
}

.mailbox-header:hover {
  background: var(--panel-alt);
}

.mailbox-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mailbox-emoji {
  font-size: 20px;
  line-height: 1;
}

.mailbox-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
}

.unread-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 99px;
  background: var(--brand);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}

.mailbox-chevron {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--muted);
  transition: transform 0.25s ease;
  flex-shrink: 0;
}

.mailbox-chevron.open {
  transform: rotate(90deg);
}

.mailbox-chevron svg {
  width: 16px;
  height: 16px;
}

.mailbox-body {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.35s ease;
  border-top: 0px solid var(--border-color);
}

.mailbox-body.open {
  max-height: 600px;
  border-top-width: 1px;
}

/* 信件列 */
.mail-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 18px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid var(--border-color);
}

.mail-row:last-child {
  border-bottom: none;
}

.mail-row:hover {
  background: var(--panel-alt);
}

.mail-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--border-color);
  transition: background 0.2s;
}

.mail-dot.unread {
  background: var(--brand);
  box-shadow: 0 0 6px color-mix(in srgb, var(--brand) 60%, transparent);
}

.mail-row-content {
  flex: 1;
  min-width: 0;
}

.mail-row-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 2px;
}

.mail-from {
  font-size: 12px;
  color: var(--muted);
}

.mail-start-date {
  font-size: 11px;
  color: var(--muted);
  flex-shrink: 0;
}

.mail-subject {
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.mail-row.unread .mail-subject {
  font-weight: 700;
}

.mail-row-arrow {
  width: 14px;
  height: 14px;
  color: var(--muted);
  flex-shrink: 0;
}

/* 信件內容 */
.mail-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 14px 18px 0;
  padding: 5px 10px;
  border-radius: 7px;
  border: 1px solid var(--border-color);
  background: transparent;
  color: var(--muted);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  font-family: inherit;
}

.mail-back:hover {
  background: var(--panel-alt);
  color: var(--text);
}

.mail-back svg {
  width: 14px;
  height: 14px;
}

.mail-detail-subject {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  margin: 14px 18px 6px;
}

.mail-detail-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--muted);
  margin: 0 18px 14px;
}

.mail-detail-body {
  font-size: 14px;
  color: var(--text);
  line-height: 1.8;
  white-space: pre-wrap;
  margin: 0 18px 18px;
  padding: 14px;
  border-radius: 8px;
  background: var(--panel-alt);
  border: 1px solid var(--border-color);
}

/* ===== 頁面標題 ===== */
.page-header {
  margin-bottom: 28px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 6px;
}

.page-subtitle {
  font-size: 13px;
  color: var(--muted);
  margin: 0;
}

/* ===== 連結清單 ===== */
.link-group {
  margin-bottom: 32px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.group-icon {
  font-size: 22px;
  line-height: 1;
}

.group-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
}

.link-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.link-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  padding: 18px 20px;
  border-radius: 14px;
  border: 1px solid var(--border-color);
  background: var(--panel);
  box-shadow: var(--shadow);
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.15s;
}

.link-card:hover {
  border-color: var(--brand);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.link-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
}

.link-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
}

.link-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 99px;
  background: var(--brand-muted);
  color: var(--brand);
  border: 1px solid color-mix(in srgb, var(--brand) 25%, transparent);
  flex-shrink: 0;
}

.link-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--muted);
}

.link-url {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--brand);
  font-family: 'JetBrains Mono', Consolas, monospace;
}

.link-url svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.relax-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--muted);
}
</style>
