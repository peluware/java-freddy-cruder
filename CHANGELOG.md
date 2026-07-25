# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [3.0.1] — 2026-07-25

### Added

#### `freddy-cruder-jpa`
- `JpaGroupBy` — new axis for the query model, building the `GROUP BY` clause. `JpaGroupBy.self()`
  groups by the query source as a whole, letting a projection select any of its columns freely
  alongside an aggregate over a joined plural association; `JpaGroupBy.none()` (the default) applies
  no grouping.
- `ListQuery`/`FindQuery` gained a `groupBy` parameter (optional — existing constructors without it
  still work), and `JpaProjectedCrudProvider`/`JpaOwnedProjectedCrudProvider` gained a `groupBy()`
  hook applied to `page`/`find`, defaulting to no grouping.

This is purely additive — every new parameter has a matching overload/default, so existing code keeps
compiling and behaving the same.

---

## [3.0.0] — 2026-07-21

This release adds MongoDB as a first-class store alongside JPA, and replaces the old ad-hoc Criteria
API helpers with a small, composable query model. Several APIs were renamed or reshaped along the
way, which is why this is a major version.

### Why 3.0.0

`freddy-cruder-jpa` grew organically around a handful of static helpers (`JpaQueryHelpers`,
`JpaCriteriaExecutor`, `JpaCriteriaCallback`). They worked, but every new query shape meant more
overloads. This release replaces them with a small set of composable pieces — a source, a selection,
a filter, an ordering, and a result — that combine into reusable, named query objects
(`CountQuery`, `ExistsQuery`, `ListQuery`, `FindQuery`, and their `Entity*` shorthands for the common
"whole entity" case). The same shape now also powers MongoDB, so both stores read the same way.

### Added

#### `freddy-cruder-mongodb` *(new module)*
- `MongoCrudProvider` / `FilterableMongoCrudProvider` / `FilterableOwnedMongoCrudProvider` — the
  MongoDB counterparts of the JPA providers, built on the MongoDB sync driver.
- `SearchFilterBuilder` — pluggable full-text/RSQL filtering strategy, with `omni-search-mongodb` as
  the optional default via `OmniSearchFilterAdapter`.

#### `freddy-cruder-spring-data-mongodb` *(new module)*
- `MongoSearchRepository` / `DefaultMongoSearchRepository` / `MongoSearchEngine` — the search
  fragment for Spring Data MongoDB repositories, autoconfigured the same way as the JPA one.

#### `freddy-cruder-jpa`
- A composable query model in the new `com.peluware.freddy.cruder.jpa.query` package: build a query
  from small independent pieces (source, selection, filter, ordering, result, hints) and run it with
  `JpaQueryExecutor`, or chain `query.exec(entityManager)` directly.
- Reusable query objects — `CountQuery`, `ExistsQuery`, `ListQuery`, `FindQuery` — plus `Entity*`
  variants for the common case of a whole entity rooted at its class.
- `JpaHints` — named constants and factories for standard Jakarta Persistence query hints (fetch/load
  graph, timeouts, cache mode), instead of hand-typed hint strings.
- `JpaProjectedCrudProvider` / `JpaOwnedProjectedCrudProvider` — read entities through a lean
  projection (e.g. a `cb.construct(...)` DTO) instead of loading the full entity, while writes still
  operate on the real entity.

#### `freddy-cruder-core`
- `afterCreate(INPUT, ENTITY)` / `afterUpdate(INPUT, ENTITY)` hooks on `EntityCrudProvider` and
  `OwnedEntityCrudProvider` (owner-scoped variants), run right after persistence with the input DTO
  and the now-persisted entity available. Useful for persisting dependent entities that need the
  parent's generated identifier and don't have a direct relationship at the store level.

### Changed

#### `freddy-cruder-spring-data`
- `SearchRepositoryEngine` renamed to `SearchEngine`.

#### `freddy-cruder-spring-data-jpa`
- `JpaSearchRepositoryEngine` renamed to `JpaSearchEngine`.

