
<template>
  <LessonView
      :js_code="code"
      :enable_table_select="false">
    <template v-slot:description>
      <H5> Json column search. </H5>
      <div class="details">
        Json 객체를 저장한 Column 은 Joined Entity 와 동일한 문법으로 검색한다.
      </div>
    </template>
  </LessonView>
</template>

<script>
import LessonView from "@/components/LessonView";

const sample_code = `
const jql_select = AUTO;

const jql_1 = {
  "metadata": { "memo" : { "favoriteFood" : "kimchi" } }
}
const jql_2 = {
  "metadata": { "memo" : { "shoeSize@ge" : 270 } }
}

/* 아래의 주석을 한 줄씩 번갈아 해제하면서 검색 결과의 차이를 비교해 보십시오. */
const jql_filter = jql_1;
// const jql_filter = jql_2;
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

<style>
b.alias {
  display: inline-block;
  font-weight: bold;
  width: 2ex;
  text-align: center;
  margin-left: 2ex;
}
</style>
