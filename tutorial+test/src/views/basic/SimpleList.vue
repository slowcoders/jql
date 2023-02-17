<template>
  <LessonView
      :js_code="code"
      :enable_table_select="true">
    <template v-slot:description>
      <H5> 계층적 프로퍼티 선택 </H5>
      <div class="details">
        JQL 은 GraphQL 과 유사한 계층적 프로퍼티 선택 기능을 제공한다.<br>
        검색 대상 노드(base entity)와 계층적으로 연결된 reference entity 들의 프로퍼티 중 일부만 선별하여 검색 결과를 생성한다.<br>
        Property 명 대신에 아래의 Alias 를 이용하여 축약된 문법을 사용할 수 있다.<br>
        <ol>
        <li><b>*</b> (All Leaf Properties : non-composite properties) </li>
        <li><b>0</b> (Primary Keys) </li>
        </ol>
        참고로, select 값을 지정하지 않은 경우, filter 의 내용에 따라 검색 결과에 포함할 프로퍼티들이 자동 선택된다.<br>
      </div>
    </template>
  </LessonView>
</template>

<script>
import LessonView from "@/components/LessonView";

const sample_code = `
/* 아래의 주석을 한 줄씩 해제하면서 검색 결과의 차이를 비교해 보십시오. */
const jql_select = "0"
  // + ", *"
  // + ", starship_"
  // + ", episode_"
  // + ", friend_.name"
  // + ", friend_.(name, friend_.name)"

const jql_filter = {}
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