#### `freddy-cruder-jpa`
- `buildIdPredicate` and `buildOwnerPredicate` now build a reusable `JpaPredicate` instead of a raw
  Criteria `Predicate`, and take just the id/owner value — the entity source is supplied by the query
  that uses them, not passed in by the caller.

### Removed

#### `freddy-cruder-jpa`
- `JpaQueryHelpers`, `JpaCriteriaExecutor`, `JpaCriteriaCallback` — replaced by the query model above.
- The `runQuery` hook on `FilterableOwnedJpaCrudProvider` — no longer needed with the new query model.

### Migration

**Renamed engines:**

| Before | After |
|---|---|
| `SearchRepositoryEngine` | `SearchEngine` |
| `JpaSearchRepositoryEngine` | `JpaSearchEngine` |

**Custom predicate overrides**, if you overrode `buildIdPredicate` or `buildOwnerPredicate`:
```java
// Before
protected Predicate buildIdPredicate(Root<ENTITY> root, CriteriaBuilder cb, ID id) {
    return cb.equal(root.get("id"), id);
}

// After
protected JpaPredicate<ENTITY> buildIdPredicate(ID id) {
    return (from, cb) -> cb.equal(from.get("id"), id);
}
```

**Custom Criteria queries**, if you used `JpaQueryHelpers`/`JpaCriteriaExecutor` directly:
```java
// Before
JpaQueryHelpers.query(entityManager, Product.class, Product.class, filter, JpaCriteriaExecutor.list(sort, pagination));

// After
new EntityListQuery<>(Product.class, filter, sort, pagination).exec(entityManager);
```

---

## [2.1.0] — 2026-06-26

### Added

#### `freddy-cruder-spring-data-jpa` *(new module)*
- `JpaSearchRepository<T>` — fragment interface for JPA-backed repositories. Extend it alongside `JpaRepository` and the search fragment is wired automatically via `spring.factories`.
- `DefaultJpaSearchRepository<T>` — fragment implementation backed by `JpaSearchRepositoryEngine`. Implements `RepositoryMetadataAccess` so `RepositoryMethodContext` is available during execution.
- `JpaSearchRepositoryEngine` — `SearchRepositoryEngine` implementation using JPA Criteria API. Delegates predicate construction to `SearchPredicateBuilder`.
- `FreddyCruderJpaSearchAutoConfiguration` — autoconfigures `JpaSearchRepositoryEngine` and, when `omni-search-jpa` is on the classpath, `JpaOmniSearchPredicateBuilder`, `JpaOmniSearch`, and `OmniSearchPredicateAdapter` as a `SearchPredicateBuilder`.

#### `freddy-cruder-spring-data`
- `SpringRepositoryCrudProvider` now exposes intersection-type constructors `<R extends CrudRepository<E,ID> & SearchRepository<E>>`. Pass a single repository that satisfies both contracts without needing a named wrapper interface.
- `SpringPage<T>` — internal bridge type that extends Peluware `Page<T>` while retaining the original Spring `Page<T>`. Eliminates the `SpringPage → PeluwarePage → SpringPage` round trip when `PageController` and `SpringRepositoryCrudProvider` are used together.

### Removed

#### `freddy-cruder-spring-data`
- **`CrudSearchRepository<ENTITY, ID>`** — removed. Spring Data's fragment mechanism only scans direct, non-`@NoRepositoryBean` interfaces of a concrete repository; this interface was never reachable by that scan and therefore never functional as a fragment enabler. Use `extends JpaRepository<E,ID>, JpaSearchRepository<E>` instead.
- **`JpaCrudSearchRepository<ENTITY, ID>`** — removed for the same reason.

### Changed

