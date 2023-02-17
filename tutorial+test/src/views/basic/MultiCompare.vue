
<template>
  <LessonView
      :js_code="code"
      :enable_table_select="false">
    <template v-slot:description>
      <H5> 다중값 비교 </H5>
      <div class="details">
        비교값이 어레이 형태이면 property 값이 다중값 중 하나와 일치하는 가를 검사한다.<br>
        SLQ 로 변경 시 IN 또는 OR 표현식으로 변경된다.<br>
      </div>
    </template>
  </LessonView>
</template>

<script>
import LessonView from "@/components/LessonView";

const sample_code = `
const jql_select = AUTO;

/* 아래의 주석을 한 줄씩 번갈아 해제하면서 검색 결과의 차이를 비교해 보십시오. */
const jql_filter = {
  // "id": [1001, 1002]
  // "id@not": [1001, 1002]

  // "name": ["Darth Vader", "Luke Skywalker"]
  // "name@not": ["Darth Vader", "Luke Skywalker"]

  // "name@like": [ "L%", "D%" ]
  // "name@not like": [ "L%", "D%" ]
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
