package com.peluware.freddy.cruder.springframework.mongodb.autoconfigure;

import com.peluware.freddy.cruder.mongodb.OmniSearchFilterAdapter;
import com.peluware.freddy.cruder.mongodb.SearchFilterBuilder;
import com.peluware.freddy.cruder.springframework.mongodb.MongoSearchEngine;
import com.peluware.omnisearch.mongodb.DefaultMongoOmniSearchFilterBuilder;
import com.peluware.omnisearch.mongodb.MongoOmniSearch;
import com.peluware.omnisearch.mongodb.MongoOmniSearchFilterBuilder;
import com.peluware.omnisearch.mongodb.mapping.SpringDataMongoMappingProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoOperations;

@AutoConfiguration
@ConditionalOnClass(MongoOperations.class)
public class FreddyCruderMongoSearchAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MongoOmniSearchFilterBuilder.class)
    static class OmniSearchConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public MongoOmniSearchFilterBuilder mongoOmniSearchFilterBuilder() {
            return new DefaultMongoOmniSearchFilterBuilder();
        }

        @Bean
        @ConditionalOnMissingBean
        public MongoOmniSearch mongoOmniSearch(MongoDatabaseFactory mongoDatabaseFactory, MongoOmniSearchFilterBuilder mongoOmniSearchFilterBuilder) {
            var mongoDatabase = mongoDatabaseFactory.getMongoDatabase();
            return new MongoOmniSearch(mongoDatabase, mongoOmniSearchFilterBuilder, new SpringDataMongoMappingProvider());
        }


        @Bean
        @ConditionalOnMissingBean
        public SearchFilterBuilder searchFilterBuilder(MongoOmniSearchFilterBuilder mongoOmniSearchFilterBuilder) {
            return new OmniSearchFilterAdapter(mongoOmniSearchFilterBuilder);
        }
    }

    @Bean
    @ConditionalOnMissingBean(MongoSearchEngine.class)
    MongoSearchEngine mongoSearchRepositoryEngine(MongoOperations mongoOperations, SearchFilterBuilder searchFilterBuilder) {
        return new MongoSearchEngine(mongoOperations, searchFilterBuilder);
    }
}
