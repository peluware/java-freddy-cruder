package com.peluware.freddy.cruder.springframework.jpa;

import com.peluware.freddy.cruder.jpa.JpaTransactionRunner;
import com.peluware.freddy.cruder.jpa.JpaTransactionRunners;
import com.peluware.freddy.cruder.springframework.jpa.autoconfigure.FreddyCruderJpaTransactionAutoConfiguration;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpringJpaTransactionRunnerTest {

    private final RecordingTransactionManager transactionManager = new RecordingTransactionManager();
    private final SpringJpaTransactionRunner runner = new SpringJpaTransactionRunner(transactionManager);

    @AfterEach
    void restoreDefaultRunner() {
        JpaTransactionRunners.install(JpaTransactionRunner.resourceLocal());
    }

    @Test
    @DisplayName("con el EntityManager ya unido a una transacción, ejecuta sin pedirle nada a Spring")
    void joinedTransactionIsHonored() {
        var result = runner.run(entityManager(true), () -> "ok");

        assertEquals("ok", result);
        assertIterableEquals(List.of(), transactionManager.calls);
    }

    @Test
    @DisplayName("sin transacción, abre una de Spring y la confirma al terminar")
    void opensAndCommits() {
        var result = runner.run(entityManager(false), () -> {
            transactionManager.calls.add("function");
            return 7;
        });

        assertEquals(7, result);
        assertIterableEquals(List.of("begin", "function", "commit"), transactionManager.calls);
    }

    @Test
    @DisplayName("si la operación falla, Spring revierte y la excepción llega intacta")
    void rollsBackOnFailure() {
        var failure = new IllegalArgumentException("boom");

        var thrown = assertThrows(IllegalArgumentException.class, () -> runner.run(entityManager(false), () -> {
            throw failure;
        }));

        assertSame(failure, thrown);
        assertIterableEquals(List.of("begin", "rollback"), transactionManager.calls);
    }

    @Test
    @DisplayName("en un stream la transacción se confirma al cerrarlo, no al volver")
    void streamCommitsOnClose() {
        var stream = runner.runStream(entityManager(false), () -> Stream.of(1, 2));

        assertIterableEquals(List.of("begin"), transactionManager.calls);
        try (stream) {
            assertEquals(List.of(1, 2), stream.toList());
        }
        assertIterableEquals(List.of("begin", "commit"), transactionManager.calls);
    }

    @Test
    @DisplayName("si el proveedor del stream falla antes de devolverlo, revierte")
    void streamRollsBackWhenSupplierFails() {
        assertThrows(IllegalStateException.class, () -> runner.runStream(entityManager(false), () -> {
            throw new IllegalStateException("boom");
        }));

        assertIterableEquals(List.of("begin", "rollback"), transactionManager.calls);
    }

    @Test
    @DisplayName("un stream con el EntityManager ya unido no abre ni confirma nada")
    void streamWithJoinedTransactionTouchesNothing() {
        try (var stream = runner.runStream(entityManager(true), () -> Stream.of("a"))) {
            assertEquals(List.of("a"), stream.toList());
        }

        assertIterableEquals(List.of(), transactionManager.calls);
    }

    @Test
    @DisplayName("la autoconfiguración instala un runner de Spring cuando todos los beans están listos")
    void autoConfigurationInstallsTheSpringRunner() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FreddyCruderJpaTransactionAutoConfiguration.class))
            .withBean(PlatformTransactionManager.class, () -> transactionManager)
            .run(_ -> assertInstanceOf(SpringJpaTransactionRunner.class, JpaTransactionRunners.installed()));
    }

    private static EntityManager entityManager(boolean joined) {
        return (EntityManager) Proxy.newProxyInstance(
            SpringJpaTransactionRunnerTest.class.getClassLoader(),
            new Class<?>[]{EntityManager.class},
            (_, method, _) -> {
                if (method.getName().equals("isJoinedToTransaction")) {
                    return joined;
                }
                throw new UnsupportedOperationException(method.getName());
            }
        );
    }

    private static final class RecordingTransactionManager implements PlatformTransactionManager {

        private final List<String> calls = new ArrayList<>();

        @Override
        public TransactionStatus getTransaction(@Nullable TransactionDefinition definition) {
            calls.add("begin");
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            calls.add("commit");
        }

        @Override
        public void rollback(TransactionStatus status) {
            calls.add("rollback");
        }
    }
}
