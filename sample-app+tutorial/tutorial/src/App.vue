<template>
  <div id="app">
    <div id="view">
      <router-view/>
    </div>
    <sidebar-menu
        class="sidebar"
        :menu="menu"
        :hide-toggle="true"
    />
  </div>
</template>

<script>
export default {
  name: "App",
  data() {
    return {
      menu: [
        {
          header: "JQL Tutorial"
        },
        {
          href: "/basic/list",
          title: "Listing & Selection",
          icon: "favicon.ico"
        },
        {
          href: "/basic/compare",
          title: "Compare operators",
          icon: "fa fa-file-alt",
        },
        {
          href: "/basic/multi-compare",
          title: "Multi Compare operators",
          icon: "fa fa-file-alt",
        },
        {
          href: "/basic/and",
          title: "And expression",
          icon: "fa fa-file-alt"
        },
        {
          href: "/basic/or",
          title: "Or expression",
          icon: "fa fa-file-alt"
        },
        {
          href: "/basic/join",
          title: "Join query",
          icon: "fa fa-file-alt",
        },
        {
          href: "/join/recursive",
          title: "Recursive Joined query",
          icon: "fa fa-file-alt"
        },
        {
          href: "/join/json",
          title: "Json column search",
          icon: "fa fa-file-alt"
        },

        {
          href: "/external",
          title: "Links",
          icon: "fa fa-file-alt"
        },
      ],
      collapsed: false
    };
  },
};
</script>

<style>
#view {
  padding-left: 350px;
}
#view.collapsed {
  padding-left: 50px;
}

.sidebar.v-sidebar-menu .vsm-arrow:after {
  content: "\f105";
  font-family: "FontAwesome";
}
.sidebar.v-sidebar-menu .collapse-btn:after {
  content: "\f07e";
  font-family: "FontAwesome";
}
</style>
