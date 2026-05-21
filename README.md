# Freddy Cruder

[![Maven Central](https://img.shields.io/maven-central/v/com.peluware/freddy-cruder.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.peluware/freddy-cruder)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)

Freddy Cruder is a modular, framework-agnostic Java library that standardizes and simplifies CRUD operations in enterprise applications. It provides a clean abstraction layer between your application logic and your persistence technology, following the Template Method pattern to enforce a consistent lifecycle across all operations.

---

## Modules

The library is split into three independent modules. Each one adds a layer on top of the previous:

```
freddy-cruder-core  ←  freddy-cruder-jpa  ←  freddy-cruder-spring-data
```

| Module | Description |
|--------|-------------|
| `freddy-cruder-core` | Core contracts and abstractions. No framework dependencies. |
| `freddy-cruder-jpa` | JPA implementation with full-text search and RSQL filtering via `omni-search-jpa`. |
| `freddy-cruder-spring-data` | Spring Data integration with REST controllers, `CrudRepository` support, and `SpringCrudOptions`. |

---

## Installation

Add the module you need to your `pom.xml`. Each module transitively includes its dependencies.

**Core only** (framework-agnostic):
```xml
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-core</artifactId>
    <version>1.2.10</version>
</dependency>
```

**JPA support:**
```xml
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-jpa</artifactId>
    <version>1.2.10</version>
</dependency>
```

**Spring Data + REST controllers:**
```xml
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-spring-data</artifactId>
    <version>1.2.10</version>
</dependency>
```

---

## Core Concepts

### `CrudProvider<ID, INPUT, OUTPUT>`

The central interface. Defines the seven standard operations:

| Method | Description |
|--------|-------------|
| `page(search, query, pagination, sort)` | Paginated list with optional full-text search and RSQL filter |
| `find(id)` | Find by ID, throws `NotFoundException` if not found |
| `count(search, query)` | Count matching entities |
| `exists(id)` | Check existence by ID |
| `create(input)` | Create a new entity from an input DTO |
| `update(id, input)` | Update an existing entity |
| `delete(id)` | Delete by ID |

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

### 1. Define your service

Extend `SpringRespositoryCrudProvider` and implement the two mapping methods. Everything else is handled for you.

```java
@Service
public class ProductService extends SpringRespositoryCrudProvider<Product, Long, ProductInput, ProductOutput> {

    public ProductService(CrudSearchRepository<Product, Long> repository) {
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

### 2. Expose REST endpoints

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
    public SpringCrudProvider<Long, ProductInput, ProductOutput> getService() {
        return service;
    }
}
```

This exposes:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/products` | Paginated list |
| `GET` | `/products/{id}` | Find by ID |
| `GET` | `/products/count` | Count |
| `HEAD` | `/products/{id}` | Exists |
| `POST` | `/products` | Create |
| `PUT` | `/products/{id}` | Update |
| `DELETE` | `/products/{id}` | Delete (returns 204) |

### 3. Use granular controller interfaces

Instead of `CrudController`, compose only the operations you need:

```java
// Read-only resource
public class ProductController implements PageController<Long, ProductOutput>,
                                          FindController<Long, ProductOutput> { ... }

// Write-only resource
public class ProductController implements CreateController<ProductInput, ProductOutput>,
                                          UpdateController<Long, ProductInput, ProductOutput> { ... }
```

---

## Multi-Tenant / Owned Resources

Use `OwnedCrudProvider` and its Spring counterpart `SpringOwnedCrudProvider` for resources scoped to an owner. All endpoints receive an extra `@PathVariable OWNER_ID ownerId`:

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
CrudProvider<ID, INPUT, OUTPUT>
└── EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT>          (core — abstract)
    ├── JpaCrudProvider<ENTITY, ID, INPUT, OUTPUT>          (jpa — abstract)
    └── SpringEntityCrudProvider<ENTITY, ID, INPUT, OUTPUT> (spring-data — abstract)
        └── SpringRespositoryCrudProvider<...>              (spring-data — abstract, uses CrudRepository)

OwnedCrudProvider<OWNER_ID, ID, INPUT, OUTPUT>
└── OwnedEntityCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT>  (core — abstract)
    └── SpringOwnedEntityCrudProvider<...>                         (spring-data — abstract)
```

---

## Requirements

- Java 25+
- Jakarta Validation API 3.1+
- Spring Boot 4.0+ *(spring-data module only)*

---

## License

Licensed under the [Apache License 2.0](LICENSE).
