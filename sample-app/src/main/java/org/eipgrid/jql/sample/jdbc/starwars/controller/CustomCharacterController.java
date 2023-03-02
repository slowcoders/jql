package org.eipgrid.jql.sample.jdbc.starwars.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.eipgrid.jql.JqlQuery;
import org.eipgrid.jql.JqlStorage;
import org.eipgrid.jql.JqlEntitySetController;
import org.eipgrid.jql.util.KVEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/jql/starwars/character")
public class CustomCharacterController extends JqlEntitySetController.CRUD<Integer> implements JqlEntitySetController.ListAll<Integer> {

    public CustomCharacterController(JqlStorage storage) {
        super(storage.getRepository("starwars.character"));
    }



    @Override
    public Response find(
            @RequestParam(value = "select", required = false) String select,
            @Schema(implementation = String.class)
            @RequestParam(value = "sort", required = false) String[] orders,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit,
            @Schema(implementation = Object.class)
            @RequestBody Map<String, Object> filter) {
        Response resp = super.find(select, orders, page, limit, filter);
        resp.setProperty("lastExecutedSql", resp.getQuery().getExecutedQuery());
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
    public <ENTITY> ENTITY add(
            @Schema(implementation = Object.class)
            @RequestBody Map<String, Object> properties) throws Exception {
        if (properties.get("metadata") == null) {
            properties.put("metadata", createNote());
        }
        return super.add(properties);
    }

    private KVEntity createNote() {
        KVEntity entity = new KVEntity();
        entity.put("autoCreated", new Date());
        return entity;
    }

}
