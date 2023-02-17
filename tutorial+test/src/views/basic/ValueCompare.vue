
<template>
  <LessonView
      :js_code="code"
      :enable_table_select="false">
    <template v-slot:description>
      <H5> 비교 연산자</H5>
      <div class="details">
        JQL 은 아래의 형태로 비교 연산식을 표현한다.<br>
        <b>"propertyKey@연산자" : 비교값</b><p/>
        "@연산자" 부분이 생략되면 equals 연산을 수행한다.<br>
      </div>
    </template>
  </LessonView>
</template>

<script>
import LessonView from "@/components/LessonView";

const sample_code = `
const jql_select = AUTO;

/* 아래의 주석을 한 줄씩 해제하면서 검색 결과의 차이를 비교해 보십시오. */
const jql_filter = {
  // "id": 1001
  // "id@not": 1001

  // "id@le": 1001
  // "id@lt": 1001
  // "id@ge": 1001
  // "id@gt": 1001

  // "id@ge": 1001, "id@le": 1002
  // "id@between": [1001, 1002]
  // "id@not between": [1001, 1002]

  // "name": "Luke Skywalker"
  // "name@not": "Luke Skywalker"

  // "name@le": "Luke Skywalker"
  // "name@lt": "Luke Skywalker"
  // "name@ge": "Luke Skywalker"
  // "name@gt": "Luke Skywalker"

  // "name@ge": "Darth Vader", "name@le": "Luke Skywalker"
  // "name@between": ["Darth Vader", "Luke Skywalker"]
  // "name@not between": ["Darth Vader", "Luke Skywalker"]

  // "name@like": "L%"
  // "name@not like": "L%"
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
