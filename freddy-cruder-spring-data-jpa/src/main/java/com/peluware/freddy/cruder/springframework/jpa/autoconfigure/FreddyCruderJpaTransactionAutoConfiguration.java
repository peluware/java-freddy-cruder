package com.peluware.freddy.cruder.springframework.jpa.autoconfigure;

import com.peluware.freddy.cruder.jpa.JpaTransactionRunners;
import com.peluware.freddy.cruder.springframework.jpa.SpringJpaTransactionRunner;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Installs a {@link SpringJpaTransactionRunner}, backed by the application's
 * {@link PlatformTransactionManager}, into {@link JpaTransactionRunners} once every bean is ready.
 */
@AutoConfiguration
@ConditionalOnClass(EntityManager.class)
public class FreddyCruderJpaTransactionAutoConfiguration {

    @Bean
    SmartInitializingSingleton freddyCruderJpaTransactionRunnerInstaller(PlatformTransactionManager transactionManager) {
        return () -> JpaTransactionRunners.install(new SpringJpaTransactionRunner(transactionManager));
    }
}
