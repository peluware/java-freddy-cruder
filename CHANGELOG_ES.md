# Registro de cambios

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [2.0.0] — 2026-06-26

Esta versión consolida varios cambios estructurales que venían acumulándose desde 1.x: un modelo de providers alineado con ISP, una capa completa de Criteria API para JPA y — lo más relevante — el desacoplamiento de `freddy-cruder-jpa` y `freddy-cruder-spring-data` de `omni-search`.

### Por qué 2.0.0

La motivación principal es hacer `omni-search` opcional. En 1.x, todos los providers JPA requerían un `JpaOmniSearchPredicateBuilder` en su constructor, lo que significaba que no podías usar `freddy-cruder-jpa` sin arrastrar toda la dependencia de `omni-search`, aunque no necesitaras búsqueda full-text ni filtrado RSQL.

Desde 2.0.0, `omni-search` es una integración opcional. La nueva interfaz `SearchPredicateBuilder` abstrae la construcción de predicados, y `OmniSearchPredicateAdapter` conecta la implementación existente para quienes la siguen usando. Si no la usas, puedes implementar tu propia estrategia, delegar a otra librería, o simplemente retornar `cb.conjunction()` para omitir el filtrado. Sin adaptador obligatorio, sin dependencia transitiva.

La misma filosofía aplica en `freddy-cruder-spring-data`: `SearchRepository` define el contrato de búsqueda sin atarte a ninguna librería; `OmniSearchRepository` es ahora solo una implementación posible de él.

### Cambios que rompen compatibilidad

#### `freddy-cruder-core`
- **`CrudProvider` y `OwnedCrudProvider` son ahora interfaces compuestas vacías.** Todas las operaciones se definen en providers atómicos `@FunctionalInterface`. Código que dependía de la forma monolítica debe migrar a los providers atómicos o a las variantes compuestas.

#### `freddy-cruder-jpa`
- **Todos los constructores de providers que aceptaban `JpaOmniSearchPredicateBuilder` ahora aceptan `SearchPredicateBuilder`.** Reemplaza con `OmniSearchPredicateAdapter.ofDefault()` para mantener el comportamiento, o provee tu propia implementación.
- **`JpaCrudProvider`, `FilterableJpaCrudProvider`, `FilterableOwnedJpaCrudProvider`** — firmas de constructores actualizadas.

#### `freddy-cruder-spring-data`
- **`SpringCrudProvider` y `SpringOwnedCrudProvider` eliminados.** Estas interfaces exponían un método default `page(Pageable)` que llamaba a `SpringDataAdapters.page(this, ...)` internamente. En la práctica este patrón era frecuentemente problemático: llamar a `this.page(...)` desde dentro de una subclase concreta del provider saltaba el proxy de Spring, por lo que cualquier advice AOP (`@Transactional`, `@Cacheable`, etc.) no aplicaba silenciosamente. La paginación ahora se maneja a nivel de controller mediante `PeluwareToSpringAdapters.page(provider, ...)`, donde la llamada pasa por el proxy correctamente.
- **`SpringEntityCrudProvider` y `SpringOwnedEntityCrudProvider` eliminados.** Extiende `EntityCrudProvider` / `OwnedEntityCrudProvider` directamente.
- **`SpringDataAdapters` eliminado.** Reemplazado por `SpringToPeluwareAdapters` y `PeluwareToSpringAdapters`.

### Añadido

#### `freddy-cruder-core`
- 14 providers atómicos `@FunctionalInterface`: `PageProvider`, `FindProvider`, `CountProvider`, `ExistsProvider`, `CreateProvider`, `UpdateProvider`, `DeleteProvider` — y sus contrapartes `Owned*`.
- 4 interfaces compuestas: `ReadProvider`, `WriteProvider`, `OwnedReadProvider`, `OwnedWriteProvider`.
- Record `OwnedId<OWNER_ID, ID>` — identificador compuesto para subrecursos con dueño, con `toString()` = `"ownerId/id"`.

