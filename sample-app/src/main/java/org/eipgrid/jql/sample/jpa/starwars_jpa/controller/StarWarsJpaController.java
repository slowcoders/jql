package org.eipgrid.jql.sample.jpa.starwars_jpa.controller;

import org.eipgrid.jql.JqlQuery;
import org.eipgrid.jql.JqlEntitySet;
import org.eipgrid.jql.JqlStorageController;
import org.eipgrid.jql.sample.jpa.starwars_jpa.StarWarsJpaService;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/jql/starwars_jpa")
public class StarWarsJpaController extends JqlStorageController.CRUD implements JqlStorageController.ListAll {

    private final StarWarsJpaService service;

    public StarWarsJpaController(StarWarsJpaService service, ConversionService conversionService) {
        super(service.getStorage(), "starwars_jpa.", conversionService);
        this.service = service;
    }

    @Override
    public JqlQuery.Response find(@PathVariable("table") String table,
                                  @RequestBody JqlQuery.Request request) {
        JqlEntitySet repository = getRepository(table);
        JqlQuery query = request.buildQuery(repository);
        JqlQuery.Response resp = query.execute();
        resp.setProperty("lastExecutedSql", query.getExecutedQuery());
        return resp;
    }

    @GetMapping("/loadData")
    public void loadData() throws IOException {
        service.loadData();
    }
}
