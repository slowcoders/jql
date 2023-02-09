
<template>
  <LessonView
      :js_code="code"
      :enable_table_select="false">
    <template v-slot:description>
      <H5> 검색 조건의 Or 결합 </H5>
      <div class="details">
      * JQL 문법은 { } 내부는 And 연산, [ ] 내부는 Or 연산을 기본 적용한다.<br>
      하나의 JQL Node 내부에서 Or 연산이 필요한 경우 "@not" + { } 형식으로 표현한다.<br>
        동일한 프로퍼티를 비교하는 경우, 다중값 어레이 또는 @between 을 사용하여 간명하게 표현하는 것이 바람직하다.</div>
    </template>
  </LessonView>
</template>

<script>
import LessonView from "@/components/LessonView";

const sample_code = `
const normal_height = {
    "height@gt": 1.2,
    "height@lt": 2.0
}

const normal_mass = {
    "mass@gt": 40,
    "mass@lt": 120
}

const too_small_or_too_tall = {
    "@not": normal_height
}

const too_light_or_too_heavy = {
    "@not": normal_mass
}

const too_small_or_too_heavy = {
    "@not": {
        "height@gt": 1.2,
        "mass@lt": 120
    }
}



const too_small_or_too_tall__AND__too_light_or_too_heavy = {
  "@not": [
    normal_height,
    normal_mass
  ]
}

const too_small_or_too_tall__AND__too_light_or_too_heavy__short_expression = {
    "height@not between": [1.2, 2],
    "mass@not between": [40, 120],
}

const jql_filter = too_small_or_too_tall;
// const jql_filter = too_light_or_too_heavy;
// const jql_filter = too_small_or_too_heavy;
// const jql_filter = too_small_or_too_tall__AND__too_light_or_too_heavy;
// const jql_filter = too_small_or_too_tall__AND__too_light_or_too_heavy__short_expression;

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
