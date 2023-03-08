package org.eipgrid.jql.sample.jpa.starwars_jpa.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import org.eipgrid.jql.JqlStorageController;
import org.eipgrid.jql.sample.jpa.starwars_jpa.service.StarWarsJpaService;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/jql/starwars_jpa")
public class StarWarsJpaController extends JqlStorageController.CRUD implements JqlStorageController.ListAll {

    private final StarWarsJpaService service;

    public StarWarsJpaController(StarWarsJpaService service, ConversionService conversionService) {
        super(service.getStorage(), "starwars_jpa.", conversionService);
        this.service = service;
    }

    @Override
    public Response find(
            @PathVariable("table") String table,
            @RequestParam(value = "select", required = false) String select,
            @Schema(implementation = String.class)
            @RequestParam(value = "sort", required = false) String[] orders,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit,
            @Schema(implementation = Object.class)
            @RequestBody Map<String, Object> filter) {
        Response resp = super.find(table, select, orders, page, limit, filter);
        resp.setProperty("lastExecutedSql", resp.getQuery().getExecutedQuery());
        return resp;
    }

    @GetMapping("/loadData")
    public void loadData() throws IOException {
        service.loadData();
    }
}
