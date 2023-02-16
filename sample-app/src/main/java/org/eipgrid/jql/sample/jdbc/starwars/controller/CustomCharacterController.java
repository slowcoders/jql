package org.eipgrid.jql.sample.jdbc.starwars.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.eipgrid.jql.JqlStorage;
import org.eipgrid.jql.JqlTableController;
import org.eipgrid.jql.JqlQuery;
import org.eipgrid.jql.util.KVEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/jql/starwars/character")
public class CustomCharacterController extends JqlTableController.CRUD<Integer> implements JqlTableController.ListAll<Integer> {

    public CustomCharacterController(JqlStorage storage) {
        super(storage.getRepository("starwars.character"));
    }



    @Override
    public JqlQuery.Response find(@Schema(example = "{ \"select\": \"\", \"sort\": \"\", \"page\": 0, \"limit\": 0, \"filter\": { } }")
                                  @RequestBody JqlQuery.Request request) {
        JqlQuery query = request.buildQuery(getTable());
        JqlQuery.Response resp = query.execute();
        resp.setProperty("lastExecutedSql", query.getExecutedQuery());
        return resp;
    }

    /**
     * Custom
     * @param idList
     * @return
     */
    @Override
    @Operation(summary = "엔터티 삭제 (사용 금지됨. secure-delete API 로 대체)")
    @Transactional
    public Collection<Integer> delete(@PathVariable("idList") Collection<Integer> idList) {
        throw new RuntimeException("Delete is forbidden by custom controller. Use ControlApiCustomizingDemoController.secure-delete api");
    }

    @PostMapping("/secure-delete/{idList}")
    @ResponseBody
    @Operation(summary = "엔터티 삭제 (권한 검사 기능 추가. 테스트용 AccessToken 값='1234')")
    @Transactional
    public Collection<Integer> delete(@PathVariable("idList") Collection<Integer> idList,
                                     @RequestParam String accessToken) {
        if ("1234".equals(accessToken)) {
            return super.delete(idList);
        } else {
            throw new RuntimeException("Not authorized");
        }
    }

    @Override
    @Operation(summary = "엔터티 추가 (default 값 설정 기능 추가)")
    @Transactional
    public <ENTITY> ENTITY add(@RequestBody Map<String, Object> properties) throws Exception {
        if (properties.get("metadata") == null) {
            properties.put("metadata", createNote());
        }
        return super.add(properties);
    }

    public <T> Collection<T> update(
            @Schema(type = "string", required = true) @PathVariable("idList") Collection<Integer> idList,
            @RequestBody HashMap<String, Object> updateSet) throws Exception {
        return super.update(idList, updateSet);
    }

    private KVEntity createNote() {
        KVEntity entity = new KVEntity();
        entity.put("autoCreated", new Date());
        return entity;
    }

}
