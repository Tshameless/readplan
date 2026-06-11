import { createRouter, createWebHistory } from 'vue-router';
import { applyRouterGuards } from './guards';
import { constantRoutes } from './routes';

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ left: 0, top: 0 }),
});

applyRouterGuards(router);

export default router;
