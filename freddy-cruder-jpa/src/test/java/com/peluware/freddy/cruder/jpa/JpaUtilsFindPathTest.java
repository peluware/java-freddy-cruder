package com.peluware.freddy.cruder.jpa;

import com.peluware.freddy.cruder.jpa.fixtures.Category;
import com.peluware.freddy.cruder.jpa.fixtures.LegacyCode;
import com.peluware.freddy.cruder.jpa.fixtures.Product;
import com.peluware.freddy.cruder.jpa.fixtures.Region;
import com.peluware.freddy.cruder.jpa.fixtures.Tag;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import com.peluware.domain.Order;
import com.peluware.freddy.cruder.jpa.query.JpaOrder;
import com.peluware.freddy.cruder.jpa.query.JpaPredicate;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica la forma del SQL que produce {@link JpaUtils#findPath}, no su valor de retorno: lo que
 * está en juego es si une una tabla o lee la clave foránea que ya tiene al lado.
 */
@Testcontainers
class JpaUtilsFindPathTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15");

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
            withCategory.active = true;
            withCategory.category = tools;
            session.persist(withCategory);

            var withoutCategory = new Product();
            withoutCategory.id = 2L;
            withoutCategory.name = "Suelto";
            withoutCategory.active = false;
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
    @DisplayName("getOrCreateJoin con atributo del metamodelo reutiliza el join existente del mismo tipo")
    void metamodelJoinIsReused() {
        var cb = sessionFactory.getCriteriaBuilder();
        var root = cb.createQuery(Product.class).from(Product.class);
        var category = metamodel.entity(Product.class).getSingularAttribute("category", Category.class);

        var first = JpaUtils.getOrCreateJoin(root, category, JoinType.LEFT);
        var second = JpaUtils.getOrCreateJoin(root, category, JoinType.LEFT);
        var inner = JpaUtils.getOrCreateJoin(root, category, JoinType.INNER);

        assertSame(first, second);
        assertNotSame(first, inner);
        assertEquals(2, root.getJoins().size());
    }

    @Test
    @DisplayName("getOrCreateJoin con un atributo Set del metamodelo reutiliza el join del mismo tipo")
    void metamodelSetJoinIsReused() {
        var cb = sessionFactory.getCriteriaBuilder();
        var root = cb.createQuery(Product.class).from(Product.class);
        var tags = metamodel.entity(Product.class).getSet("tags", Tag.class);

        var first = JpaUtils.getOrCreateJoin(root, tags, JoinType.LEFT);
        var second = JpaUtils.getOrCreateJoin(root, tags, JoinType.LEFT);
        var inner = JpaUtils.getOrCreateJoin(root, tags, JoinType.INNER);

        assertSame(first, second);
        assertNotSame(first, inner);
    }

    @Test
    @DisplayName("el join por metamodelo y el de texto sobre el mismo atributo son el mismo join")
    void metamodelAndStringJoinsShareTheJoin() {
        var cb = sessionFactory.getCriteriaBuilder();
        var root = cb.createQuery(Product.class).from(Product.class);
        var category = metamodel.entity(Product.class).getSingularAttribute("category", Category.class);

        var byText = JpaUtils.getOrCreateJoin(root, "category", JoinType.LEFT);
        var byMetamodel = JpaUtils.getOrCreateJoin(root, category, JoinType.LEFT);

        assertSame(byText, byMetamodel);
    }

    @Test
    @DisplayName("una propiedad desconocida nombra la propiedad y la entidad en el mensaje")
    void unknownPropertyMessageNamesPropertyAndEntity() {
        var cb = sessionFactory.getCriteriaBuilder();
        var root = cb.createQuery(Product.class).from(Product.class);

        var error = assertThrows(IllegalArgumentException.class, () -> JpaUtils.findPath("nope", root, metamodel));

        assertEquals("Unknown property: nope in entity " + Product.class.getName(), error.getMessage());
    }

    @Test
    @DisplayName("JpaPredicate.eq compara el atributo con el valor")
    void predicateEq() {
        assertIterableEquals(List.of("Martillo"), namesWhere(JpaPredicate.eq(productAttribute("name", String.class), "Martillo")));
    }

    @Test
    @DisplayName("JpaPredicate.eq con valor nulo busca IS NULL")
    void predicateEqNullMatchesIsNull() {
        assertIterableEquals(List.of("Suelto"), namesWhere(JpaPredicate.<Product, Category>eq(productAttribute("category", Category.class), null)));
    }

    @Test
    @DisplayName("JpaPredicate.in acepta cualquiera de los valores")
    void predicateIn() {
        assertIterableEquals(
            List.of("Martillo", "Suelto"),
            namesWhere(JpaPredicate.in(productAttribute("name", String.class), List.of("Suelto", "Martillo", "Inexistente")))
        );
    }

    @Test
    @DisplayName("JpaPredicate.in con una colección vacía no encuentra nada, en vez de no restringir")
    void predicateInEmptyMatchesNothing() {
        assertIterableEquals(List.of(), namesWhere(JpaPredicate.in(productAttribute("name", String.class), List.<String>of())));
    }

    @Test
    @DisplayName("JpaPredicate.isTrue deja solo las filas con el booleano en verdadero")
    void predicateIsTrue() {
        assertIterableEquals(List.of("Martillo"), namesWhere(JpaPredicate.isTrue(productAttribute("active", Boolean.class))));
    }

    @Test
    @DisplayName("JpaOrder.by(Collection) ordena según los órdenes de dominio, en el orden dado")
    void orderByCollection() {
        var names = sessionFactory.fromTransaction(session -> {
            var cb = session.getCriteriaBuilder();
            var cq = cb.createQuery(String.class);
            var root = cq.from(Product.class);
            var orders = JpaOrder.<Product>by(List.of(Order.descending("name")))
                .orders(root, cb, metamodel);
            cq.select(root.get("name")).orderBy(orders);
            return session.createQuery(cq).getResultList();
        });

        assertIterableEquals(List.of("Suelto", "Martillo"), names);
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

    private static <V> SingularAttribute<? super Product, V> productAttribute(String name, Class<V> type) {
        return metamodel.entity(Product.class).getSingularAttribute(name, type);
    }

    private static List<String> namesWhere(JpaPredicate<Product> predicate) {
        return sessionFactory.fromTransaction(session -> {
            var cb = session.getCriteriaBuilder();
            var cq = cb.createQuery(String.class);
            var root = cq.from(Product.class);
            var where = predicate.build(root, cb);
            cq.select(root.get("name")).orderBy(cb.asc(root.get("name")));
            if (where != null) {
                cq.where(where);
            }
            return session.createQuery(cq).getResultList();
        });
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
