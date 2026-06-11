import type { App } from 'vue';
import ElementPlus from 'element-plus';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import 'element-plus/dist/index.css';

export const installElementPlus = (app: App) => {
  app.use(ElementPlus, {
    locale: zhCn,
  });
};
