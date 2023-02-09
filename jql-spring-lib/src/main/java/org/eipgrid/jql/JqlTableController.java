package org.eipgrid.jql;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface JqlTableController<ID> {

    JqlTable<ID> getTable();

    class Search<ID> implements JqlTableController<ID> {
        private final JqlTable<ID> table;

        public Search(JqlTable<ID> table) {
            this.table = table;
        }

        public JqlTable<ID> getTable() {
            return table;
        }

        @GetMapping(path = "/{id}")
        @Operation(summary = "지정 엔터티 읽기")
        @Transactional
        @ResponseBody
        public Object get(@PathVariable("id") ID id,
                          @RequestParam(value = "select", required = false) String select$) {
            JqlSelect select = JqlSelect.of(select$);
            Object entity = getTable().find(id, select);
            if (entity == null) {
                throw new HttpServerErrorException("Entity(" + id + ") is not found", HttpStatus.NOT_FOUND, null, null, null, null);
            }
            return entity;
        }

        @PostMapping(path = "/find", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
        @Operation(summary = "엔터티 검색")
        @Transactional
        @ResponseBody
        public Object find_form(@Schema(example = "{ \"select\": \"\", \"sort\": \"\", \"page\": 0, \"limit\": 0, \"filter\": { } }")
                                @ModelAttribute JqlQuery.Request request) {
            return find(request);
        }

        @PostMapping(path = "/find", consumes = {MediaType.APPLICATION_JSON_VALUE})
        @Operation(summary = "엔터티 검색")
        @Transactional
        @ResponseBody
        public JqlQuery.Response find(@Schema(example = "{ \"select\": \"\", \"sort\": \"\", \"page\": 0, \"limit\": 0, \"filter\": { } }")
                                      @RequestBody JqlQuery.Request request) {
            return request.buildQuery(getTable()).execute();
        }

        @PostMapping(path = "/count")
        @Operation(summary = "엔터티 수 조회")
        @Transactional
        @ResponseBody
        public long count(@Schema(implementation = Object.class)
                          @RequestBody() HashMap<String, Object> jsFilter) {
            long count = getTable().count(getTable().createFilter(jsFilter));
            return count;
        }


        @PostMapping(path = "/clear-cache")
        @ResponseBody
        @Operation(summary = "Cache 비우기")
        public void clearCache() {
            getTable().clearEntityCaches();
        }
    }

    interface ListAll<ID> extends JqlTableController<ID> {

        @GetMapping(path = "")
        @Operation(summary = "전체 목록")
        @Transactional
        @ResponseBody
        default JqlQuery.Response list(@RequestParam(value = "select", required = false) String select$) throws Exception {
            JqlSelect select = JqlSelect.of(select$);
            return JqlQuery.of(getTable(), select, null).execute();
        }
    }


    interface Insert<ID> extends JqlTableController<ID> {

        @PutMapping(path = "/", consumes = {MediaType.APPLICATION_JSON_VALUE})
        @Operation(summary = "엔터티 추가")
        @Transactional
        @ResponseBody
        default <ENTITY> ENTITY add(@Schema(implementation = Object.class)
                       @RequestBody Map<String, Object> properties) throws Exception {
            JqlTable<ID> table = getTable();
            ID id = table.insert(properties);
            return (ENTITY)table.find(id);
        }
    }

    interface Update<ID> extends JqlTableController<ID> {

        @PatchMapping(path = "/{idList}", consumes = {MediaType.APPLICATION_JSON_VALUE})
        @Operation(summary = "엔터티 내용 변경")
        @Transactional
        @ResponseBody
        default <ENTITY> Collection<ENTITY> update(
                @Schema(type = "string", required = true) @PathVariable("idList") Collection<ID> idList,
                @RequestBody Map<String, Object> properties) throws Exception {
            JqlTable<ID> table = getTable();
            table.update(idList, properties);
            return (Collection<ENTITY>)table.find(idList);
        }
    }

    interface Delete<ID> extends JqlTableController<ID> {
        @DeleteMapping("/{idList}")
        @ResponseBody
        @Operation(summary = "엔터티 삭제")
        @Transactional
        default Collection<ID> delete(@PathVariable("idList") Collection<ID> idList) {
            getTable().delete(idList);
            return idList;
        }
    }


    /****************************************************************
     * Form Control API set
     */
    interface InsertForm<FORM, ID> extends JqlTableController<ID> {

        @PostMapping(path = "/", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
        @ResponseBody
        @Operation(summary = "엔터티 추가")
        @Transactional
        default ID add_form(@ModelAttribute FORM formData) throws Exception {
            Map<String, Object> dataSet = convertFormDataToMap(formData);
            return getTable().insert(dataSet);
        }

        Map<String, Object> convertFormDataToMap(FORM formData);
    }

    class CRUD<ID> extends Search<ID> implements Insert<ID>, Update<ID>, Delete<ID> {
        public CRUD(JqlTable<ID> table) {
            super(table);
        }
    }

}




