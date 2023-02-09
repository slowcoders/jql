<template>
  <LessonView
      :js_code="code"
      :enable_table_select="true">
    <template v-slot:description>
      <H5> 계층적 프로퍼티 선택 </H5>
      <div class="details">
        정보 검색 시, 결과에 포함할 프로퍼티(select), 정렬(sort), 최대 개수(limit), 페이지(page)를 지정할 수 있다.<br>
        select 문은 검색 대상 노드(base entity)를 기점으로 계층적으로 연결된 reference entity 들의 전체 프로퍼티 중 검색 결과에 포함시킬 프로퍼티들을 선별하기 위해 사용한다.<br>
        select 를 지정하지 않으면, filter 노드의 내용에 따라 검색 결과에 포함할 프로퍼티들이 자동 선택된다.<br>
        <b>*</b> (All) 외에 <b>0</b> (Primary Keys) 키워드를 추가로 제공한다.<br>
      </div>
    </template>
  </LessonView>
</template>

<script>
import LessonView from "@/components/LessonView";

const sample_code = `
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
