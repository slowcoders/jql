import { createWebHashHistory, createRouter } from "vue-router";
import SimpleList from "../views/basic/SimpleList.vue"
import ValueCompare from "../views/basic/ValueCompare.vue"
import MultiCompare from "../views/basic/MultiCompare.vue"
import AndExpression from "@/views/basic/AndExpression";
import OrExpression from "@/views/basic/OrExpression";
import SimpleJoin from "../views/join/SimpleJoin.vue"
import RecursiveJoin from "@/views/join/RecursiveJoin";
import JsonSearch from "@/views/join/JsonSearch";
import ExternalLinks from "@/views/ExternalLinks";
import Introduction from "@/views/Introduction";

const router = createRouter({
    history: createWebHashHistory(),
    routes: [
        {
            path: '/',
            component: Introduction,
        },
        {
            path: '/basic/list',
            component: SimpleList,
        },
        {
            path: '/basic/compare',
            component: ValueCompare,
        },
        {
            path: '/basic/multi-compare',
            component: MultiCompare,
        },
        {
            path: '/basic/and',
            component: AndExpression,
        },
        {
            path: '/basic/or',
            component: OrExpression,
        },
        {
            path: '/basic/join',
            component: SimpleJoin,
        },
        {
            path: '/join/recursive',
            component: RecursiveJoin,
        },
        {
            path: '/join/json',
            component: JsonSearch,
        },
        {
            path: '/external',
            component: ExternalLinks,
        },
    ]
});

export default router