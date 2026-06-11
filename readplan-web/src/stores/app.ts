import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useAppStore = defineStore('app', () => {
  const isSidebarCollapsed = ref(false);

  const toggleSidebar = () => {
    isSidebarCollapsed.value = !isSidebarCollapsed.value;
  };

  return {
    isSidebarCollapsed,
    toggleSidebar,
  };
});
