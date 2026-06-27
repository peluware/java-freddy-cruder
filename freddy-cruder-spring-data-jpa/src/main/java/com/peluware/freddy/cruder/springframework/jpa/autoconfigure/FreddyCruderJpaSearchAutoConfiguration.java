package com.peluware.freddy.cruder.springframework.jpa.autoconfigure;

import com.peluware.freddy.cruder.jpa.OmniSearchPredicateAdapter;
import com.peluware.freddy.cruder.jpa.SearchPredicateBuilder;
import com.peluware.freddy.cruder.springframework.SearchRepositoryEngine;
import com.peluware.freddy.cruder.springframework.jpa.JpaSearchRepositoryEngine;
import com.peluware.omnisearch.jpa.DefaultJpaOmniSearchPredicateBuilder;
import com.peluware.omnisearch.jpa.JpaOmniSearch;
import com.peluware.omnisearch.jpa.JpaOmniSearchPredicateBuilder;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfiguration
@ConditionalOnClass({EntityManager.class})
public class FreddyCruderJpaSearchAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(JpaOmniSearch.class)
    static class OmniSearchConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public JpaOmniSearchPredicateBuilder jpaOmniSearchPredicateBuilder() {
            return new DefaultJpaOmniSearchPredicateBuilder();
        }

        @Bean
        @ConditionalOnMissingBean
        public JpaOmniSearch jpaOmniSearch(EntityManager entityManager, JpaOmniSearchPredicateBuilder jpaOmniSearchPredicateBuilder) {
            return new JpaOmniSearch(entityManager, jpaOmniSearchPredicateBuilder);
        }

        @Bean
        @ConditionalOnMissingBean
        public SearchPredicateBuilder searchPredicateBuilder(JpaOmniSearchPredicateBuilder jpaOmniSearchPredicateBuilder) {
            return new OmniSearchPredicateAdapter(jpaOmniSearchPredicateBuilder);
        }

    }

    @Bean
    @ConditionalOnMissingBean(SearchRepositoryEngine.class)
    JpaSearchRepositoryEngine jpaSearchRepositoryEngine(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder) {
        return new JpaSearchRepositoryEngine(entityManager, searchPredicateBuilder);
    }
}
