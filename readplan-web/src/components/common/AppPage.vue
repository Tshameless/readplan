<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
}>();
</script>

<template>
  <section class="app-page">
    <header class="app-page__header">
      <div>
        <h1>{{ title }}</h1>
        <p v-if="description">{{ description }}</p>
      </div>
      <slot name="actions" />
    </header>

    <div class="app-page__content">
      <slot />
    </div>
  </section>
</template>

<style scoped lang="scss">
.app-page {
  display: grid;
  gap: 16px;
}

.app-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.app-page__header h1 {
  margin: 0;
  font-size: 26px;
  color: #14213d;
}

.app-page__header p {
  margin: 8px 0 0;
  color: #52627a;
}

.app-page__content {
  display: grid;
  gap: 16px;
}
</style>
