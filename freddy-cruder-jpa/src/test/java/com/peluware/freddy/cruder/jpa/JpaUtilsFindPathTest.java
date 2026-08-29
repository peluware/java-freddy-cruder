package com.peluware.freddy.cruder.jpa;

import com.peluware.freddy.cruder.jpa.fixtures.Category;
import com.peluware.freddy.cruder.jpa.fixtures.LegacyCode;
import com.peluware.freddy.cruder.jpa.fixtures.Product;
import com.peluware.freddy.cruder.jpa.fixtures.Region;
import com.peluware.freddy.cruder.jpa.fixtures.Tag;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Metamodel;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

/**
 * Verifica la forma del SQL que produce {@link JpaUtils#findPath}, no su valor de retorno: lo que
 * está en juego es si une una tabla o lee la clave foránea que ya tiene al lado.
 */
@Testcontainers
class JpaUtilsFindPathTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15");

    static SessionFactory sessionFactory;
    static Metamodel metamodel;

    @BeforeAll
    static void bootstrap() {
        sessionFactory = new Configuration()
            .addAnnotatedClass(Product.class)
            .addAnnotatedClass(Category.class)
            .addAnnotatedClass(Tag.class)
            .addAnnotatedClass(LegacyCode.class)
            .addAnnotatedClass(Region.class)
            .setProperty("jakarta.persistence.jdbc.url", POSTGRES.getJdbcUrl())
            .setProperty("jakarta.persistence.jdbc.user", POSTGRES.getUsername())
            .setProperty("jakarta.persistence.jdbc.password", POSTGRES.getPassword())
            .setProperty("hibernate.hbm2ddl.auto", "create-drop")
            .setProperty("hibernate.session_factory.statement_inspector", SqlCollector.class.getName())
            .buildSessionFactory();

        metamodel = sessionFactory.getMetamodel();

        sessionFactory.inTransaction(session -> {
            var tools = new Category();
            tools.id = 1L;
            tools.name = "Herramientas";
            session.persist(tools);

            var withCategory = new Product();
            withCategory.id = 1L;
            withCategory.name = "Martillo";
            withCategory.category = tools;
            session.persist(withCategory);

            var withoutCategory = new Product();
            withoutCategory.id = 2L;
            withoutCategory.name = "Suelto";
            session.persist(withoutCategory);
        });
    }

    @AfterAll
    static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    @DisplayName("una asociación a-uno seguida de su id no une nada: la FK ya está en la fila")
    void toOneIdIsDereferenced() {
        assertEquals(0, joinsFor("category.id"));
    }

    @Test
    @DisplayName("si el último segmento no es el id, la columna vive en la otra tabla y hay que unir")
    void toOneNonIdStillJoins() {
        assertEquals(1, joinsFor("category.name"));
    }

    @Test
    @DisplayName("en una ruta profunda solo se une lo intermedio; el id final se navega")
    void nestedToOneIdJoinsOnlyTheIntermediateStep() {
        assertEquals(1, joinsFor("category.parent.id"));
    }

    @Test
    @DisplayName("una colección se une siempre: no hay FK en la fila propietaria que leer")
    void collectionAlwaysJoins() {
        assertEquals(1, joinsFor("tags.id"));
    }

    @Test
    @DisplayName("una ruta de un solo segmento sigue uniendo, como antes del atajo")
    void bareAssociationStillJoins() {
        assertEquals(1, joinsFor("category"));
    }

    @Test
    @DisplayName("con @IdClass no hay atributo identificador único, así que se une")
    void idClassTargetStillJoins() {
        assertEquals(1, joinsFor("legacyCode.code"));
    }

    @Test
    @DisplayName("con @EmbeddedId sí hay atributo único, y el atajo resuelve contra la FK compuesta")
    void embeddedIdIsDereferenced() {
        assertEquals(0, joinsFor("region.id"));
    }

    @Test
    @DisplayName("sin join, un INNER por defecto ya no puede borrar las filas con la asociación nula")
    void nullAssociationSurvivesTheInnerDefault() {
        var orphans = sessionFactory.fromTransaction(session -> {
            var cb = session.getCriteriaBuilder();
            var cq = cb.createQuery(String.class);
            var root = cq.from(Product.class);
            cq.select(root.get("name"))
                .where(cb.isNull(JpaUtils.findPath("category.id", root, metamodel, JoinType.INNER)));
            return session.createQuery(cq).getResultList();
        });

        assertIterableEquals(List.of("Suelto"), orphans);
    }

    private static int joinsFor(String path) {
        return countJoins(sqlFor(root -> JpaUtils.findPath(path, root, metamodel, JoinType.INNER)));
    }

    private static String sqlFor(Function<Root<Product>, Path<?>> resolver) {
        SqlCollector.clear();
        sessionFactory.inTransaction(session -> {
            var cb = session.getCriteriaBuilder();
            var cq = cb.createQuery(Product.class);
            var root = cq.from(Product.class);
            cq.select(root).where(cb.isNotNull(resolver.apply(root)));
            session.createQuery(cq).getResultList();
        });
        return SqlCollector.lastSelect();
    }

    private static int countJoins(String sql) {
        return sql.toLowerCase().split("\\bjoin\\b", -1).length - 1;
    }
}