#### `freddy-cruder-spring-data`
- `SearchRepository` no longer depends on `peluware-domain` types. The overload `findBySearch(String, String, Pagination, Sort)` — which returned `com.peluware.domain.Page<T>` — has been removed. The interface now only declares methods that use Spring Data types (`Pageable`, Spring `Page<T>`). The mutual delegation pattern between the two overloads, which could produce a `StackOverflowError` if neither was overridden, is gone entirely.
- `SearchRepository.findBySearch` renamed to `findAllBySearch`. The previous name matched Spring Data's `findBy*` query derivation pattern, which caused `QueryCreationException` at startup. The new name avoids that collision.
- `SearchRepository.findAllBySearch` and `countBySearch` are now `default` methods (throw `UnsupportedOperationException`) instead of abstract. This prevents Spring Data from attempting query derivation for methods that match its naming conventions; the fragment implementation overrides them before they are ever called.
- `SpringToPeluwareAdapters.toPage()` now returns a `SpringPage` instead of a plain `Page`, enabling the short-circuit in `PeluwareToSpringAdapters`.
- `PeluwareToSpringAdapters.toPage()` unwraps `SpringPage` directly instead of re-wrapping into `PageImpl`.

### Migration

**Repository definition:**
```java
// Before (never worked)
interface ProductRepository extends CrudSearchRepository<Product, Long> {}

// After
interface ProductRepository extends JpaRepository<Product, Long>, JpaSearchRepository<Product> {}
```

**Service constructor:**
```java
// Before
public ProductService(ProductRepository repo) {
    super(repo, repo, Product.class); // had to pass twice
}

// After — intersection type accepts a single argument
public ProductService(ProductRepository repo) {
    super(repo, Product.class);
}
```

---

## [2.0.0] — 2026-06-26

This release consolidates several structural changes that were building up since 1.x: an ISP-aligned provider model, a complete Criteria API layer for JPA, and — most importantly — the decoupling of `freddy-cruder-jpa` and `freddy-cruder-spring-data` from `omni-search`.

### Why 2.0.0

The core motivation is making `omni-search` optional. In 1.x, every JPA provider required a `JpaOmniSearchPredicateBuilder` in its constructor, which meant you couldn't use `freddy-cruder-jpa` without pulling in the entire `omni-search` dependency tree — even if you didn't need full-text search or RSQL filtering.

Starting from 2.0.0, `omni-search` is an optional integration. The new `SearchPredicateBuilder` interface abstracts predicate construction, and `OmniSearchPredicateAdapter` bridges the existing implementation for those who still use it. If you don't, you can provide your own strategy — or just return `cb.conjunction()` to skip filtering entirely.

The same philosophy applies to `freddy-cruder-spring-data`: `SearchRepository` defines the search contract without tying you to any library; `OmniSearchRepository` is now just one possible implementation of it.

### Breaking Changes

#### `freddy-cruder-core`
- **`CrudProvider` and `OwnedCrudProvider` are now thin composite interfaces.** All operations are defined in atomic `@FunctionalInterface` providers. Code depending on the monolithic interface shape must migrate to the atomic or composed variants.

#### `freddy-cruder-jpa`
- **All provider constructors that accepted `JpaOmniSearchPredicateBuilder` now accept `SearchPredicateBuilder`.** Replace with `OmniSearchPredicateAdapter.ofDefault()` to preserve existing behavior, or provide a custom implementation.
- **`JpaCrudProvider`, `FilterableJpaCrudProvider`, `FilterableOwnedJpaCrudProvider`** — constructor signatures updated accordingly.

#### `freddy-cruder-spring-data`
- **`SpringCrudProvider` and `SpringOwnedCrudProvider` deleted.** These interfaces exposed a default `page(Pageable)` method that called `SpringDataAdapters.page(this, ...)` internally. In practice this pattern was frequently misused: calling `this.page(...)` from within a concrete provider subclass bypasses the Spring proxy, so any AOP advice (`@Transactional`, `@Cacheable`, etc.) would silently not apply. Pagination is now handled at the controller level via `PeluwareToSpringAdapters.page(provider, ...)`, where the call goes through the proxy correctly.
- **`SpringEntityCrudProvider` and `SpringOwnedEntityCrudProvider` deleted.** Extend `EntityCrudProvider` / `OwnedEntityCrudProvider` directly.
- **`SpringDataAdapters` deleted.** Replaced by `SpringToPeluwareAdapters` and `PeluwareToSpringAdapters`.

### Added

