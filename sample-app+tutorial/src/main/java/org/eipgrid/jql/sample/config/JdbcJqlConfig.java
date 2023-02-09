package org.eipgrid.jql.sample.config;

import org.eipgrid.jql.JqlStorage;
import org.eipgrid.jql.jdbc.JdbcStorage;
import org.eipgrid.jql.config.DefaultJqlConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

@Configuration
public class JdbcJqlConfig extends DefaultJqlConfig {

    @Bean
    public JqlStorage jdbcStorage(DataSource dataSource, TransactionTemplate transactionTemplate,
                                  ConversionService conversionService,
                                  EntityManager entityManager) throws Exception {
        JdbcStorage storage = new JdbcStorage(dataSource, transactionTemplate,
                conversionService, entityManager);
        return storage;
    }
}
