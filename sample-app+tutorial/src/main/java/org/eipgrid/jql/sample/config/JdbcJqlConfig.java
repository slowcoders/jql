package org.eipgrid.jql.sample.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eipgrid.jql.config.DefaultJqlConfig;
import org.eipgrid.jql.jdbc.JdbcStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

@Configuration
public class JdbcJqlConfig extends DefaultJqlConfig {

    @Bean
    public JdbcStorage jdbcStorage(DataSource dataSource, TransactionTemplate transactionTemplate,
                                  ObjectMapper objectMapper,
                                  EntityManager entityManager) throws Exception {
        JdbcStorage storage = new JdbcStorage(dataSource, transactionTemplate,
                objectMapper, entityManager);
        return storage;
    }
}
