<template>
  <form>
    <div>
      <div style="background-color: #F0F0F0">
        <table><tr><td>
          <label class="form-label">Storage: </label>
        </td><td class="input-column">
          <b-form-select v-model="selectedStorage"
                         :options="storageNames"
                         @input="onTableChanged()">
          </b-form-select>
        </td><td>
          <label class="form-label">Table: </label>
        </td><td class="input-column">
          <b-form-select v-model="selectedTable"
                         :options="tableNames"
                         :disabled="!showSchemaInfo"
                         @input="onTableChanged()">
          </b-form-select>
        </td><td>
          <label class="form-label">Select: </label>
        </td><td class="input-column">
          <b-dropdown text="Columns" ref="dropDown">
            <b-dropdown-item @click.stop="">
              <b-form-checkbox-group v-model="selectedColumns"
                                       :options="selectableColumns"
                                       stacked
                                       @change="onSelectChanged"/>
            </b-dropdown-item>
          </b-dropdown>
        </td><td>
          <label class="form-label">Sort: </label>
        </td><td class="input-column">
          <b-form-select
                         class="form-control"
                         v-model="first_sort"
                         @input="onTableChanged()">
            <b-form-select-option :value="''" key="-1">
               Sort Options
            </b-form-select-option>
            <b-form-select-option
                v-for="(value, i) in sortOptions"
                :key="i"
                :value="value.trim()">
              {{ value }}
            </b-form-select-option>
          </b-form-select>
        </td><td>
          <label class="form-label">Limit: </label>
        </td><td class="input-column">
          <b-form-input v-model="limit"
                        @input="onTableChanged()"/>
        </td>
        </tr>
        </table>
      </div>
      <br>
      <slot name="description" />

    </div>

    <!------------>
    <div id="code-area">
      <div class="code" style="position: relative">
        <CodeMirror ref="codeView"
                    v-model:value="sampleCode"
                    :options="editOptions"
                    border
                    placeholder="test placeholder"
        />

        <b-button style="position: absolute; top:5px; right: 10px" @click="execute">
          run
        </b-button>
      </div>
      <div class="code">
        <CodeMirror ref="resultView"
            class="test-result-view col-sm-6"
            v-model:value="test_result"
            :options="viewOptions"
            border
            placeholder="test placeholder"
            :aria-readonly="true"
        />
      </div>
    </div>
  </form>
</template>

<script>
import CodeMirror from "codemirror-editor-vue3";
import "codemirror/mode/javascript/javascript.js";
import "codemirror/theme/dracula.css";

import { ref } from "vue";

import axios from "axios";

const dbSchema = 'starwars';
const baseUrl = 'http://localhost:7007/api/jql'

function count_lines(code) {
  const lines = code.split("\n");
  return lines.length;
}

const sampleStorages = [
  "starwars",
  "starwars_jpa",
]

const sampleTables = [
  "character",
  "starship",
  "episode",
  "character_episode_link",
  "character_friend_link"
]

