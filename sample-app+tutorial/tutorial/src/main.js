import { createApp } from 'vue'

import App from './App.vue'
import VueSidebarMenu from 'vue-sidebar-menu'
import router from './router/router'
import BootstrapVue3 from 'bootstrap-vue-3'
import VueHighlightJS from 'vue3-highlightjs'
import { GlobalCmComponent } from 'codemirror-editor-vue3';

import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue-3/dist/bootstrap-vue-3.css'
import 'vue-sidebar-menu/dist/vue-sidebar-menu.css'

const app = createApp(App)
//import axios from "axios";
//app.config.globalProperties.$axios = axios
app.use(VueHighlightJS)
app.use(GlobalCmComponent)
app.use(BootstrapVue3)
app.use(VueSidebarMenu)
app.use(router);
app.mount("#app")