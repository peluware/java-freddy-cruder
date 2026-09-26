# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [4.1.0] — 2026-09-25

Adds bulk import: loading records from a file — Excel or CSV — through a `CreateProvider`, with a
preview of what the file would do before doing it, a downloadable template, and REST controllers
that expose all three. Nothing in 4.0.0 changes: the release is additive, and the only edit to an
existing module is new `EntityCrudEvents` composition helpers.

### Added

#### `freddy-cruder-bulk-import` (new)

- `BulkImportProvider<INPUT, PREVIEW, PREVIEW_META, OUTPUT>` and its owner-scoped counterpart
  `OwnedBulkImportProvider` — the contract of an import: `execute(InputStream)`,
  `preview(InputStream)` and `template()`. Source-agnostic: it knows nothing about Excel, CSV or HTTP.
  Records are created through a `CreateProvider`, so validation, mapping and lifecycle events run
  exactly as they do for a single `create`.
- `ImportConversion<INPUT>` — what a source says about one record: `converted(input)`,
  `unconvertible(problems)` or `skipped(reason)`. `ImportConversion.attempt(Supplier)` turns a
  conversion that throws into `unconvertible`. A skipped record is not an error: it is reported and
  the import carries on.
- `ImportBinder` — collects the problems of a record instead of stopping at the first, so the user
  gets every mistake of a row at once.
- `BulkImportPreview` with `ImportRecordPreview` (`Created`, `Skipped`, `Rejected`), `BulkImportResult`
  (`created`, `skipped`), `ImportProblem` (message and optional field), `ImportTemplate` and
  `BulkImportRecordException` (carries the position and the problems of the record that failed).
- Hooks `created(output)` and `createFailed(position, failure)`, to react to each created record and
  to translate persistence failures into something a user can act on.

#### `freddy-cruder-bulk-import-excel` (new)

- `ExcelBulkImportProvider` — loads the whole workbook; reads `.xls` and `.xlsx` and evaluates formulas.
- `StreamingExcelBulkImportProvider` — reads `.xlsx` row by row with `excel-streaming-reader`, an
  **optional** dependency you add yourself. Formulas are read as their saved value. Memory stays flat
  regardless of the file: in our measurements the classic reader needs roughly 150 times the file size
  in heap, while the streaming one ran in a fixed 32 MB.
- Owned variants: `OwnedExcelBulkImportProvider` and `OwnedStreamingExcelBulkImportProvider`.
- `ExcelRow`, `ExcelCell` (`text`, `textOrNull`, `textAs`, `flag`, `integer`, `date`, `enumeration`, …),
  `ExcelRowBinder`, `ExcelLayout` (header row, first data row, first column) and `ExcelMetadata`
  (the headers, for the preview).
- `ExcelSheetMissingException` when the file has no sheet with the expected name.

#### `freddy-cruder-bulk-import-csv` (new)

- `CsvBulkImportProvider` and `OwnedCsvBulkImportProvider`, on Apache Commons CSV, with `charset()`,
  `format()`, `header()` and `firstColumn()` to adapt them to the file.
- `CsvRow`, `CsvCell`, `CsvRowBinder` and `CsvMetadata`, mirroring the Excel ones.

#### `freddy-cruder-spring-web-bulk-import` (new)

- `BulkImportController<PREVIEW, PREVIEW_META>` and `OwnedBulkImportController` expose a provider as
  `POST /import`, `POST /import/preview` and `GET /import/template`, each wrapped in `CrudContext`
  with the request's `SpringCrudOptions`. The template is written straight to the response through a
  `StreamingResponseBody`, without buffering it in memory.
- The controllers declare `getBulkImportService()` rather than `getService()`, so one controller can
  also implement `CrudController`.
- Errors are not mapped: `BulkImportRecordException` and, for Excel, `ExcelSheetMissingException`
  reach your exception handler, which decides how to answer.
- The module is Spring MVC only — hence `web` in its name — and lives apart from
  `freddy-cruder-spring-data`, where REST is optional, because everything in it is a controller.

#### `freddy-cruder-memory` (new)

