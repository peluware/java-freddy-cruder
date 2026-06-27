# Freddy Cruder

[![Maven Central](https://img.shields.io/maven-central/v/com.peluware/freddy-cruder.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.peluware/freddy-cruder)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)

Freddy Cruder is a modular, framework-agnostic Java library that standardizes and simplifies CRUD operations in enterprise applications. It provides a clean abstraction layer between your application logic and your persistence technology, following the Template Method pattern to enforce a consistent lifecycle across all operations.

---

## Modules

```
                  freddy-cruder-core
                 /                  \
    freddy-cruder-jpa    freddy-cruder-spring-data
              \                    /
         freddy-cruder-spring-data-jpa
```

| Module                          | Description                                                                                                                                                                                                  |
|---------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `freddy-cruder-core`            | Core contracts and abstractions. No framework dependencies.                                                                                                                                                  |
| `freddy-cruder-jpa`             | JPA implementation via Criteria API. `omni-search-jpa` is an optional integration for full-text search and RSQL filtering.                                                                                   |
| `freddy-cruder-spring-data`     | Spring Data integration with REST controllers, `CrudRepository` support, and `SpringCrudOptions`.                                                                                                            |
| `freddy-cruder-spring-data-jpa` | JPA fragment for `freddy-cruder-spring-data`. Autoconfigures `JpaSearchRepositoryEngine` and optional omni-search integration. Use this when your project combines Spring Data JPA with the search fragment. |

---

## Installation

Add the module you need to your `pom.xml`. Each module transitively includes its dependencies.

**Core only** (framework-agnostic):
```xml
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-core</artifactId>
    <version>2.1.0</version>
</dependency>
```

**JPA support:**
```xml
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-jpa</artifactId>
    <version>2.1.0</version>
</dependency>
```

**Spring Data + REST controllers:**
```xml
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-spring-data</artifactId>
    <version>2.1.0</version>
</dependency>
```

**Spring Data JPA with search fragment (includes the two above):**
```xml
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-spring-data-jpa</artifactId>
    <version>2.1.0</version>
</dependency>
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
protected abstract long internalCount(String search, String query);
protected abstract boolean internalExists(ID id);
protected abstract ENTITY internalCreate(ENTITY entity);
protected abstract ENTITY internalUpdate(ENTITY entity);
protected abstract void internalDelete(ENTITY entity);
```

**Extension hooks (optional overrides):**
```java
protected void preProcess(CrudOperation operation) {}
protected void postProcess(CrudOperation operation) {}
protected String applyQueryPolicies(String query) { return query; }
protected <T> T withTransaction(Supplier<T> function) { return function.get(); }
protected ENTITY newEntity() { /* reflection-based by default */ }
```

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
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSearchRepository<Product> {}
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
                                          FindController<Long, ProductOutput> { ... }

// Write-only resource
public class ProductController implements CreateController<ProductInput, ProductOutput>,
                                          UpdateController<Long, ProductInput, ProductOutput> { ... }
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

## Class Hierarchy

```
CrudProvider<ID, INPUT, OUTPUT>                                        (core — interface)
└── EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT>                      (core — abstract)
    ├── JpaCrudProvider<ENTITY, ID, INPUT, OUTPUT>                     (jpa — abstract)
    │   └── FilterableJpaCrudProvider<ENTITY, ID, INPUT, OUTPUT>       (jpa — abstract)
    └── SpringRepositoryCrudProvider<ENTITY, ID, INPUT, OUTPUT>        (spring-data — abstract)

OwnedCrudProvider<OWNER_ID, ID, INPUT, OUTPUT>                         (core — interface)
└── OwnedEntityCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT>       (core — abstract)
    └── FilterableOwnedJpaCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT>  (jpa — abstract)
```

---

## Requirements

- Java 25+
- Jakarta Validation API 3.1+
- Spring Boot 4.0+ *(spring-data module only)*

---

## License

Licensed under the [Apache License 2.0](LICENSE).
