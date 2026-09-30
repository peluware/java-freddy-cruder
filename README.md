# Freddy Cruder

[![Maven Central](https://img.shields.io/maven-central/v/com.peluware/freddy-cruder.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.peluware/freddy-cruder)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)

Freddy Cruder is a modular, framework-agnostic Java library that standardizes and simplifies CRUD operations in enterprise applications. It provides a clean abstraction layer between your application logic and your persistence technology, following the Template Method pattern to enforce a consistent lifecycle across all operations.

---

## Modules

```
                         freddy-cruder-core
          ┌─────────────────────┼─────────────────────┐
 freddy-cruder-jpa     freddy-cruder-mongodb    freddy-cruder-spring-data
          │                     │                     │
          └──────┐              └──────┐              │
   freddy-cruder-spring-data-jpa   freddy-cruder-spring-data-mongodb
       (jpa + spring-data)            (mongodb + spring-data)

                     freddy-cruder-bulk-import  (+ core)
          ┌──────────────────┼──────────────────────┐
 freddy-cruder-       freddy-cruder-       freddy-cruder-spring-web-bulk-import
 bulk-import-excel    bulk-import-csv        (+ spring-data, exposes over REST)

                        freddy-cruder-export  (+ core)
          ┌──────────────────┼──────────────────────┐
 freddy-cruder-       freddy-cruder-         freddy-cruder-spring-web-export
 export-csv           export-excel            (+ spring-data, exposes over REST)

                     freddy-cruder-memory  (+ core, for tests and prototypes)

                     freddy-cruder-bom  (every module's version, managed)
```

| Module                              | Description                                                                                                                                                                                                      |
|-------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `freddy-cruder-core`                | Core contracts and abstractions. No framework dependencies.                                                                                                                                                      |
| `freddy-cruder-jpa`                 | JPA implementation via Criteria API. `omni-search-jpa` is an optional integration for full-text search and RSQL filtering.                                                                                       |
| `freddy-cruder-mongodb`             | MongoDB implementation, mirroring the JPA module (`MongoCrudProvider`, `FilterableMongoCrudProvider`, `FilterableOwnedMongoCrudProvider`). `omni-search-mongodb` is an optional integration for search and RSQL. |
| `freddy-cruder-spring-data`         | Spring Data integration with REST controllers, `CrudRepository` support, and `SpringCrudOptions`.                                                                                                                |
| `freddy-cruder-spring-data-jpa`     | JPA fragment for `freddy-cruder-spring-data`. Autoconfigures `JpaSearchEngine` and optional omni-search integration. Use this when your project combines Spring Data JPA with the search fragment.               |
| `freddy-cruder-spring-data-mongodb` | MongoDB fragment for `freddy-cruder-spring-data`. Autoconfigures `MongoSearchEngine` and optional omni-search integration. Use this when your project combines Spring Data MongoDB with the search fragment.     |
| `freddy-cruder-bulk-import`         | Source-agnostic bulk import contract: records from a file are converted to inputs and created through a `CreateProvider`, with a preview and a downloadable template.                                            |
| `freddy-cruder-bulk-import-excel`   | Excel source (Apache POI) for bulk import: classic reader (`.xls`/`.xlsx`, formulas) and streaming reader (`.xlsx`, constant memory).                                                                            |
| `freddy-cruder-bulk-import-csv`     | CSV source (Apache Commons CSV) for bulk import.                                                                                                                                                                 |
| `freddy-cruder-spring-web-bulk-import` | Spring MVC controllers that expose a bulk import provider: `POST /import`, `POST /import/preview` and `GET /import/template`.                                                                                 |
| `freddy-cruder-export`              | Source-agnostic export contract: turns a `ListProvider`/`StreamProvider` into a downloadable file, with a selectable subset of fields.                                                                          |
| `freddy-cruder-export-csv`          | CSV writer (Apache Commons CSV) for export, list- and stream-backed.                                                                                                                                             |
| `freddy-cruder-export-excel`        | Excel writer (Apache POI) for export: in-memory `XSSFWorkbook` (list-backed) or streaming `SXSSFWorkbook` that flushes rows to a temporary file (stream-backed).                                                |
| `freddy-cruder-spring-web-export`   | Spring MVC controllers that expose an export provider: `GET /export`.                                                                                                                                           |
| `freddy-cruder-memory`              | In-memory `EntityCrudProvider`s, meant for tests and prototypes.                                                                                                                                                 |
| `freddy-cruder-bom`                 | POM-only artifact with the version-managed list of every module and the third-party dependencies they need. Import it instead of declaring each module's version by hand.                                       |

---

## Installation

Add the module you need to your `pom.xml`. Each module transitively includes its dependencies.

**Core only** (framework-agnostic):

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-core</artifactId>
    <version>4.3.0</version>
</dependency>
```

**JPA support:**

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-jpa</artifactId>
    <version>4.3.0</version>
</dependency>
```

**Spring Data + REST controllers:**

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-spring-data</artifactId>
    <version>4.3.0</version>
</dependency>
```

**Spring Data JPA with search fragment (includes the two above):**

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-spring-data-jpa</artifactId>
    <version>4.3.0</version>
</dependency>
```

**MongoDB support:**

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-mongodb</artifactId>
    <version>4.3.0</version>
</dependency>
```

**Spring Data MongoDB with search fragment:**

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-spring-data-mongodb</artifactId>
    <version>4.3.0</version>
</dependency>
```

**Bulk import** (pick the source you read; add the REST module to expose it):

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-bulk-import-excel</artifactId> <!-- or freddy-cruder-bulk-import-csv -->
    <version>4.3.0</version>
</dependency>
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-spring-web-bulk-import</artifactId>
    <version>4.3.0</version>
</dependency>
```

The streaming Excel reader needs `com.github.pjfanning:excel-streaming-reader`, which is an optional
dependency of `freddy-cruder-bulk-import-excel` — add it yourself to use `StreamingExcelBulkImportProvider`.

**Export** (pick the format you write; add the REST module to expose it):

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-export-excel</artifactId> <!-- or freddy-cruder-export-csv -->
    <version>4.3.0</version>
</dependency>
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-spring-web-export</artifactId>
    <version>4.3.0</version>
</dependency>
```

**BOM** (manage every module's version from one place, instead of repeating it per dependency):

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.peluware</groupId>
            <artifactId>freddy-cruder-bom</artifactId>
            <version>4.3.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## Core Concepts

### Provider interfaces

`CrudProvider<ID, INPUT, OUTPUT>` and `OwnedCrudProvider<OWNER_ID, ID, INPUT, OUTPUT>` are composed from atomic `@FunctionalInterface` providers, one per operation:

| Atomic provider  | Operation                                                     |
|------------------|---------------------------------------------------------------|
| `PageProvider`   | Paginated list with optional full-text search and RSQL filter |
| `FindProvider`   | Find by ID, throws `NotFoundException` if not found           |
| `CountProvider`  | Count matching entities                                       |
| `ExistsProvider` | Check existence by ID                                         |
| `CreateProvider` | Create a new entity from an input DTO                         |
| `UpdateProvider` | Update an existing entity                                     |
| `DeleteProvider` | Delete by ID                                                  |

Each has an `Owned*` counterpart that adds an `OWNER_ID` scope parameter. Controllers and services can declare only the capabilities they actually need.

### `EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT>`

The abstract base class that implements `CrudProvider` and wires the full CRUD lifecycle. You subclass this and implement the mapping and persistence methods.

**Mapping contracts (required):**

```java
protected abstract void mapInput(INPUT input, ENTITY entity, boolean isNew);

protected abstract OUTPUT mapOutput(ENTITY entity);
```

**Persistence contracts (required):**

```java
protected abstract ENTITY internalFind(ID id) throws NotFoundEntityException;

protected abstract Page<ENTITY> internalPage(String search, String query, Pagination pagination, Sort sort);

protected abstract List<ENTITY> internalList(String search, String query, Sort sort);

protected abstract long internalCount(String search, String query);

protected abstract boolean internalExists(ID id);

protected abstract ENTITY internalCreate(ENTITY entity);

protected abstract ENTITY internalUpdate(ENTITY entity);

protected abstract void internalDelete(ENTITY entity);
```

`EntityCrudProvider` also implements `ListProvider<OUTPUT>` (`list(search, query, sort)`, no pagination)
and `StreamProvider<OUTPUT>` (`stream(search, query, sort)`, a lazily-read `Stream` instead of a `List`),
both following the same path as `page` — same projection, mapping, and lifecycle events. Neither is
**deliberately** composed into `ReadProvider` or exposed by any controller: pagination support doesn't
imply listing an entire resource is sensible, and an unpaginated read belongs to process-internal
consumers (exports, reports, batch jobs), not HTTP endpoints. The default `stream` collects `list`'s
result and streams that — correct but not lazy; a JPA or MongoDB provider overrides it to read through
a live cursor instead, so nothing is loaded until the stream is consumed. `OwnedEntityCrudProvider`
implements the owner-scoped counterparts, `OwnedListProvider<OWNER_ID, OUTPUT>` and
`OwnedStreamProvider<OWNER_ID, OUTPUT>`, the same way.

**Extension hooks (optional overrides):**

```java
protected void preProcess(CrudOperation operation) {
}

protected void postProcess(CrudOperation operation) {
}

protected String applyQueryPolicies(String query) {
    return query;
}

protected <T> T withTransaction(Supplier<T> function) {
    return function.get();
}

protected ENTITY newEntity() { /* reflection-based by default */ }

protected void afterCreate(INPUT input, ENTITY created) {
}

protected void afterUpdate(INPUT input, ENTITY updated) {
}
```

`afterCreate`/`afterUpdate` run right after persistence, inside the same transaction, with the input DTO and the persisted entity (so its generated identifier is available). Use them to persist dependent entities that need the parent's identifier but have no direct relationship at the store level.

### `CrudOptions`

A key/value bag passed to every operation to carry optional metadata that can modify behavior at runtime (soft-delete flags, audit hints, include relations, etc.). Accessed via `CrudContext`:

```java
CrudContext.current().options().getBoolean("includeDeleted");
CrudContext.current().options().require("tenantId", UUID.class);
```

In Spring, options are built automatically from HTTP query parameters and bound to the thread via `CrudContext` (backed by `ScopedValue`).

### `EntityCrudEvents<ENTITY, ID, INPUT>`

Lifecycle hooks that fire at each stage of a CRUD operation. All methods have a no-op default, so you only override what you need:

```java
// Read
void onFind(ENTITY entity)

void onPage(Page<ENTITY> page)

void onCount(long count)

void onExists(boolean exists, ID id)

void eachEntity(ENTITY entity)

// Write — before
void onBeforeCreate(INPUT input, ENTITY entity)

void onBeforeUpdate(INPUT input, ENTITY entity)

void onBeforeDelete(ENTITY entity)

// Write — after
void onAfterCreate(INPUT input, ENTITY entity)

void onAfterUpdate(INPUT input, ENTITY entity)

void onAfterDelete(ENTITY entity)
```

Several handlers compose into one with `EntityCrudEvents.of(first, second)` (or `first.andAll(others)`): they run in order and stop at the first that throws.

---

## Operation Lifecycle

Every write operation follows the same structured flow. Here is the `create` example:

```
create(input)
  └─ preProcess(CREATE)
  └─ withTransaction(() -> {
        newEntity()
        mapInput(input, entity, true)
        events.onBeforeCreate(input, entity)
        internalCreate(entity)
        events.onAfterCreate(input, created)
        events.eachEntity(created)
        return mapOutput(created)
     })
  └─ postProcess(CREATE)
```

---

## Usage with Spring Data

### 1. Define your repository

Extend `JpaRepository` and `JpaSearchRepository`. The search fragment is registered automatically via `spring.factories` — no extra configuration needed.

```java
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSearchRepository<Product> {
}
```

### 2. Define your service

Extend `SpringRepositoryCrudProvider` and implement the two mapping methods. Pass your repository as a single argument — the intersection type constructor accepts any object that implements both `CrudRepository` and `SearchRepository`.

```java

@Service
public class ProductService extends SpringRepositoryCrudProvider<Product, Long, ProductInput, ProductOutput> {

    public ProductService(ProductRepository repository) {
        super(repository, Product.class);
    }

    @Override
    protected void mapInput(ProductInput input, Product entity, boolean isNew) {
        entity.setName(input.name());
        entity.setPrice(input.price());
    }

    @Override
    protected ProductOutput mapOutput(Product entity) {
        return new ProductOutput(entity.getId(), entity.getName(), entity.getPrice());
    }
}
```

### 3. Expose REST endpoints

Implement any combination of controller interfaces. Each one brings a default `@RequestMapping` method wired to your service.

```java

@RestController
@RequestMapping("/products")
public class ProductController implements CrudController<Long, ProductInput, ProductOutput> {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @Override
    public CrudProvider<Long, ProductInput, ProductOutput> getService() {
        return service;
    }
}
```

This exposes:

| Method   | Path              | Description          |
|----------|-------------------|----------------------|
| `GET`    | `/products`       | Paginated list       |
| `GET`    | `/products/{id}`  | Find by ID           |
| `GET`    | `/products/count` | Count                |
| `HEAD`   | `/products/{id}`  | Exists               |
| `POST`   | `/products`       | Create               |
| `PUT`    | `/products/{id}`  | Update               |
| `DELETE` | `/products/{id}`  | Delete (returns 204) |

### 4. Use granular controller interfaces

Instead of `CrudController`, compose only the operations you need:

```java
// Read-only resource
public class ProductController implements PageController<ProductOutput>,
    FindController<Long, ProductOutput> { ...
}

// Write-only resource
public class ProductController implements CreateController<ProductInput, ProductOutput>,
    UpdateController<Long, ProductInput, ProductOutput> { ...
}
```

---

## Multi-Tenant / Owned Resources

Use `OwnedCrudProvider` for resources scoped to an owner. All endpoints receive an extra `@PathVariable OWNER_ID ownerId`:

```java

@RestController
@RequestMapping("/users/{ownerId}/orders")
public class OrderController implements OwnedCrudController<Long, Long, OrderInput, OrderOutput> {
    // ...
}
```

---

## Bulk Import

Load records from a file through a `CreateProvider`, so validation, mapping and lifecycle events run exactly as they do for a single `create`. A `BulkImportProvider` offers three operations:

| Operation           | What it does                                                                                                     |
|---------------------|------------------------------------------------------------------------------------------------------------------|
| `execute(in)`       | Creates every record and returns a `BulkImportResult` (`created`, `skipped`). Stops at the first failing record. |
| `preview(in)`       | Reports what `execute` would do, record by record, without creating anything — and every problem of every row.   |
| `template()`        | An `ImportTemplate`: the file a user fills in.                                                                   |

Each record of a preview is `Created`, `Skipped` (with a reason — not an error, the import carries on) or `Rejected` (with its problems). `execute` throws `BulkImportRecordException`, carrying the position and the problems of the record that failed; run it inside a transaction so a failure leaves nothing behind.

### Define an import

Extend the provider of the source you read and say how a row becomes an input:

```java

@Service
public class ProductImport extends StreamingExcelBulkImportProvider<ProductInput, ProductOutput> {

    public ProductImport(ProductService products) {
        super(products);
    }

    @Override
    protected String sheetName() {
        return "Products";
    }

    @Override
    protected ImportConversion<ProductInput> convert(ExcelRow row) {
        var input = new ProductInput();
        var binder = new ExcelRowBinder(row);
        binder.required(input::setName, 0, "Name", ExcelCell::textOrNull, "The name is missing.");
        binder.required(input::setCategory, 1, "Category", cell -> cell.enumeration(Category.class), "The category is missing.");
        return binder.toConversion(input);   // every problem of the row at once
    }

    @Override
    public ImportTemplate template() {
        return new ImportTemplate("products.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out -> { /* write the workbook to out */ });
    }
}
```

`convert` may also return `ImportConversion.skipped("Already exists")` to leave a row out. Override `created(output)` to react to each created record and `createFailed(position, failure)` to turn a persistence failure into something a user can fix.

| Source module                     | Providers                                                                                                                        |
|-----------------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| `freddy-cruder-bulk-import-excel` | `ExcelBulkImportProvider` (whole workbook in memory; `.xls`/`.xlsx`; evaluates formulas) and `StreamingExcelBulkImportProvider` (`.xlsx` row by row; formulas read as their saved value). |
| `freddy-cruder-bulk-import-csv`   | `CsvBulkImportProvider`, with `charset()`, `format()`, `header()` and `firstColumn()`.                                           |

Prefer the streaming reader for anything that can be large: the classic reader needs roughly 150 times the file size in heap, while the streaming one stays flat. Every provider has an `Owned*` counterpart that adds an `OWNER_ID`.

### Expose it over REST

Implement `BulkImportController` next to your CRUD controller:

```java

@RestController
@RequestMapping("/products")
public class ProductController implements CrudController<Long, ProductInput, ProductOutput>,
    BulkImportController<List<String>, ExcelMetadata> {

    private final ProductService service;
    private final ProductImport bulkImport;

    // constructor omitted

    @Override
    public CrudProvider<Long, ProductInput, ProductOutput> getService() {
        return service;
    }

    @Override
    public BulkImportProvider<?, List<String>, ExcelMetadata, ?> getBulkImportService() {
        return bulkImport;
    }
}
```

| Method | Path                       | Description                                      |
|--------|----------------------------|--------------------------------------------------|
| `POST` | `/products/import`         | Import the uploaded `file`                       |
| `POST` | `/products/import/preview` | What the file would do, without doing it         |
| `GET`  | `/products/import/template`| The template, streamed to the response           |

The library does not map errors: `BulkImportRecordException` and `ExcelSheetMissingException` reach your exception handler, which decides how to answer.

---

## Export

Turn a `ListProvider`/`StreamProvider` into a downloadable file. An `ExportProvider` resolves a search,
query, sort and field selection into an `Export` (`filename()`, `mediaType()`, `writeTo(OutputStream)`)
upfront, without reading anything — the actual read happens whenever `writeTo` ends up being called.

A field is a named, selectable piece of a record (`ExportField<OUTPUT>`: key, label, how to read it off
a record), independent of the destination format. A request may ask for a subset by key; asking for one
that doesn't exist throws `UnknownExportFieldsException`.

### Define an export

Extend the provider of the format you write, passing the `ListProvider`/`StreamProvider` it reads from
and declaring the fields it offers:

```java

@Service
public class ProductExport extends ExcelExportProvider<ProductOutput> {

    public ProductExport(ProductService products) {
        super(products);
    }

    @Override
    protected String filename() {
        return "products.xlsx";
    }

    @Override
    protected List<ExportField<ProductOutput>> fields() {
        return List.of(
            new ExportField<>("name", "Name", ProductOutput::name),
            new ExportField<>("price", "Price", ProductOutput::price)
        );
    }
}
```

The default `writeWorkbook`/`writeRecords` writes a single sheet (or CSV) with a header row of field
labels and one row per record — override it for a different sheet name, columns, styles or starting
position; freddy-cruder only guarantees the result is written to the output afterward.

| Provider base                        | Source                             | Notes                                                                 |
|---------------------------------------|-------------------------------------|------------------------------------------------------------------------|
| `CsvExportProvider`                   | `ListProvider` (`freddy-cruder-export-csv`)   | Apache Commons CSV, with `charset()` and `format()`.                  |
| `StreamingCsvExportProvider`          | `StreamProvider` (`freddy-cruder-export-csv`) | Same, without holding every record in memory at once.                 |
| `ExcelExportProvider`                 | `ListProvider` (`freddy-cruder-export-excel`) | `XSSFWorkbook`, built in memory.                                       |
| `StreamingExcelExportProvider`        | `StreamProvider` (`freddy-cruder-export-excel`) | `SXSSFWorkbook`, flushing rows to a temporary file as they're written — prefer this for a large export. |

Every provider has an `Owned*` counterpart that adds an `OWNER_ID`.

### Expose it over REST

Implement `ExportController` next to your CRUD controller:

```java

@RestController
@RequestMapping("/products")
public class ProductController implements CrudController<Long, ProductInput, ProductOutput>,
    ExportController {

    private final ProductService service;
    private final ProductExport export;

    // constructor omitted

    @Override
    public CrudProvider<Long, ProductInput, ProductOutput> getService() {
        return service;
    }

    @Override
    public ExportProvider getExportService() {
        return export;
    }
}
```

| Method | Path              | Description                                                  |
|--------|-------------------|----------------------------------------------------------------|
| `GET`  | `/products/export`| Downloads the file, honoring `search`, `query`, `sort` and `fields` |

---

## Class Hierarchy

```
CrudProvider<ID, INPUT, OUTPUT>                                             (core — interface)
├── EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT>                          (core — abstract)
│   ├── JpaCrudProvider<ENTITY, ID, INPUT, OUTPUT>                         (jpa — abstract)
│   ├── FilterableJpaCrudProvider<ENTITY, ID, INPUT, OUTPUT>               (jpa — abstract)
│   ├── MemoryCrudProvider<ENTITY, ID, INPUT, OUTPUT>                      (memory — abstract)
│   ├── MongoCrudProvider<ENTITY, ID, INPUT, OUTPUT>                       (mongodb — abstract)
│   ├── FilterableMongoCrudProvider<ENTITY, ID, INPUT, OUTPUT>             (mongodb — abstract)
│   └── SpringRepositoryCrudProvider<ENTITY, ID, INPUT, OUTPUT>            (spring-data — abstract)
└── JpaProjectedCrudProvider<ENTITY, ID, PROJECTION, INPUT, OUTPUT>        (jpa — abstract)

OwnedCrudProvider<OWNER_ID, ID, INPUT, OUTPUT>                                        (core — interface)
├── OwnedEntityCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT>                      (core — abstract)
│   ├── OwnedMemoryCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT>                  (memory — abstract)
│   ├── FilterableOwnedJpaCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT>           (jpa — abstract)
│   └── FilterableOwnedMongoCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT>         (mongodb — abstract)
└── JpaOwnedProjectedCrudProvider<ENTITY, OWNER_ID, ID, PROJECTION, INPUT, OUTPUT>    (jpa — abstract)
```

`JpaCrudProvider` and `FilterableJpaCrudProvider` are siblings, not parent/child — the latter simply adds a `predicateFilter` hook and query hints on top of the same `EntityCrudProvider` base (same for their Mongo counterparts). `JpaProjectedCrudProvider`/`JpaOwnedProjectedCrudProvider` implement `CrudProvider`/
`OwnedCrudProvider` directly instead of extending the entity base, since their read path returns a projection rather than the entity.

---

## Requirements

- Java 25+
- Jakarta Validation API 3.1+
- Spring Boot 4.0+ *(spring-data module only)*

---

## License

Licensed under the [Apache License 2.0](LICENSE).
