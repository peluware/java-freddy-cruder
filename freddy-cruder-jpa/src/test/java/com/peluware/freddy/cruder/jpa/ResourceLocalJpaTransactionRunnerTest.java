package com.peluware.freddy.cruder.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica quién abre, confirma y revierte la transacción, sin base de datos: el
 * {@link EntityManager} y su {@link EntityTransaction} son dobles que anotan lo que se les pide.
 */
class ResourceLocalJpaTransactionRunnerTest {

    private final JpaTransactionRunner runner = JpaTransactionRunner.resourceLocal();
    private final List<String> calls = new ArrayList<>();

    @AfterEach
    void restoreDefaultRunner() {
        JpaTransactionRunners.install(JpaTransactionRunner.resourceLocal());
    }

    @Test
    @DisplayName("con el EntityManager ya unido a una transacción, ejecuta sin abrir otra")
    void joinedTransactionIsHonored() {
        var result = runner.run(entityManager(true, transaction(false)), () -> "ok");

        assertEquals("ok", result);
        assertIterableEquals(List.of(), calls);
    }

    @Test
    @DisplayName("sin transacción, la abre antes y la confirma después de la operación")
    void opensAndCommits() {
        var result = runner.run(entityManager(false, transaction(false)), () -> {
            calls.add("function");
            return 42;
        });

        assertEquals(42, result);
        assertIterableEquals(List.of("begin", "function", "commit"), calls);
    }

    @Test
    @DisplayName("si la operación falla, revierte y relanza la misma excepción")
    void rollsBackAndRethrows() {
        var failure = new IllegalArgumentException("boom");

        var thrown = assertThrows(IllegalArgumentException.class, () -> runner.run(entityManager(false, transaction(false)), () -> {
            throw failure;
        }));

        assertSame(failure, thrown);
        assertIterableEquals(List.of("begin", "rollback"), calls);
    }

    @Test
    @DisplayName("un IllegalStateException de la propia operación revierte y se relanza, sin volver a ejecutarla")
    void illegalStateFromTheFunctionIsNotMistakenForJta() {
        var executions = new int[]{0};

        assertThrows(IllegalStateException.class, () -> runner.run(entityManager(false, transaction(false)), () -> {
            executions[0]++;
            throw new IllegalStateException("from the function");
        }));

        assertEquals(1, executions[0]);
        assertIterableEquals(List.of("begin", "rollback"), calls);
    }

    @Test
    @DisplayName("con un EntityManager JTA, que no deja pedir la transacción, ejecuta directo")
    void jtaManagedEntityManagerRunsDirectly() {
        var result = runner.run(entityManager(false, null), () -> "jta");

        assertEquals("jta", result);
        assertIterableEquals(List.of(), calls);
    }

    @Test
    @DisplayName("una transacción ya activa no se vuelve a abrir ni se confirma: no es de este runner")
    void alreadyActiveTransactionIsLeftAlone() {
        var result = runner.run(entityManager(false, transaction(true)), () -> "ok");

        assertEquals("ok", result);
        assertIterableEquals(List.of(), calls);
    }

    @Test
    @DisplayName("en un stream la transacción sigue abierta al volver y se confirma al cerrarlo")
    void streamCommitsOnClose() {
        var stream = runner.runStream(entityManager(false, transaction(false)), () -> Stream.of(1, 2, 3));

        assertIterableEquals(List.of("begin"), calls);
        try (stream) {
            assertEquals(List.of(1, 2, 3), stream.toList());
            assertIterableEquals(List.of("begin"), calls);
        }
        assertIterableEquals(List.of("begin", "commit"), calls);
    }

    @Test
    @DisplayName("si el proveedor del stream falla antes de devolverlo, revierte")
    void streamRollsBackWhenSupplierFails() {
        assertThrows(IllegalStateException.class, () -> runner.runStream(entityManager(false, transaction(false)), () -> {
            throw new IllegalStateException("boom");
        }));

        assertIterableEquals(List.of("begin", "rollback"), calls);
    }

    @Test
    @DisplayName("un stream con el EntityManager ya unido no abre ni confirma nada")
    void streamWithJoinedTransactionTouchesNothing() {
        try (var stream = runner.runStream(entityManager(true, transaction(false)), () -> Stream.of("a"))) {
            assertEquals(List.of("a"), stream.toList());
        }

        assertIterableEquals(List.of(), calls);
    }

    @Test
    @DisplayName("el runner instalado es el que devuelve current(), y null se rechaza")
    void installReplacesTheCurrentRunner() {
        var custom = new JpaTransactionRunner() {
            @Override
            public <T> T run(EntityManager entityManager, java.util.function.Supplier<T> function) {
                return function.get();
            }

            @Override
            public <T> Stream<T> runStream(EntityManager entityManager, java.util.function.Supplier<Stream<T>> supplier) {
                return supplier.get();
            }
        };

        JpaTransactionRunners.install(custom);

        assertSame(custom, JpaTransactionRunners.installed());
        assertNotSame(JpaTransactionRunner.resourceLocal(), JpaTransactionRunners.installed());
        assertThrows(NullPointerException.class, () -> JpaTransactionRunners.install(null));
    }

    private EntityManager entityManager(boolean joined, EntityTransaction transaction) {
        InvocationHandler handler = (_, method, _) -> switch (method.getName()) {
            case "isJoinedToTransaction" -> joined;
            case "getTransaction" -> {
                if (transaction == null) {
                    throw new IllegalStateException("A JTA EntityManager cannot use getTransaction()");
                }
                yield transaction;
            }
            default -> throw new UnsupportedOperationException(method.getName());
        };
        return (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityManager.class}, handler);
    }

    private EntityTransaction transaction(boolean active) {
        var state = new boolean[]{active};
        InvocationHandler handler = (_, method, _) -> {
            switch (method.getName()) {
                case "isActive" -> {
                    return state[0];
                }
                case "begin" -> {
                    state[0] = true;
                    calls.add("begin");
                }
                case "commit" -> {
                    state[0] = false;
                    calls.add("commit");
                }
                case "rollback" -> {
                    state[0] = false;
                    calls.add("rollback");
                }
                default -> throw new UnsupportedOperationException(method.getName());
            }
            return null;
        };
        return (EntityTransaction) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{EntityTransaction.class}, handler);
    }
}
