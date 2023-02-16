package net.ion.eip.edge.config;

import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.CacheManager;
import javax.cache.Caching;

/**
 * 참조) https://stackoverflow.com/questions/67993442/hhh90001006-missing-cachedefault-update-timestamps-region-was-created-on-the
 */

@Configuration @EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager ehCacheManager() {
        CacheManager cacheManager = Caching.getCachingProvider().getCacheManager();

        hibernateDefaultUpdateTimestampsRegionConfiguration(cacheManager);
        hibernateDefaultQueryResultsRegion(cacheManager);

        return cacheManager;
    }

    /**
     * Create Hibernate's default-update-timestamps-region cache. No expiration per Hibernate recommendation:
     * https://github.com/hibernate/hibernate-orm/blob/main/documentation/src/main/asciidoc/userguide/chapters/caching/Caching.adoc#query-cache-regions
     *
     * @param cacheManager
     */
    private void hibernateDefaultUpdateTimestampsRegionConfiguration(CacheManager cacheManager) {
        CacheConfigurationBuilder<Object, Object> builder = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(Object.class, Object.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder().offheap(1, MemoryUnit.MB));

        javax.cache.configuration.Configuration<Object, Object> cache = Eh107Configuration.fromEhcacheCacheConfiguration(builder);

        cacheManager.createCache("default-update-timestamps-region", cache);

        //cacheManager.createCache("default-update-timestamps-region", new MutableConfiguration<>());
    }

    /**
     * Create Hibernate's default-query-results-region cache.
     * https://github.com/hibernate/hibernate-orm/blob/main/documentation/src/main/asciidoc/userguide/chapters/caching/Caching.adoc#query-cache-regions
     *
     * @param cacheManager
     */
    private void hibernateDefaultQueryResultsRegion(CacheManager cacheManager) {
        CacheConfigurationBuilder<Object, Object> builder = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(Object.class, Object.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder().offheap(1, MemoryUnit.MB));

        javax.cache.configuration.Configuration<Object, Object> cache = Eh107Configuration.fromEhcacheCacheConfiguration(builder);

        cacheManager.createCache("default-query-results-region", cache);

        //cacheManager.createCache("default-query-results-region", new MutableConfiguration<>());
    }

}