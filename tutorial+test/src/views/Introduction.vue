<template>
  <div class="hello">
    <br>
    <h3>Json Query Language(JQL)</h3>
    <br>
    JQL 은 다양한 형태의 연결 구조를 가진 데이터의 검색을 위한 쿼리 언어이다.
    <p/>
    <br>

    <h4>JQL vs GraphQL</h4>
    JQL 은 <b>select</b> 문과 <b>filter</b> 문으로 구성된다. <br>
    <b>select</b> 는 GraphQL 의 query 문에 해당한다. 검색 결과를 계층적으로 구조화하기 위해 사용된다.<br>
    <b>filter</b> 는 SQL 의 where 조건절에 해당한다. Json 기반의 단순한 문법을 통해 기본적인 비교 연산과 Join 처리가 가능하도록 하였다. <br>
    JQL 은 기능상으로 GraphQL 과 유사하나, RDB 환경에 보다 최적화하고 문법을 최대한 단순화하였다.<br>
    Backend 수정 없이도 Rest API 만으로 간단한 DB 검색이 가능하도록 함으로써, Backend/Frontend 개발의 상호 의존성을 최소화하는 것이 JQL 의 주요 목표이다.
    <p/>
    <br>
    <h4>JQL Spring Library </h4>
    Spring JDBC 기반 JQL 구현 라이브러리로서, JDBC Metadata 를 분석하여 RestAPI 를 자동 생성한다.<br>
    특정 테이블이 JPA Entity 와 연결된 경우, 해당 Entity 와 EntityManager 를 통해 검색 및 검색 결과 직렬화를 처리한다.<br>
    <ul>
      <li>JDBC/JPA Repository 자동 생성</li>
      <li>SQL/JPQL 자동 생성 및 자동 Join 처리</li>
      <li>Entity Manager 를 통한 JPA CacheManager 지원</li>
      <li>JQL Rest API Template</li>
      <li>JDBC -> JPA migration utility</li>
      <li>JSON 칼럼 검색 기능 지원</li>
    </ul>
    <br>
    예제) starwars.character Table 에서 "Luke" 로 시작하는 Entity 검색하기.
    <div class="code">
      <code>
        curl -X 'POST' 'http://localhost:7007/api/jql/starwars/character/find' \<br>
        -H 'Content-Type: application/json' \<br>
        -d '{ "name@like": "Luke%" }'
      </code>
    </div>
    <p/>
    <br>
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