- `MemoryCrudProvider` and `OwnedMemoryCrudProvider` — in-memory `EntityCrudProvider`s for tests and
  prototypes. Create, update and delete run in a simulated transaction that is rolled back on failure.
  `MemoryStore` and `MemoryIds` are the storage and the identifier strategy.

#### `freddy-cruder-core`

- `EntityCrudEvents.of(...)` (varargs or `Iterable`) and `andAll(Iterable)` compose several event
  handlers into one, dispatching in order and stopping at the first that throws. `DEFAULT` handlers
  are dropped and nested compositions flattened.

### Changed

- Build: JUnit 6.1.3 and Maven Surefire 3.6.0 are managed in the parent `pom.xml`.

### Migration

Nothing to migrate. To use bulk import, add the source module you need
(`freddy-cruder-bulk-import-excel` or `freddy-cruder-bulk-import-csv`) and, to expose it over REST,
`freddy-cruder-spring-web-bulk-import`. To read `.xlsx` files in streaming, also add
`com.github.pjfanning:excel-streaming-reader`.

---

## [4.0.0] — 2026-09-10

Adds an unpaginated listing operation alongside `page`, for process-internal consumers — exports,
reports, batch jobs — that today build a `Page` just to discard it via `Pagination.unpaginated()`.

### Why 4.0.0

`EntityCrudProvider` and `OwnedEntityCrudProvider` gain a new required `internalList` persistence
contract, the same way `internalPage` already is. Any class extending either of them
directly — outside the JPA, MongoDB, and Spring Data modules, which already implement it — needs to
add that method to keep compiling.

### Added

#### `freddy-cruder-core`

- `ListProvider<OUTPUT>` and `OwnedListProvider<OWNER_ID, OUTPUT>` — unpaginated counterparts of
  `PageProvider`/`OwnedPageProvider`. Deliberately **not** composed into `ReadProvider`/
  `OwnedReadProvider`, and not exposed by any controller: pagination support doesn't imply listing an
  entire resource is sensible.
- `EntityCrudProvider`/`OwnedEntityCrudProvider` implement `ListProvider`/`OwnedListProvider`,
  following the same path as `page` — same projection, mapping, and lifecycle events.
- `CrudOperation.LIST`.

#### `freddy-cruder-jpa`

- `internalList` in `JpaCrudProvider`, `FilterableJpaCrudProvider`, and
  `FilterableOwnedJpaCrudProvider`, reusing the existing unpaginated `EntityListQuery` constructor.
- `JpaProjectedCrudProvider`/`JpaOwnedProjectedCrudProvider` implement `ListProvider`/
  `OwnedListProvider` directly — no new abstract method needed, since `list` reuses the same
  `selection()`/`groupBy()`/`searchPredicate()` hooks `page` already does.

#### `freddy-cruder-mongodb`

- `internalList` in `MongoCrudProvider`, `FilterableMongoCrudProvider`, and
  `FilterableOwnedMongoCrudProvider`.
- `MongoQueryHelpers.find(MongoCollection, Bson, Sort)` — unpaginated overload.

#### `freddy-cruder-spring-data`

- `SearchEngine`/`SearchRepository` gain a native, unpaginated `findAllBySearch(..., Sort)` returning
  `List<T>` directly — no `Pageable`/`Page` involved. A custom `SearchEngine` implementation must add
  it too. `internalList` in `SpringRepositoryCrudProvider` calls it directly.

#### `freddy-cruder-spring-data-jpa`

- `JpaSearchEngine.findAllBySearch(..., Sort)` implements the native overload by reusing
  `EntityListQuery` with `Pageable.unpaged(sort)` — an internal detail; the fragment method itself
  takes and returns no `Pageable`/`Page`.

#### `freddy-cruder-spring-data-mongodb`

- `MongoSearchEngine.findAllBySearch(..., Sort)` implements the native overload via `Query.with(Sort)`
  — no `Pageable` at all, not even internally.

### Fixed

#### `freddy-cruder-spring-data`

