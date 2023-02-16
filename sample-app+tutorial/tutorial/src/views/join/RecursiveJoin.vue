
<template>
  <LessonView
      :js_code="code"
      :enable_table_select="false">
    <template v-slot:description>
      <H5> Recursive 검색 </H5>
      <div class="details">
        Associative Table 의 두 개의 Foreign key 가 동일한 PK-Table 을 참조하는 경우, <br>
        PK-Table 명을 포함한 Foreign key 를 기본 Join key 로 자동 선정한다. <br>
        아래는 character_friend_link table 을 통해 친구 관계를 recursive 하게 검색하는 예제이다.
      </div>
    </template>
  </LessonView>
</template>

<script>
import LessonView from "@/components/LessonView";

const sample_code = `
const jql_select = AUTO;

const jql_filter = {
  /*
   Luke 의 친구의 친구 중 특정 episode 에 출연한 캐릭터를 검색한다.
  */
  "name@like": "Luke%",
  "friend_": {
    "friend_": {
      "episode_" : {
          "title" : "JEDI"
      },
    }
  },
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
