<template>
  <div class="hello">
    <br>
    <h1>JQL Sample & Tutorial</h1>
    <h3>Essential Links</h3>
    <ul>
      <li><a href="http://localhost:7007/swagger-ui.html" target="_blank" rel="noopener">Swagger API</a></li>
      <li><a href="https://github.com/slowcoders/jql-spring" target="_blank" rel="noopener">Sources</a></li>
    </ul>
  </div>
</template>

<script>
export default {
  name: 'ExternalLinks',
  props: {
    msg: String
  },
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h3 {
  margin: 40px 0 0;
}
li {
  margin: .5em
}
a {
  color: #42b983;
}
</style>