- `PeluwareToSpringAdapters.toPageable` silently dropped the requested `Sort` whenever `Pagination`
  was unpaginated, since it returned `Pageable.unpaged()` instead of `Pageable.unpaged(sort)`. Any
  call to `page(search, query, Pagination.unpaginated(), sort)` on a `SpringRepositoryCrudProvider`
  — the exact pattern this release's `list()` replaces — returned results in an unspecified order,
  ignoring `sort` entirely.

### Migration

If you extend `EntityCrudProvider` or `OwnedEntityCrudProvider` directly (not through
`FilterableJpaCrudProvider`, `MongoCrudProvider`, or `SpringRepositoryCrudProvider`), implement the
new `internalList` method:

```java
// EntityCrudProvider
protected List<ENTITY> internalList(@Nullable String search, @Nullable String query, Sort sort) {
    // return every entity matching search/query, sorted
}
```

---

## [3.2.1] — 2026-09-09

### Changed

#### `freddy-cruder-jpa`

- `searchPredicate` is no longer `final` in `JpaCrudProvider`, `FilterableJpaCrudProvider`,
  `FilterableOwnedJpaCrudProvider`, `JpaProjectedCrudProvider`, and `JpaOwnedProjectedCrudProvider`.
  It was the only customization point left non-overridable while `predicateFilter`,
  `buildIdPredicate`, `buildOwnerPredicate`, and `getQueryHints()` already were; this removes that
  inconsistency.

#### `freddy-cruder-spring-data-jpa`

- `JpaSearchEngine`'s `searchPredicate` and `count` are now `protected` instead of `private`, so a
  subclass can reuse them.

This is purely a visibility relaxation — nothing was overridable before that now behaves differently
by default; existing code is unaffected either way.

---

## [3.2.0] — 2026-08-29

### Fixed

#### `freddy-cruder-jpa`

- `JpaUtils.findPath` no longer forces a join just to read the id of a single-valued association
  (e.g. `"customer.id"`). When the path's last segment is the target's own id, it now navigates the
  path directly (`from.get("customer").get("id")`), which most JPA providers resolve to the local
  foreign-key column without touching the associated table. This also fixes `isNull`/`isNotNull`
  checks on such a path, which previously could never match: an `INNER` join was silently dropping
  rows with a null association before the predicate was ever evaluated.

  This shortcut only skips the join for the **owning** side of the association (`@ManyToOne`, or an
  owning `@OneToOne` with `@JoinColumn`) — where the id is already the local foreign-key column, so
  the requested `joinType` (`INNER`/`LEFT`) is irrelevant either way. For the **inverse** side of a
  `@OneToOne` (`mappedBy`), a join is still required and still happens; the only known gap is that the
  implicit path navigation used to reach it defaults to `INNER` per JPQL, regardless of the `joinType`
  originally requested — so ordering/filtering by the id of a nullable inverse `@OneToOne` could drop
  rows that a requested `LEFT` join would have kept. Inverse-side single-valued associations are rare
  in a well-modeled domain; this edge case is left as a known limitation for now, not a silent-data-loss
  risk — the join always happens, only its type can differ from what was requested.
