<template>
  <div class="monitor-wrapper">
    <router-view v-slot="{ Component }">
      <transition name="fade-slide" mode="out-in">
        <KeepAlive>
          <component :is="Component" :key="$route.path" />
        </KeepAlive>
      </transition>
    </router-view>
  </div>
</template>

<style scoped>
.monitor-wrapper {
  width: 100%;
  min-height: 100%;
}

</style>