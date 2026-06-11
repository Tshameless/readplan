import type { RouteRecordRaw } from 'vue-router';
import { PERMISSION_KEYS } from '@/constants/permission';
import { ROUTE_NAMES } from '@/constants/routeNames';
import AppLayout from '@/layouts/AppLayout.vue';

export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: ROUTE_NAMES.login,
    component: () => import('@/views/auth/LoginView.vue'),
    meta: {
      title: '登录',
      requiresAuth: false,
      hidden: true,
    },
  },
  {
    path: '/register',
    name: ROUTE_NAMES.register,
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: {
      title: '注册',
      requiresAuth: false,
      hidden: true,
    },
  },
  {
    path: '/',
    name: ROUTE_NAMES.root,
    component: AppLayout,
    redirect: '/books',
    meta: {
      title: 'ReadPlan',
      requiresAuth: false,
      hidden: true,
    },
    children: [
      {
        path: 'books',
        name: ROUTE_NAMES.home,
        component: () => import('@/views/book/BookCatalogView.vue'),
        meta: {
          title: '发现书籍',
          requiresAuth: false,
        },
      },
      {
        path: 'book/:id',
        name: ROUTE_NAMES.bookDetail,
        component: () => import('@/views/book/BookDetailView.vue'),
        meta: {
          title: '书籍详情',
          requiresAuth: false,
          hidden: true,
        },
      },
      {
        path: 'plan',
        name: ROUTE_NAMES.plan,
        component: () => import('@/views/plan/PlanView.vue'),
        meta: {
          title: '我的计划',
          requiresAuth: true,
        },
      },
      {
        path: 'note',
        name: ROUTE_NAMES.note,
        component: () => import('@/views/note/NoteView.vue'),
        meta: {
          title: '我的笔记',
          requiresAuth: true,
        },
      },
      {
        path: 'admin/books',
        name: ROUTE_NAMES.adminBooks,
        component: () => import('@/views/admin/AdminBookManageView.vue'),
        meta: {
          title: '后台管理',
          requiresAuth: true,
          requiredPermission: PERMISSION_KEYS.bookManage,
        },
      },
    ],
  },
  {
    path: '/403',
    name: ROUTE_NAMES.forbidden,
    component: () => import('@/views/error/ForbiddenView.vue'),
    meta: {
      title: '无权限',
      requiresAuth: false,
      hidden: true,
    },
  },
  {
    path: '/:pathMatch(.*)*',
    name: ROUTE_NAMES.notFound,
    component: () => import('@/views/error/NotFoundView.vue'),
    meta: {
      title: '页面不存在',
      requiresAuth: false,
      hidden: true,
    },
  },
];