#### `freddy-cruder-jpa`
- `FilterableOwnedJpaCrudProvider` — implementación JPA de `OwnedEntityCrudProvider` via Criteria API, con hooks `buildOwnerPredicate`, `predicateFilter`, `buildSearchPredicate`, `buildIdPredicate`, `getQueryHints()` y `runQuery`.
- `SearchPredicateBuilder` — `@FunctionalInterface` que desacopla los providers JPA de cualquier librería de búsqueda.
- `OmniSearchPredicateAdapter` — conecta `JpaOmniSearchPredicateBuilder` con `SearchPredicateBuilder`. Comportamiento existente preservado via `ofDefault()`.
- `JpaQueryHelpers` — clase utilitaria que elimina el boilerplate de Criteria API (lista, resultado único, conteo, existencia).
- `JpaCriteriaCallback` — `@FunctionalInterface` para construcción de predicados: `(Root<E>, CriteriaBuilder) → Predicate`.
- `JpaUtils.requireTransaction(EntityManager, Supplier)` — nueva sobrecarga con compatibilidad JTA.

#### `freddy-cruder-spring-data`
- `SpringToPeluwareAdapters` — conversiones Spring → peluware: `toPagination`, `toSort`, `toOrders`, `toPage`, `applyAsPage`.
- `PeluwareToSpringAdapters` — conversiones peluware → Spring y helpers de ejecución: `toSort`, `toPageable`, `toPage`, `apply`, `applyAsPage`, `page`.
- `SearchRepository` — abstracción de búsqueda con delegación mutua entre `findBySearch(Pagination, Sort)` y `findBySearch(Pageable)`.
- `OmniSearchRepository` — implementación de `SearchRepository` respaldada por `omni-search`.

### Guía de migración

**Providers JPA:**
```java
// Antes
new MyProvider(entityManager, jpaOmniSearchPredicateBuilder);

// Después — mismo comportamiento sin cambios
new MyProvider(entityManager, OmniSearchPredicateAdapter.ofDefault());

// O sin omni-search
new MyProvider(entityManager, (root, cb, metamodel, search, query) -> cb.conjunction());
```

**`SpringDataAdapters`:**

| Antes | Después |
|---|---|
| `SpringDataAdapters.toPeluwarePagination(p)` | `SpringToPeluwareAdapters.toPagination(p)` |
| `SpringDataAdapters.toPeluwareSort(s)` | `SpringToPeluwareAdapters.toSort(s)` |
| `SpringDataAdapters.toPeluwareOrders(s)` | `SpringToPeluwareAdapters.toOrders(s)` |
| `SpringDataAdapters.toSpringSort(s)` | `PeluwareToSpringAdapters.toSort(s)` |
| `SpringDataAdapters.toSpringPageable(p, s)` | `PeluwareToSpringAdapters.toPageable(p, s)` |
| `SpringDataAdapters.withSpringPageable(p, fn)` | `PeluwareToSpringAdapters.apply(p, fn)` |
| `SpringDataAdapters.page(provider, ...)` | `PeluwareToSpringAdapters.page(provider, ...)` |

**Clases Spring eliminadas:**

| Antes | Después |
|---|---|
| `extends SpringEntityCrudProvider<...>` | `extends EntityCrudProvider<...>` |
| `extends SpringOwnedEntityCrudProvider<...>` | `extends OwnedEntityCrudProvider<...>` |
| `implements SpringCrudProvider<...>` | `PeluwareToSpringAdapters.page(provider, ...)` en el controller |
| `implements SpringOwnedCrudProvider<...>` | `PeluwareToSpringAdapters.page(provider, ownerId, ...)` en el controller |

---

## [1.x]

Ver el [historial de commits](https://github.com/PeluWare/freddy-cruder/commits/main) para cambios anteriores a 2.0.0.
