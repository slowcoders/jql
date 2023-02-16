
<template>
  <LessonView
      :js_code="code"
      :enable_table_select="false">
    <template v-slot:description>
      <H5> Join Query. </H5>
      <div class="details">
        JQL 은 DB 의 metadata 를 분석하여 FK->PK Join 관계를 자동 분석하여 처리한다.<br>
        Property key 를 '.' 기호로 연결하거나, 비교값 위치에 Object {} 또는 Object Array [ {} ] 를 사용하여 Joined Query 를 작성할 수 있다.<p/>
        select 값을 명시하지 않으면, filter 노드에 포함된 entity 들이 검색 결과에 자동으로 포함된다.<p/>
        Joined entity 의 특정 프로퍼티들만을 선택하고자 하는 경우, starship.&lt;name, length&gt; 와 대상을 명시할 수 있다.
      </div>
    </template>
  </LessonView>
</template>

<script>
import LessonView from "@/components/LessonView";

const sample_code = `
const jql_select = AUTO;

const jql_filter = {
  "species" : "Human",

  /* 특정 JEDI 에피소드 출연진을 검색.
  ----------------------------------------------------------*/
  // "episode_": { "title": "JEDI" },

  /* 에피소드의 발행일로 시간 검색.
  ----------------------------------------------------------*/
  // "episode_": { "published@ge": "2021-12-01T00:00:00" }

  /* { } 내부의 비교식은 And 조건으로 처리된다. 아래 두 예제는 각각
    길이가 10m 이상이고, 이름이 'M'으로 시작하는 비행선의 조종사를 검색한다.
  ----------------------------------------------------------*/
  // "starship_.length@ge": 10, "starship_.name@like": "M%"
  // "starship_": { "length@ge": 10, "name@like": "M%" }

  /* [ ] 내부의 비교 노드는 Or 조건으로 처리된다.
     10m 이하 또는 20m 이상의 비행선의 조종사를 검색하는 예제.
  ----------------------------------------------------------*/
  // "starship_": [ { "length@le": 10 }, { "length@ge": 20 } ]

  /* [ ] 내부의 비교 노드는 Or 조건으로 처리된다.
     10m 이하 또는 이름이 'M'으로 시작하는 비행선의 조종사를 검색
  ----------------------------------------------------------*/
  // "starship_": [ { "length@le": 10 }, { "name@like": "M%" } ]
}
`

export default {
  components: {
    LessonView
  },

  data() {
    return {
      code: sample_code
    }
  }
}
</script>
