package org.eipgrid.jql.sample.jpa.starwars_jpa.service;

import org.eipgrid.jql.JqlStorage;
import org.eipgrid.jql.jdbc.JdbcStorage;
import org.eipgrid.jql.sample.jpa.starwars_jpa.model.Character;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class StarWarsJpaService  {

    private final JdbcStorage storage;

    public StarWarsJpaService(JdbcStorage storage) {
        this.storage = storage;
    }

    public JqlStorage getStorage() {
        return this.storage;
    }

    @PostConstruct
    void initData() throws IOException {
        long cntCharacter = storage.loadJpaTable(Character.class).count(null);
        if (cntCharacter == 0) {
            loadData();
        }
    }
    public void loadData() throws IOException {
        String dbType = storage.getDbType();
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
