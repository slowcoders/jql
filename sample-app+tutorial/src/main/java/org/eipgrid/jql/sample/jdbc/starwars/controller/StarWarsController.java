package org.eipgrid.jql.sample.jdbc.starwars.controller;

import org.eipgrid.jql.JqlStorage;
import org.eipgrid.jql.JqlStorageController;
import org.eipgrid.jql.jdbc.JdbcStorage;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jql/starwars")
public class StarWarsController extends JqlStorageController.CRUD implements JqlStorageController.ListAll {

    public StarWarsController(JqlStorage storage) {
        super(storage, "starwars.");
    }

}
