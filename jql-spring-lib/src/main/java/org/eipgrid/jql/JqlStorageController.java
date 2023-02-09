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
import java.util.Map;

public interface JqlStorageController {

    JqlRepository getRepository(String tableName);

    class Search implements JqlStorageController {
        private final JqlStorage storage;
        private final String tableNamePrefix;

        public Search(JqlStorage storage, String tableNamePrefix) {
            this.storage = storage;
            this.tableNamePrefix = tableNamePrefix;
        }

        public final JqlStorage getStorage() {
            return storage;
        }

        public JqlRepository getRepository(String tableName) {
            String tablePath = tableNamePrefix + tableName;
            return storage.getRepository(tablePath);
        }

        @GetMapping(path = "/{table}/{id}")
        @Operation(summary = "지정 엔터티 읽기")
        @Transactional
        @ResponseBody
        public Object get(@PathVariable("table") String table,
                          @PathVariable("id") String id$,
                          @RequestParam(value = "select", required = false) String select$) {
            JqlRepository repository = getRepository(table);
            JqlSelect select = JqlSelect.of(select$);
            Object id = repository.convertId(id$);
            Object entity = repository.find(id, select);
            if (entity == null) {
                throw new HttpServerErrorException("Entity(" + id$ + ") is not found", HttpStatus.NOT_FOUND, null, null, null, null);
            }
            return entity;
        }

        @PostMapping(path = "/{table}/find", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
        @Operation(summary = "엔터티 검색")
        @Transactional
        @ResponseBody
        public JqlQuery.Response find_form(@PathVariable("table") String table,
                                @Schema(example = "{ \"select\": \"\", \"sort\": \"\", \"page\": 0, \"limit\": 0, \"filter\": { } }")
                                @ModelAttribute JqlQuery.Request request) {
            return find(table, request);
        }

        @PostMapping(path = "/{table}/find", consumes = {MediaType.APPLICATION_JSON_VALUE})
        @Operation(summary = "엔터티 검색")
        @Transactional
        @ResponseBody
        public JqlQuery.Response find(@PathVariable("table") String table,
                           @Schema(example = "{ \"select\": \"\", \"sort\": \"\", \"page\": 0, \"limit\": 0, \"filter\": { } }")
                           @RequestBody JqlQuery.Request request) {
            JqlRepository repository = getRepository(table);
            return request.buildQuery(repository).execute();
        }

        @PostMapping(path = "/{table}/count")
        @Operation(summary = "엔터티 수 조회")
        @Transactional
        @ResponseBody
        public long count(@PathVariable("table") String table,
                          @Schema(implementation = Object.class)
                          @RequestBody() HashMap<String, Object> jsFilter) {
            JqlRepository repository = getRepository(table);
            long count = repository.count(repository.createFilter(jsFilter));
            return count;
        }
    }

    interface ListAll extends JqlStorageController {

        @GetMapping(path = "/{table}")
        @Operation(summary = "전체 목록")
        @Transactional
        @ResponseBody
        default JqlQuery.Response list(@PathVariable("table") String table,
                                       @RequestParam(value = "select", required = false) String select$) throws Exception {
            JqlRepository repository = getRepository(table);
            JqlSelect select = JqlSelect.of(select$);
            return JqlQuery.of(repository, select, null).execute();
        }
    }


    interface Insert extends JqlStorageController {

        @PutMapping(path = "/{table}", consumes = {MediaType.APPLICATION_JSON_VALUE})
        @Operation(summary = "엔터티 추가")
        @Transactional
        @ResponseBody
        default <ENTITY> ENTITY add(@PathVariable("table") String table,
                           @Schema(implementation = Object.class)
                           @RequestBody Map<String, Object> properties) throws Exception {
            JqlRepository repository = getRepository(table);
            Object id = repository.insert(properties);
            return (ENTITY)repository.find(id);
        }
    }

    interface Update extends JqlStorageController {

        @PatchMapping(path = "/{table}/{idList}", consumes = {MediaType.APPLICATION_JSON_VALUE})
        @Operation(summary = "엔터티 내용 변경")
        @Transactional
        @ResponseBody
        default <ENTITY, ID> Collection<ENTITY> update(@PathVariable("table") String table,
                               @Schema(type = "string", required = true) @PathVariable("idList") Collection<ID> idList,
                               @RequestBody Map<String, Object> properties) throws Exception {
            JqlRepository repository = getRepository(table);
            repository.update(idList, properties);
            return repository.find(idList);
        }
    }

    interface Delete extends JqlStorageController {
        @DeleteMapping("/{table}/{idList}")
        @Operation(summary = "엔터티 삭제")
        @Transactional
        @ResponseBody
        default <ID> Collection<ID> delete(@PathVariable("table") String table,
                                      @PathVariable("idList") Collection<ID> idList) {
            JqlRepository repository = getRepository(table);
            repository.delete(idList);
            return idList;
        }
    }

    class CRUD extends JqlStorageController.Search implements Insert, Update, Delete {
        public CRUD(JqlStorage storage, String tableNamePrefix) {
            super(storage, tableNamePrefix);
        }
    }

}
