import { ElMessage } from 'element-plus';

export const message = {
  success: (text: string) => ElMessage.success({ message: text, duration: 1800 }),
  error: (text: string) => ElMessage.error({ message: text, duration: 2400 }),
  warning: (text: string) => ElMessage.warning({ message: text, duration: 2200 }),
  info: (text: string) => ElMessage.info({ message: text, duration: 1800 }),
};
