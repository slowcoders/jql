<template>
  <div class="hello">
    <br>
    <h1>What is JQL</h1>
    <br>
    <h4>JQL is simple query language for database. It supports..</h4>
    <ol>
      <li>Hierarchical property selection like GraphQL</li>
      <li>JSON based simple search filter</li>
    </ol>
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
