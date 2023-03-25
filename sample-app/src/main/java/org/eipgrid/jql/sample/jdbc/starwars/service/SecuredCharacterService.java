package org.eipgrid.jql.sample.jdbc.starwars.service;

import org.eipgrid.jql.JqlEntitySet;
import org.eipgrid.jql.JqlRepository;
import org.eipgrid.jql.JqlStorage;
import org.eipgrid.jql.util.KVEntity;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

@Service
public class SecuredCharacterService {

    private final JqlRepository<Long> characterEntitySet;

    SecuredCharacterService(JqlStorage storage) {
        characterEntitySet = storage.loadRepository("starwars.character");
    }

    public JqlRepository<Long> getCharacterEntitySet() {
        return characterEntitySet;
    }

    public void deleteCharacter(Collection<Long> idList, String accessToken) {
        if ("1234".equals(accessToken)) {
            characterEntitySet.delete(idList);
        } else {
            throw new RuntimeException("Not authorized");
        }
    }

    public Map addNewCharacter(Map<String, Object> properties) {
        if (properties.get("metadata") == null) {
            properties.put("metadata", createNote());
        }
        return characterEntitySet.insert(properties);
    }

    private KVEntity createNote() {
        KVEntity entity = new KVEntity();
        entity.put("autoCreated", new Date());
        return entity;
    }

}
