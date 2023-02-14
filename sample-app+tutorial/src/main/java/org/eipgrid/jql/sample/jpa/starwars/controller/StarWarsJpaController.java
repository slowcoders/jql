package org.eipgrid.jql.sample.jpa.starwars.controller;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.io.IOUtils;
import org.eipgrid.jql.JqlQuery;
import org.eipgrid.jql.JqlRepository;
import org.eipgrid.jql.JqlStorage;
import org.eipgrid.jql.JqlStorageController;
import org.eipgrid.jql.jdbc.JdbcStorage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@RestController
@RequestMapping("/api/jql/starwars_jpa")
public class StarWarsJpaController extends JqlStorageController.CRUD implements JqlStorageController.ListAll {

    public StarWarsJpaController(JqlStorage storage) {
        super(storage, "starwars_jpa.");
    }

    @Override
    public JqlQuery.Response find(@PathVariable("table") String table,
                                  @RequestBody JqlQuery.Request request) {
        JqlRepository repository = getRepository(table);
        JqlQuery query = request.buildQuery(repository);
        JqlQuery.Response resp = query.execute();
        resp.setProperty("lastExecutedSql", query.getExecutedQuery());
        return resp;
    }

    @GetMapping("/{dbType}/loadData")
    public void loadData(
            @Schema(allowableValues = {"postgresql", "mysql"})
            @PathVariable String dbType) throws IOException {
        JdbcStorage storage = (JdbcStorage) getStorage();
        ClassPathResource resource = new ClassPathResource("db/" + dbType + "/starwars_jpa-data.sql");
        BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()));
        StringBuilder sql = new StringBuilder();
        for (String s = null; (s = br.readLine()) != null; ) {
            sql.append(s);
            if (s.trim().endsWith(";")) {
                storage.getJdbcTemplate().update(sql.toString());
                sql.setLength(0);
            }
        }
    }
}