export default {
  props : {
    js_code : String,
    enable_table_select: Boolean,
  },

  components: { CodeMirror },
  data() {
    return {
      showSchemaInfo: this.enable_table_select,
      storageNames: sampleStorages,
      selectedStorage: sampleStorages[0],
      tableNames: sampleTables,
      selectedTable: sampleTables[0],
      schemaInfo: '',
      selectableColumns: [],
      selectedColumns: [],
      allColumnSelected: true,
      sortOptions: [],
      first_sort: '',
      columns: '*',
      limit: 0,
      sampleCode: "--", //this.make_sample_code(),
      source_lines: count_lines(this.js_code),
      test_result: '',
      sortBy: null,
      cntTest: 0,
      axios: axios,
      editOptions: {
        mode: "text/javascript", // Language mode
        theme: "default", // Theme
        lineNumbers: true, // Show line number
        smartIndent: true, // Smart indent
        indentUnit: 4, // The smart indent unit is 2 spaces in length
        foldGutter: true, // Code folding
        styleActiveLine: true, // Display the style of the selected row
      },
      viewOptions: {
        mode: "text/javascript", // Language mode
        theme: "dracula", // Theme
        lineNumbers: false, // Show line number
        smartIndent: true, // Smart indent
        indentUnit: 2, // The smart indent unit is 2 spaces in length
        foldGutter: true, // Code folding
        lineWrapping: true,
        styleActiveLine: true, // Display the style of the selected row
      }
    }
  },

  mounted() {
    this.codeView = this.$refs.codeView.cminstance;
    this.resultView = this.$refs.resultView.cminstance;
    setTimeout(this.onTableChanged, 10);
  },

  computed: {
  },

  methods : {
    execute() {
      try {
        eval(this.sampleCode);
      }
      catch(e) {
        this.show_error_in_result_view("Test source compile error.\n" + e.message);
      }
    },

    make_sample_code() {
      const vm = this;

      return ` // JQL Sample
const dbSchema = '${vm.selectedStorage}'
const dbTable = '${vm.selectedTable}'
const AUTO = ""
${vm.js_code}
const jql = {
  select: jql_select,\
  ${vm.first_sort?.length > 0 ? '\n  sort: "' + vm.first_sort + '", ' : ''}\
  ${vm.limit > 0 ? '\n  limit: ' + vm.limit + ', ' : ''}
  filter: jql_filter
}
this.http_post(\`\${baseUrl}/\${dbSchema}/\${dbTable}/find\`, jql);
${vm.schemaInfo}`
    },

    resetColumns() {
      const vm = this;

      axios.get(`${baseUrl}/metadata/${dbSchema}/${vm.selectedTable}`).
      then((res) => {
        const sortOptions = [];
        const selectableColumns = [
          { value: "*", text: "* (All internal properties)" },
          { value: "0", text: "0 (Primary keys)" },
        ];
        for (const column of res.data.columns) {
          sortOptions.push(" " + column);
          sortOptions.push("-" + column);
          selectableColumns.push({value: column, text: column })
        }
        for (const column of res.data.references) {
          const skey = column + ".*"
          selectableColumns.push({value: skey, text: skey, is_ref: true })
        }
        vm.sortOptions = sortOptions;
        vm.selectableColumns = selectableColumns;
        console.log(res.data);
      })
    },

    show_error_in_result_view(msg) {
      this.resultView.setValue("!!!! " + msg);
    },

    onSelectChanged() {
      const vm = this;
      if (vm.allColumnSelected) {
        if (vm.selectedColumns.filter((k) => k.indexOf('*') < 0).length > 0) {
          vm.selectedColumns = vm.selectedColumns.filter((k) => k != '*');
          vm.allColumnSelected = false;
        }
      }
      else {
        const wasAllColumnSelected = vm.allColumnSelected;
        vm.allColumnSelected = vm.selectedColumns.indexOf("*") >= 0;
        if (!wasAllColumnSelected && vm.allColumnSelected) {
          vm.selectedColumns = vm.selectedColumns.filter((k) => k.indexOf('*') >= 0);
        }
      }

      vm.sortColumn = null;
      vm.codeView.setValue(vm.make_sample_code());
      vm.resultView.setValue("");
      vm.resetColumns();
    },

    onTableChanged() {
      const vm = this;
      if (vm.showSchemaInfo) {
        const url = `${baseUrl}/metadata/${dbSchema}/${vm.selectedTable}/Simple`
        axios.get(url).then(res => {
          vm.schemaInfo = `\n/*************** Schema<${vm.selectedTable}> ***********************\n${res.data}*/`;
          vm.onSelectChanged()
        }).catch(vm.show_http_error)
      } else {
        vm.onSelectChanged()
      }
    },

    show_http_error(err) {
      let msg = err.message + "\n" + JSON.stringify(err.response.data, null, 4);
      this.show_error_in_result_view(msg);
    },

    http_post(url, jql) {
      const vm = this;
      const options = {
        headers: { "Content-Type": `application/json`}
      }
      axios.post(url, jql, options).then(res => {
        vm.cntTest ++;
        const header = "ex " + vm.cntTest + ") result: " + res.data.content.length + "\n\n";
        const results = JSON.stringify(res.data.content, null, 2);
        const sql = res.data.metadata?.lastExecutedSql ? "\n\n---------------\nexecuted sql:\n" + res.data.metadata.lastExecutedSql : "";
        vm.resultView.setValue(header + results + sql);
      }).catch(vm.show_http_error)
    }
  }
};

</script>

<style>
  form {
    padding-top: 2em;
    padding-right: 2em;
    padding-bottom: 1em;
    display: grid;
    grid-template-rows: auto 1fr;
    height: 100vh;
    max-height: 100vh;
  }

  /*.test-result-view .CodeMirror {*/
  /*  overflow: auto;*/
  /*  height: 100%;*/
  /*}*/

  td > label {
    padding-top: 0.5em;
  }

  td.input-column {
    padding-right: 2em;
  }

  .details {
    padding-left: 1em;
    margin-bottom: 0.7em;
  }

  #code-area {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
  div.code {
    /*overflow: auto;*/
    /*max-height: 100%;*/
  }

  .CodeMirror * {
    font-family: Curier, monospace;
    font-size: small;
  }
  .test-result-view .CodeMirror-cursor {
    display: none !important
  }
  table td {
    padding: 5px
  }

</style>
