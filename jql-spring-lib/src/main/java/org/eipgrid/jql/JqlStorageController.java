package org.eipgrid.jql;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.eipgrid.jql.js.JsUtil;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface JqlStorageController extends JqlRestApi {

    JqlRepository getRepository(String tableName);

    class Search implements JqlStorageController {
        private final JqlStorage storage;
        private final String tableNamePrefix;
        private final ConversionService conversionService;

        public Search(JqlStorage storage, String tableNamePrefix, ConversionService conversionService) {
            this.storage = storage;
            this.tableNamePrefix = tableNamePrefix == null ? "" : tableNamePrefix;
            this.conversionService = conversionService;
        }

        public final JqlStorage getStorage() {
            return storage;
        }

        public JqlRepository getRepository(String tableName) {
            String tablePath = tableNamePrefix + tableName;
            return storage.getRepository(tablePath);
        }

        @GetMapping(path = "/")
        @Operation(summary = "Table 목록")
        @ResponseBody
        public List<String> listTableNames() {
            int p = tableNamePrefix.indexOf('.');
            String namespace = p <= 0 ? null : tableNamePrefix.substring(0, p);
            List<String> tableNames = storage.getTableNames(namespace);
            return tableNames;
        }

        @GetMapping(path = "/{table}/{id}")
        @Operation(summary = "지정 엔터티 읽기")
        @Transactional
        @ResponseBody
        public Response get(
                @PathVariable("table") String table,
                @Schema(implementation = String.class)
                @PathVariable("id") Object id,
                @RequestParam(value = "select", required = false) String select$) {
            JqlRepository repository = getRepository(table);
            JqlSelect select = JqlSelect.of(select$);
            Object res = repository.find(id, select);
            if (res == null) {
                throw new HttpServerErrorException("Entity(" + id + ") is not found", HttpStatus.NOT_FOUND, null, null, null, null);
            }
            return Response.of(res, select);
        }

        @PostMapping(path = "/{table}/find", consumes = {MediaType.APPLICATION_JSON_VALUE})
        @Operation(summary = "엔터티 검색")
        @Transactional
        @ResponseBody
        public Response find(
                @PathVariable("table") String table,
                @RequestParam(value = "select", required = false) String select,
                @Schema(implementation = String.class)
                @RequestParam(value = "sort", required = false) String[] orders,
                @RequestParam(value = "page", required = false) Integer page,
                @RequestParam(value = "limit", required = false) Integer limit,
                @Schema(implementation = Object.class)
                @RequestBody Map<String, Object> filter) {
            JqlRepository repository = getRepository(table);
            return search(repository, select, orders, page, limit, filter);
        }

        @PostMapping(path = "/{table}/count")
        @Operation(summary = "엔터티 수 조회")
        @Transactional
        @ResponseBody
        public long count(
                @PathVariable("table") String table,
                @Schema(implementation = Object.class)
                @RequestBody HashMap<String, Object> jsFilter) {
            JqlRepository repository = getRepository(table);
            long count = repository.createQuery(jsFilter).count();
            return count;
        }

        @PostMapping("/{table}/schema")
        @ResponseBody
        @Operation(summary = "엔터티 속성 정보 요약")
        public String schema(@PathVariable("table") String table) {
            JqlRepository repository = getRepository(table);
            String schema = JsUtil.getSimpleSchema(repository.getSchema());
            return schema;
        }
    }

    interface ListAll extends JqlStorageController {

        @GetMapping(path = "/{table}")
        @Operation(summary = "전체 목록")
        @Transactional
        @ResponseBody
        default Response list(
                @PathVariable("table") String table,
                @RequestParam(value = "select", required = false) String select,
                @Schema(implementation = String.class)
                @RequestParam(value = "sort", required = false) String[] orders,
                @RequestParam(value = "page", required = false) Integer page,
                @RequestParam(value = "limit", required = false) Integer limit) throws Exception {
            JqlRepository repository = getRepository(table);
            return search(repository, select, orders, page, limit, null);
        }
    }


    interface Insert extends JqlStorageController {

        @PutMapping(path = "/{table}", consumes = {MediaType.APPLICATION_JSON_VALUE})
        @Operation(summary = "엔터티 추가")
        @Transactional
        @ResponseBody
        default <ENTITY> ENTITY add(
                @PathVariable("table") String table,
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
        default <ENTITY, ID> Collection<ENTITY> update(
                @PathVariable("table") String table,
                @Schema(type = "string", required = true)
                @PathVariable("idList") Collection<ID> idList,
                @RequestParam(value = "select", required = false) String select$,
                @Schema(implementation = Object.class)
                @RequestBody Map<String, Object> properties) throws Exception {
            JqlSelect select = JqlSelect.of(select$);
            JqlRepository repository = getRepository(table);
            repository.update(idList, properties);
            List<ENTITY> res = repository.find(idList, select);
            return (Collection<ENTITY>) Response.of(res, select);
        }
    }

    interface Delete extends JqlStorageController {
        @DeleteMapping("/{table}/{idList}")
        @Operation(summary = "엔터티 삭제")
        @Transactional
        @ResponseBody
        default <ID> Collection<ID> delete(
                @PathVariable("table") String table,
                @Schema(implementation = String.class)
                @PathVariable("idList") Collection<ID> idList) {
            JqlRepository repository = getRepository(table);
            repository.delete(idList);
            return idList;
        }
    }

    class CRUD extends JqlStorageController.Search implements Insert, Update, Delete {
        public CRUD(JqlStorage storage, String tableNamePrefix, ConversionService conversionService) {
            super(storage, tableNamePrefix, conversionService);
        }
    }

}