- `SearchPredicateBuilder.build` (and `OmniSearchPredicateAdapter`'s implementation) is now annotated
  `@Nullable` on its return type, matching the "`null` means no restriction" contract the rest of the
  query model (`JpaPredicate`) already followed. No behavior change — this corrects a stale javadoc
  that said the return "must not be null".

---

## [3.1.0] — 2026-08-22

Bumps the optional `omni-search` integration to 2.5.0. No Java API of `freddy-cruder` changes — this is a transitive dependency bump — but it changes RSQL search **behavior**, so read the note below before upgrading if your project searches on nullable fields.

### ⚠️ Behavior change — `field==null` in RSQL queries

`omni-search` 2.5.0 introduces dedicated, value-less null operators and stops treating the literal text `"null"` as a magic value:

| Query            | 2.4.0 and earlier | 2.5.0+                        |
|------------------|-------------------|-------------------------------|
| `field==null`    | `IS NULL` check   | literal string `"null"` match |
| `field=null=`    | *(not supported)* | `IS NULL` check               |
| `field=notnull=` | *(not supported)* | `IS NOT NULL` check           |

If any RSQL query your application sends — from a saved filter, a frontend, or a script — uses
`field==null` to mean "is null", **it will silently stop matching those rows** after upgrading, since it now matches the literal word "null" instead. Search `field==null` in your codebase and replace it with `field=null=`, and treat `field==''` as the empty string from here on — it is no longer coerced to `null`.

This only affects `freddy-cruder-jpa`/`freddy-cruder-mongodb` consumers using `OmniSearchPredicateAdapter`/
`OmniSearchFilterAdapter` (the default `SearchPredicateBuilder`/`SearchFilterBuilder`) with RSQL query strings that filter on nullability. A custom `SearchPredicateBuilder`/`SearchFilterBuilder` is unaffected.

### Changed

- The optional `omni-search-jpa`/`omni-search-mongodb` dependency moved to 2.5.0, which forks the RSQL parser to a maintained fork (`io.github.nstdio:rsql-parser`, same `cz.jirutka.rsql.parser.*`
  packages — no source changes needed). If your project also declares
  `cz.jirutka.rsql:rsql-parser` directly, remove it to avoid two jars providing the same packages.

---

## [3.0.1] — 2026-07-25

### Added

#### `freddy-cruder-jpa`

- `JpaGroupBy` — new axis for the query model, building the `GROUP BY` clause. `JpaGroupBy.self()`
  groups by the query source as a whole, letting a projection select any of its columns freely alongside an aggregate over a joined plural association; `JpaGroupBy.none()` (the default) applies no grouping.
- `ListQuery`/`FindQuery` gained a `groupBy` parameter (optional — existing constructors without it still work), and `JpaProjectedCrudProvider`/`JpaOwnedProjectedCrudProvider` gained a `groupBy()`
  hook applied to `page`/`find`, defaulting to no grouping.

This is purely additive — every new parameter has a matching overload/default, so existing code keeps compiling and behaving the same.

---

## [3.0.0] — 2026-07-21

This release adds MongoDB as a first-class store alongside JPA, and replaces the old ad-hoc Criteria API helpers with a small, composable query model. Several APIs were renamed or reshaped along the way, which is why this is a major version.

### Why 3.0.0

`freddy-cruder-jpa` grew organically around a handful of static helpers (`JpaQueryHelpers`,
`JpaCriteriaExecutor`, `JpaCriteriaCallback`). They worked, but every new query shape meant more overloads. This release replaces them with a small set of composable pieces — a source, a selection, a filter, an ordering, and a result — that combine into reusable, named query objects (`CountQuery`, `ExistsQuery`, `ListQuery`, `FindQuery`, and their `Entity*` shorthands for the common
"whole entity" case). The same shape now also powers MongoDB, so both stores read the same way.

### Added

#### `freddy-cruder-mongodb` *(new module)*

- `MongoCrudProvider` / `FilterableMongoCrudProvider` / `FilterableOwnedMongoCrudProvider` — the MongoDB counterparts of the JPA providers, built on the MongoDB sync driver.
- `SearchFilterBuilder` — pluggable full-text/RSQL filtering strategy, with `omni-search-mongodb` as the optional default via `OmniSearchFilterAdapter`.

#### `freddy-cruder-spring-data-mongodb` *(new module)*

- `MongoSearchRepository` / `DefaultMongoSearchRepository` / `MongoSearchEngine` — the search fragment for Spring Data MongoDB repositories, autoconfigured the same way as the JPA one.

#### `freddy-cruder-jpa`

- A composable query model in the new `com.peluware.freddy.cruder.jpa.query` package: build a query from small independent pieces (source, selection, filter, ordering, result, hints) and run it with
  `JpaQueryExecutor`, or chain `query.exec(entityManager)` directly.
- Reusable query objects — `CountQuery`, `ExistsQuery`, `ListQuery`, `FindQuery` — plus `Entity*`
  variants for the common case of a whole entity rooted at its class.
- `JpaHints` — named constants and factories for standard Jakarta Persistence query hints (fetch/load graph, timeouts, cache mode), instead of hand-typed hint strings.
- `JpaProjectedCrudProvider` / `JpaOwnedProjectedCrudProvider` — read entities through a lean projection (e.g. a `cb.construct(...)` DTO) instead of loading the full entity, while writes still operate on the real entity.

#### `freddy-cruder-core`

- `afterCreate(INPUT, ENTITY)` / `afterUpdate(INPUT, ENTITY)` hooks on `EntityCrudProvider` and
  `OwnedEntityCrudProvider` (owner-scoped variants), run right after persistence with the input DTO and the now-persisted entity available. Useful for persisting dependent entities that need the parent's generated identifier and don't have a direct relationship at the store level.

### Changed

#### `freddy-cruder-spring-data`

- `SearchRepositoryEngine` renamed to `SearchEngine`.

#### `freddy-cruder-spring-data-jpa`

- `JpaSearchRepositoryEngine` renamed to `JpaSearchEngine`.

#### `freddy-cruder-jpa`

- `buildIdPredicate` and `buildOwnerPredicate` now build a reusable `JpaPredicate` instead of a raw Criteria `Predicate`, and take just the id/owner value — the entity source is supplied by the query that uses them, not passed in by the caller.

### Removed

#### `freddy-cruder-jpa`

- `JpaQueryHelpers`, `JpaCriteriaExecutor`, `JpaCriteriaCallback` — replaced by the query model above.
- The `runQuery` hook on `FilterableOwnedJpaCrudProvider` — no longer needed with the new query model.

### Migration

**Renamed engines:**

| Before                      | After             |
|-----------------------------|-------------------|
| `SearchRepositoryEngine`    | `SearchEngine`    |
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
interface ProductRepository extends CrudSearchRepository<Product, Long> {
}

// After
interface ProductRepository extends JpaRepository<Product, Long>, JpaSearchRepository<Product> {
}
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

- **`SpringCrudProvider` and `SpringOwnedCrudProvider` deleted.** These interfaces exposed a default `page(Pageable)` method that called `SpringDataAdapters.page(this, ...)` internally. In practice this pattern was frequently misused: calling `this.page(...)` from within a concrete provider subclass bypasses the Spring proxy, so any AOP advice (`@Transactional`, `@Cacheable`, etc.) would silently not apply. Pagination is now handled at the controller level via
  `PeluwareToSpringAdapters.page(provider, ...)`, where the call goes through the proxy correctly.
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

| Before                                         | After                                          |
|------------------------------------------------|------------------------------------------------|
| `SpringDataAdapters.toPeluwarePagination(p)`   | `SpringToPeluwareAdapters.toPagination(p)`     |
| `SpringDataAdapters.toPeluwareSort(s)`         | `SpringToPeluwareAdapters.toSort(s)`           |
| `SpringDataAdapters.toPeluwareOrders(s)`       | `SpringToPeluwareAdapters.toOrders(s)`         |
| `SpringDataAdapters.toSpringSort(s)`           | `PeluwareToSpringAdapters.toSort(s)`           |
| `SpringDataAdapters.toSpringPageable(p, s)`    | `PeluwareToSpringAdapters.toPageable(p, s)`    |
| `SpringDataAdapters.withSpringPageable(p, fn)` | `PeluwareToSpringAdapters.apply(p, fn)`        |
| `SpringDataAdapters.page(provider, ...)`       | `PeluwareToSpringAdapters.page(provider, ...)` |

**Deleted Spring provider classes:**

| Before                                       | After                                                                      |
|----------------------------------------------|----------------------------------------------------------------------------|
| `extends SpringEntityCrudProvider<...>`      | `extends EntityCrudProvider<...>`                                          |
| `extends SpringOwnedEntityCrudProvider<...>` | `extends OwnedEntityCrudProvider<...>`                                     |
| `implements SpringCrudProvider<...>`         | `PeluwareToSpringAdapters.page(provider, ...)` in your controller          |
| `implements SpringOwnedCrudProvider<...>`    | `PeluwareToSpringAdapters.page(provider, ownerId, ...)` in your controller |

---

## [1.x]

See [git history](https://github.com/PeluWare/freddy-cruder/commits/main) for changes prior to 2.0.0.
