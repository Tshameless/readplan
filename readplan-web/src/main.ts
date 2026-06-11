import { createApp } from 'vue';
import App from './App.vue';
import { installElementPlus } from './plugins/elementPlus';
import router from './router';
import { pinia } from './stores';
import './styles/index.scss';

const app = createApp(App);

installElementPlus(app);
app.use(pinia);
app.use(router);

app.mount('#app');
