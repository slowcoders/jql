
<template>
  <LessonView
      :js_code="code"
      :enable_table_select="false">
    <template v-slot:description>
      <H5> 검색 조건의 And 결합 </H5>
      <div class="details">
        { } 내부의 비교식은 And 연산으로 자동 결합된다.<br>
        아래의 비교식의 comment 를 차례로 해제하면서 검색 결과의 차이를 알아본다.<br>
      </div>
    </template>
  </LessonView>
</template>

<script>
import LessonView from "@/components/LessonView";

const sample_code = `
const jql_filter = {
  //  "species": "Human",
  //  "height@gt": 1.2,
  //  "height@lt": 2.0,
  //  "mass@gt": 60,
  //  "metadata.homePlanet": "Tatooine",
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