#### `freddy-cruder-core`
- 14 atomic `@FunctionalInterface` providers: `PageProvider`, `FindProvider`, `CountProvider`, `ExistsProvider`, `CreateProvider`, `UpdateProvider`, `DeleteProvider` — and their `Owned*` counterparts.
- 4 composed interfaces: `ReadProvider`, `WriteProvider`, `OwnedReadProvider`, `OwnedWriteProvider`.
- `OwnedId<OWNER_ID, ID>` record — composite identifier for owned sub-resources with `toString()` = `"ownerId/id"`.

#### `freddy-cruder-jpa`
- `FilterableOwnedJpaCrudProvider` — JPA implementation of `OwnedEntityCrudProvider` via Criteria API, with `buildOwnerPredicate`, `predicateFilter`, `buildSearchPredicate`, `buildIdPredicate`, `getQueryHints()`, and `runQuery` hooks.
- `SearchPredicateBuilder` — `@FunctionalInterface` decoupling JPA providers from any search library.
- `OmniSearchPredicateAdapter` — bridges `JpaOmniSearchPredicateBuilder` to `SearchPredicateBuilder`. Default behavior unchanged via `ofDefault()`.
- `JpaQueryHelpers` — utility class eliminating Criteria API boilerplate (list, single-result, count, exists).
- `JpaCriteriaCallback` — `@FunctionalInterface` for predicate construction: `(Root<E>, CriteriaBuilder) → Predicate`.
- `JpaUtils.requireTransaction(EntityManager, Supplier)` — new overload with JTA compatibility.

#### `freddy-cruder-spring-data`
- `SpringToPeluwareAdapters` — Spring → peluware type conversions: `toPagination`, `toSort`, `toOrders`, `toPage`, `applyAsPage`.
- `PeluwareToSpringAdapters` — peluware → Spring conversions and execution helpers: `toSort`, `toPageable`, `toPage`, `apply`, `applyAsPage`, `page`.
- `SearchRepository` — search abstraction with bidirectional default delegation between `findBySearch(Pagination, Sort)` and `findBySearch(Pageable)`.
- `OmniSearchRepository` — `SearchRepository` implementation backed by `omni-search`.

### Migration Guide

**JPA providers:**
```java
// Before
new MyProvider(entityManager, jpaOmniSearchPredicateBuilder);

// After — same behavior out of the box
new MyProvider(entityManager, OmniSearchPredicateAdapter.ofDefault());

// Or, without omni-search
new MyProvider(entityManager, (root, cb, metamodel, search, query) -> cb.conjunction());
```

**`SpringDataAdapters`:**

| Before | After |
|---|---|
| `SpringDataAdapters.toPeluwarePagination(p)` | `SpringToPeluwareAdapters.toPagination(p)` |
| `SpringDataAdapters.toPeluwareSort(s)` | `SpringToPeluwareAdapters.toSort(s)` |
| `SpringDataAdapters.toPeluwareOrders(s)` | `SpringToPeluwareAdapters.toOrders(s)` |
| `SpringDataAdapters.toSpringSort(s)` | `PeluwareToSpringAdapters.toSort(s)` |
| `SpringDataAdapters.toSpringPageable(p, s)` | `PeluwareToSpringAdapters.toPageable(p, s)` |
| `SpringDataAdapters.withSpringPageable(p, fn)` | `PeluwareToSpringAdapters.apply(p, fn)` |
| `SpringDataAdapters.page(provider, ...)` | `PeluwareToSpringAdapters.page(provider, ...)` |

**Deleted Spring provider classes:**

| Before | After |
|---|---|
| `extends SpringEntityCrudProvider<...>` | `extends EntityCrudProvider<...>` |
| `extends SpringOwnedEntityCrudProvider<...>` | `extends OwnedEntityCrudProvider<...>` |
| `implements SpringCrudProvider<...>` | `PeluwareToSpringAdapters.page(provider, ...)` in your controller |
| `implements SpringOwnedCrudProvider<...>` | `PeluwareToSpringAdapters.page(provider, ownerId, ...)` in your controller |

---

## [1.x]

See [git history](https://github.com/PeluWare/freddy-cruder/commits/main) for changes prior to 2.0.0.
