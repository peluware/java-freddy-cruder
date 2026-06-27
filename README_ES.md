# Freddy Cruder

[![Maven Central](https://img.shields.io/maven-central/v/com.peluware/freddy-cruder.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.peluware/freddy-cruder)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)

Freddy Cruder es una librería Java modular y agnóstica al framework que estandariza y simplifica las operaciones CRUD en aplicaciones empresariales. Provee una capa de abstracción limpia entre la lógica de aplicación y la tecnología de persistencia, siguiendo el patrón Template Method para garantizar un ciclo de vida consistente en todas las operaciones.

---

## Módulos

```
                  freddy-cruder-core
                 /                  \
    freddy-cruder-jpa    freddy-cruder-spring-data
              \                    /
         freddy-cruder-spring-data-jpa
```

| Módulo                          | Descripción                                                                                                                                                                                           |
|---------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `freddy-cruder-core`            | Contratos y abstracciones base. Sin dependencias de framework.                                                                                                                                        |
| `freddy-cruder-jpa`             | Implementación JPA via Criteria API. `omni-search-jpa` es una integración opcional para búsqueda full-text y filtrado RSQL.                                                                           |
| `freddy-cruder-spring-data`     | Integración con Spring Data: controllers REST, soporte para `CrudRepository` y `SpringCrudOptions`.                                                                                                   |
| `freddy-cruder-spring-data-jpa` | Fragmento JPA para `freddy-cruder-spring-data`. Autoconfigura `JpaSearchRepositoryEngine` e integración opcional con omni-search. Úsalo cuando combines Spring Data JPA con el fragmento de búsqueda. |

---

## Instalación

Agrega el módulo que necesitas en tu `pom.xml`. Cada módulo incluye sus dependencias transitivamente.

**Solo core** (agnóstico al framework):
```xml
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-core</artifactId>
    <version>2.1.0</version>
</dependency>
```

**Soporte JPA:**
```xml
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-jpa</artifactId>
    <version>2.1.0</version>
</dependency>
```

**Spring Data + controllers REST:**
```xml
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-spring-data</artifactId>
    <version>2.1.0</version>
</dependency>
```

**Spring Data JPA con fragmento de búsqueda (incluye los dos anteriores):**
```xml
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-spring-data-jpa</artifactId>
    <version>2.1.0</version>
</dependency>
```

---

## Conceptos base

### Interfaces de provider

`CrudProvider<ID, INPUT, OUTPUT>` y `OwnedCrudProvider<OWNER_ID, ID, INPUT, OUTPUT>` se componen de providers atómicos `@FunctionalInterface`, uno por operación:

| Provider atómico | Operación                                                      |
|------------------|----------------------------------------------------------------|
| `PageProvider`   | Lista paginada con búsqueda full-text y filtro RSQL opcionales |
| `FindProvider`   | Buscar por ID; lanza `NotFoundException` si no existe          |
| `CountProvider`  | Contar entidades que coincidan                                 |
| `ExistsProvider` | Verificar existencia por ID                                    |
| `CreateProvider` | Crear una entidad desde un DTO de entrada                      |
| `UpdateProvider` | Actualizar una entidad existente                               |
| `DeleteProvider` | Eliminar por ID                                                |

Cada uno tiene su contraparte `Owned*` que agrega un parámetro de scope `OWNER_ID`. Los controllers y servicios pueden declarar solo las capacidades que realmente necesitan.

### `EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT>`

La clase abstracta base que implementa `CrudProvider` y conecta el ciclo de vida CRUD completo. La subclasificas e implementas los métodos de mapeo y persistencia.

**Contratos de mapeo (requeridos):**
```java
protected abstract void mapInput(INPUT input, ENTITY entity, boolean isNew);
protected abstract OUTPUT mapOutput(ENTITY entity);
```

**Contratos de persistencia (requeridos):**
```java
protected abstract ENTITY internalFind(ID id) throws NotFoundEntityException;
protected abstract Page<ENTITY> internalPage(String search, String query, Pagination pagination, Sort sort);
protected abstract long internalCount(String search, String query);
protected abstract boolean internalExists(ID id);
protected abstract ENTITY internalCreate(ENTITY entity);
protected abstract ENTITY internalUpdate(ENTITY entity);
protected abstract void internalDelete(ENTITY entity);
```

**Hooks de extensión (overrides opcionales):**
```java
protected void preProcess(CrudOperation operation) {}
protected void postProcess(CrudOperation operation) {}
protected String applyQueryPolicies(String query) { return query; }
protected <T> T withTransaction(Supplier<T> function) { return function.get(); }
protected ENTITY newEntity() { /* basado en reflexión por defecto */ }
```

### `CrudOptions`

Un mapa clave/valor que se pasa a cada operación para llevar metadatos opcionales que pueden modificar el comportamiento en tiempo de ejecución (flags de soft-delete, hints de auditoría, incluir relaciones, etc.). Se accede via `CrudContext`:

```java
CrudContext.current().options().getBoolean("includeDeleted");
CrudContext.current().options().require("tenantId", UUID.class);
```

En Spring, las opciones se construyen automáticamente desde los parámetros HTTP y se enlazan al hilo via `CrudContext` (respaldado por `ScopedValue`).

### `EntityCrudEvents<ENTITY, ID, INPUT>`

Hooks de ciclo de vida que se disparan en cada etapa de una operación CRUD. Todos tienen una implementación no-op por defecto, así que solo sobreescribes lo que necesitas:

```java
// Lectura
void onFind(ENTITY entity)
void onPage(Page<ENTITY> page)
void onCount(long count)
void onExists(boolean exists, ID id)
void eachEntity(ENTITY entity)

// Escritura — antes
void onBeforeCreate(INPUT input, ENTITY entity)
void onBeforeUpdate(INPUT input, ENTITY entity)
void onBeforeDelete(ENTITY entity)

// Escritura — después
void onAfterCreate(INPUT input, ENTITY entity)
void onAfterUpdate(INPUT input, ENTITY entity)
void onAfterDelete(ENTITY entity)
```

---

## Ciclo de vida de una operación

Toda operación de escritura sigue el mismo flujo estructurado. Ejemplo con `create`:

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

## Uso con Spring Data

### 1. Define tu repositorio

Extiende `JpaRepository` y `JpaSearchRepository`. El fragmento de búsqueda se registra automáticamente via `spring.factories` — sin configuración adicional.

```java
public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSearchRepository<Producto> {}
```

### 2. Define tu servicio

Extiende `SpringRepositoryCrudProvider` e implementa los dos métodos de mapeo. Pasa tu repositorio como argumento único — el constructor de tipo intersección acepta cualquier objeto que implemente tanto `CrudRepository` como `SearchRepository`.

```java
@Service
public class ProductoService extends SpringRepositoryCrudProvider<Producto, Long, ProductoInput, ProductoOutput> {

    public ProductoService(ProductoRepository repository) {
        super(repository, Producto.class);
    }

    @Override
    protected void mapInput(ProductoInput input, Producto entity, boolean isNew) {
        entity.setNombre(input.nombre());
        entity.setPrecio(input.precio());
    }

    @Override
    protected ProductoOutput mapOutput(Producto entity) {
        return new ProductoOutput(entity.getId(), entity.getNombre(), entity.getPrecio());
    }
}
```

### 3. Expón los endpoints REST

Implementa cualquier combinación de interfaces de controller. Cada una trae un método `@RequestMapping` default conectado a tu servicio.

```java
@RestController
@RequestMapping("/productos")
public class ProductoController implements CrudController<Long, ProductoInput, ProductoOutput> {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @Override
    public CrudProvider<Long, ProductoInput, ProductoOutput> getService() {
        return service;
    }
}
```

Esto expone:

| Método   | Ruta               | Descripción            |
|----------|--------------------|------------------------|
| `GET`    | `/productos`       | Lista paginada         |
| `GET`    | `/productos/{id}`  | Buscar por ID          |
| `GET`    | `/productos/count` | Contar                 |
| `HEAD`   | `/productos/{id}`  | Verificar existencia   |
| `POST`   | `/productos`       | Crear                  |
| `PUT`    | `/productos/{id}`  | Actualizar             |
| `DELETE` | `/productos/{id}`  | Eliminar (retorna 204) |

### 4. Usa interfaces de controller granulares

En lugar de `CrudController`, compón solo las operaciones que necesitas:

```java
// Recurso de solo lectura
public class ProductoController implements PageController<ProductoOutput>,
                                           FindController<Long, ProductoOutput> { ... }

// Recurso de solo escritura
public class ProductoController implements CreateController<ProductoInput, ProductoOutput>,
                                           UpdateController<Long, ProductoInput, ProductoOutput> { ... }
```

---

## Recursos con dueño (Multi-tenant)

Usa `OwnedCrudProvider` para recursos acotados a un propietario. Todos los endpoints reciben un `@PathVariable OWNER_ID ownerId` adicional:

```java
@RestController
@RequestMapping("/usuarios/{ownerId}/ordenes")
public class OrdenController implements OwnedCrudController<Long, Long, OrdenInput, OrdenOutput> {
    // ...
}
```

---

## Jerarquía de clases

```
CrudProvider<ID, INPUT, OUTPUT>                                        (core — interfaz)
└── EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT>                      (core — abstracta)
    ├── JpaCrudProvider<ENTITY, ID, INPUT, OUTPUT>                     (jpa — abstracta)
    │   └── FilterableJpaCrudProvider<ENTITY, ID, INPUT, OUTPUT>       (jpa — abstracta)
    └── SpringRepositoryCrudProvider<ENTITY, ID, INPUT, OUTPUT>        (spring-data — abstracta)

OwnedCrudProvider<OWNER_ID, ID, INPUT, OUTPUT>                         (core — interfaz)
└── OwnedEntityCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT>       (core — abstracta)
    └── FilterableOwnedJpaCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT>  (jpa — abstracta)
```

---

## Requisitos

- Java 25+
- Jakarta Validation API 3.1+
- Spring Boot 4.0+ *(solo módulo spring-data)*

---

## Licencia

Licenciado bajo [Apache License 2.0](LICENSE).
