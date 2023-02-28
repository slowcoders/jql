<template>
  <div class="hello">
    <br>
    <h3>Joint Query Language(JQL)</h3>
    <br>
    JQL 은 Json 기반의 단순한 문법을 가진 계층적 데이터 검색 언어이다.
    <p/>
    <br>

    <h4>JQL vs GraphQL</h4>
    JQL 은 GraphQL 의 문법을 <b>select</b> 와 <b>filter</b> 로 분할하여 단순화하였다.<br>
    GraphQL 과 동일하게 계층적 데이터 검색이 가능하며, Backend 수정없이 간단한 조건 검색이 가능하다.
    <p/>
    <br>
    <h4>JQL Spring Library</h4>
    <div class="code">
      <code>
        curl -X 'POST' 'http://localhost:7007/api/jql/starwars/character/find' \<br>
        -H 'Content-Type: application/json' \<br>
        -d '{ "name@like": "Luke%" }'
      </code>
    </div>
    <p/>
    <br>
    <h4>JQL Spring Library</h4>
    <ul>
      <li>JDBC/JPA Repository 자동 생성</li>
      <li>Rest API Template</li>
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

code {
  color: lightgray;
}
div.code {
  padding: 0.5em 1em;
  background-color: black;
}
</style>
