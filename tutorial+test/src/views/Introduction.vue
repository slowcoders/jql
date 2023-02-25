<template>
  <div class="hello">
    <br>
    <h2>Json-based Query Language(JQL)</h2>
    <br>
    <h5>JQL 은 Json 기반의 단순한 문법을 가진 계층적 데이터 검색 언어이다.</h5>
    <p/>
    <p/>
    <h3>JQL vs GraphQL</h3>
    <h5>JQL 은 기능상으로 GraphQL 과 매우 유사하다.<br>
      특히, Frontend 개발자도 -Backend 개발자의 도움없이- 간단한 수준의 조건 검색을 쉽게 처리할 수 있다는 것이 특징이다.</h5>

    <p/>
    <p/>
    <h2>JQL Spring Library</h2>
    <code>
    curl -X 'POST' 'http://localhost:7007/api/jql/starwars/character/find' \<br>
    -H 'Content-Type: application/json' \<br>
    -d '{  "filter": { "name@like": "Luke%" } }'
    </code>
    <h2>Json Query Language(JQL)</h2>
    <br>
    <ol>
      <li>GraphQL 과 같은 계층적 구조의 정보 검색</li>
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
