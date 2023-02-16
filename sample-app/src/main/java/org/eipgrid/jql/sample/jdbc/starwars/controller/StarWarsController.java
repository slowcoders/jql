package org.eipgrid.jql.sample.jdbc.starwars.controller;

import org.eipgrid.jql.JqlStorage;
import org.eipgrid.jql.JqlStorageController;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jql/starwars")
public class StarWarsController extends JqlStorageController.CRUD implements JqlStorageController.ListAll {

    public StarWarsController(JqlStorage storage, ConversionService conversionService) {
        super(storage, "starwars.", conversionService);
    }

}
