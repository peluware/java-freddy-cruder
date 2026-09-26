# Freddy Cruder

[![Maven Central](https://img.shields.io/maven-central/v/com.peluware/freddy-cruder.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.peluware/freddy-cruder)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)

Freddy Cruder es una librería Java modular y agnóstica al framework que estandariza y simplifica las operaciones CRUD en aplicaciones empresariales. Provee una capa de abstracción limpia entre la lógica de aplicación y la tecnología de persistencia, siguiendo el patrón Template Method para garantizar un ciclo de vida consistente en todas las operaciones.

---

## Módulos

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
 bulk-import-excel    bulk-import-csv        (+ spring-data, expone por REST)

                     freddy-cruder-memory  (+ core, para pruebas y prototipos)
```

| Módulo                              | Descripción                                                                                                                                                                                                     |
|-------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `freddy-cruder-core`                | Contratos y abstracciones base. Sin dependencias de framework.                                                                                                                                                  |
| `freddy-cruder-jpa`                 | Implementación JPA via Criteria API. `omni-search-jpa` es una integración opcional para búsqueda full-text y filtrado RSQL.                                                                                     |
| `freddy-cruder-mongodb`             | Implementación MongoDB, espejo del módulo JPA (`MongoCrudProvider`, `FilterableMongoCrudProvider`, `FilterableOwnedMongoCrudProvider`). `omni-search-mongodb` es una integración opcional para búsqueda y RSQL. |
| `freddy-cruder-spring-data`         | Integración con Spring Data: controllers REST, soporte para `CrudRepository` y `SpringCrudOptions`.                                                                                                             |
| `freddy-cruder-spring-data-jpa`     | Fragmento JPA para `freddy-cruder-spring-data`. Autoconfigura `JpaSearchEngine` e integración opcional con omni-search. Úsalo cuando combines Spring Data JPA con el fragmento de búsqueda.                     |
| `freddy-cruder-spring-data-mongodb` | Fragmento MongoDB para `freddy-cruder-spring-data`. Autoconfigura `MongoSearchEngine` e integración opcional con omni-search. Úsalo cuando combines Spring Data MongoDB con el fragmento de búsqueda.           |
| `freddy-cruder-bulk-import`         | Contrato de carga masiva agnóstico de la fuente: los registros de un archivo se convierten en inputs y se crean a través de un `CreateProvider`, con vista previa y plantilla descargable.               |
| `freddy-cruder-bulk-import-excel`   | Fuente Excel (Apache POI) para la carga masiva: lector clásico (`.xls`/`.xlsx`, fórmulas) y lector en streaming (`.xlsx`, memoria constante).                                                                    |
| `freddy-cruder-bulk-import-csv`     | Fuente CSV (Apache Commons CSV) para la carga masiva.                                                                                                                                                            |
| `freddy-cruder-spring-web-bulk-import` | Controllers Spring MVC que exponen un provider de carga masiva: `POST /import`, `POST /import/preview` y `GET /import/template`.                                                                              |
| `freddy-cruder-memory`              | `EntityCrudProvider`s en memoria, pensados para pruebas y prototipos.                                                                                                                                            |

---

## Instalación

Agrega el módulo que necesitas en tu `pom.xml`. Cada módulo incluye sus dependencias transitivamente.

**Solo core** (agnóstico al framework):

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-core</artifactId>
    <version>4.1.0</version>
</dependency>
```

**Soporte JPA:**

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-jpa</artifactId>
    <version>4.1.0</version>
</dependency>
```

**Spring Data + controllers REST:**

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-spring-data</artifactId>
    <version>4.1.0</version>
</dependency>
```

**Spring Data JPA con fragmento de búsqueda (incluye los dos anteriores):**

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-spring-data-jpa</artifactId>
    <version>4.1.0</version>
</dependency>
```

**Soporte MongoDB:**

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-mongodb</artifactId>
    <version>4.1.0</version>
</dependency>
```

**Spring Data MongoDB con fragmento de búsqueda:**

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-spring-data-mongodb</artifactId>
    <version>4.1.0</version>
</dependency>
```

**Carga masiva** (elige la fuente que lees; agrega el módulo REST para exponerla):

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-bulk-import-excel</artifactId> <!-- o freddy-cruder-bulk-import-csv -->
    <version>4.1.0</version>
</dependency>
<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>freddy-cruder-spring-web-bulk-import</artifactId>
    <version>4.1.0</version>
</dependency>
```

El lector Excel en streaming necesita `com.github.pjfanning:excel-streaming-reader`, que es una
dependencia opcional de `freddy-cruder-bulk-import-excel` — agrégala tú para usar
`StreamingExcelBulkImportProvider`.

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

protected abstract List<ENTITY> internalList(String search, String query, Sort sort);

protected abstract long internalCount(String search, String query);

protected abstract boolean internalExists(ID id);

protected abstract ENTITY internalCreate(ENTITY entity);

protected abstract ENTITY internalUpdate(ENTITY entity);

protected abstract void internalDelete(ENTITY entity);
```

`EntityCrudProvider` también implementa `ListProvider<OUTPUT>` (`list(search, query, sort)`, sin
paginación), siguiendo el mismo camino que `page` — misma proyección, mismo mapeo y mismos eventos de
ciclo de vida. Deliberadamente **no** está compuesto en `ReadProvider` ni expuesto por ningún
controller: poder paginar no implica que listar todo un recurso sea sensato, y una lectura sin
paginar es para consumidores de proceso (exports, reportes, procesos masivos), no para endpoints
HTTP. `OwnedEntityCrudProvider` implementa la contraparte con dueño, `OwnedListProvider<OWNER_ID, OUTPUT>`,
de la misma forma.

**Hooks de extensión (overrides opcionales):**

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

protected ENTITY newEntity() { /* basado en reflexión por defecto */ }

protected void afterCreate(INPUT input, ENTITY created) {
}

protected void afterUpdate(INPUT input, ENTITY updated) {
}
```

`afterCreate`/`afterUpdate` se ejecutan justo después de persistir, dentro de la misma transacción, con el DTO de entrada y la entidad ya persistida (con su identificador generado disponible). Úsalos para persistir entidades dependientes que necesitan el identificador del padre pero no tienen una relación directa a nivel de store.

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

Varios manejadores se componen en uno con `EntityCrudEvents.of(primero, segundo)` (o `primero.andAll(otros)`): se ejecutan en orden y se detienen en el primero que lance una excepción.

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
public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSearchRepository<Producto> {
}
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
    FindController<Long, ProductoOutput> { ...
}

// Recurso de solo escritura
public class ProductoController implements CreateController<ProductoInput, ProductoOutput>,
    UpdateController<Long, ProductoInput, ProductoOutput> { ...
}
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

## Carga masiva

Carga registros desde un archivo a través de un `CreateProvider`, así que la validación, el mapeo y los eventos de ciclo de vida corren igual que en un `create` individual. Un `BulkImportProvider` ofrece tres operaciones:

| Operación           | Qué hace                                                                                                                 |
|---------------------|--------------------------------------------------------------------------------------------------------------------------|
| `execute(in)`       | Crea cada registro y devuelve un `BulkImportResult` (`created`, `skipped`). Se detiene en el primer registro que falla.  |
| `preview(in)`       | Informa qué haría `execute`, registro por registro, sin crear nada — y todos los problemas de cada fila.                 |
| `template()`        | Un `ImportTemplate`: el archivo que el usuario llena.                                                                    |

Cada registro de una vista previa es `Created`, `Skipped` (con un motivo — no es un error, la carga sigue) o `Rejected` (con sus problemas). `execute` lanza `BulkImportRecordException`, con la posición y los problemas del registro que falló; ejecútalo dentro de una transacción para que un fallo no deje nada a medias.

### Define una carga

Extiende el provider de la fuente que lees y di cómo una fila se vuelve un input:

```java

@Service
public class ProductoImport extends StreamingExcelBulkImportProvider<ProductoInput, ProductoOutput> {

    public ProductoImport(ProductoService productos) {
        super(productos);
    }

    @Override
    protected String sheetName() {
        return "Productos";
    }

    @Override
    protected ImportConversion<ProductoInput> convert(ExcelRow row) {
        var input = new ProductoInput();
        var binder = new ExcelRowBinder(row);
        binder.required(input::setNombre, 0, "Nombre", ExcelCell::textOrNull, "Falta el nombre.");
        binder.required(input::setCategoria, 1, "Categoría", cell -> cell.enumeration(Categoria.class), "Falta la categoría.");
        return binder.toConversion(input);   // todos los problemas de la fila a la vez
    }

    @Override
    public ImportTemplate template() {
        return new ImportTemplate("productos.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out -> { /* escribe el libro en out */ });
    }
}
```

`convert` también puede devolver `ImportConversion.skipped("Ya existe")` para dejar una fila fuera. Sobreescribe `created(output)` para reaccionar a cada registro creado y `createFailed(position, failure)` para convertir un fallo de persistencia en algo que el usuario pueda corregir.

| Módulo de fuente                  | Providers                                                                                                                                                                                      |
|-----------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `freddy-cruder-bulk-import-excel` | `ExcelBulkImportProvider` (libro completo en memoria; `.xls`/`.xlsx`; evalúa fórmulas) y `StreamingExcelBulkImportProvider` (`.xlsx` fila a fila; las fórmulas se leen como su valor guardado). |
| `freddy-cruder-bulk-import-csv`   | `CsvBulkImportProvider`, con `charset()`, `format()`, `header()` y `firstColumn()`.                                                                                                            |

Prefiere el lector en streaming para cualquier archivo que pueda ser grande: el lector clásico necesita cerca de 150 veces el tamaño del archivo en heap, mientras que el de streaming se mantiene plano. Todos los providers tienen su contraparte `Owned*` que agrega un `OWNER_ID`.

### Exponla por REST

Implementa `BulkImportController` junto a tu controller CRUD:

```java

@RestController
@RequestMapping("/productos")
public class ProductoController implements CrudController<Long, ProductoInput, ProductoOutput>,
    BulkImportController<List<String>, ExcelMetadata> {

    private final ProductoService service;
    private final ProductoImport bulkImport;

    // constructor omitido

    @Override
    public CrudProvider<Long, ProductoInput, ProductoOutput> getService() {
        return service;
    }

    @Override
    public BulkImportProvider<?, List<String>, ExcelMetadata, ?> getBulkImportService() {
        return bulkImport;
    }
}
```

| Método | Ruta                        | Descripción                                   |
|--------|-----------------------------|-----------------------------------------------|
| `POST` | `/productos/import`         | Importa el `file` subido                      |
| `POST` | `/productos/import/preview` | Qué haría el archivo, sin hacerlo             |
| `GET`  | `/productos/import/template`| La plantilla, en streaming hacia la respuesta |

La librería no mapea los errores: `BulkImportRecordException` y `ExcelSheetMissingException` llegan a tu manejador de excepciones, que decide cómo responder.

---

## Jerarquía de clases

```
CrudProvider<ID, INPUT, OUTPUT>                                             (core — interfaz)
├── EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT>                          (core — abstracta)
│   ├── JpaCrudProvider<ENTITY, ID, INPUT, OUTPUT>                         (jpa — abstracta)
│   ├── FilterableJpaCrudProvider<ENTITY, ID, INPUT, OUTPUT>               (jpa — abstracta)
│   ├── MemoryCrudProvider<ENTITY, ID, INPUT, OUTPUT>                      (memory — abstracta)
│   ├── MongoCrudProvider<ENTITY, ID, INPUT, OUTPUT>                       (mongodb — abstracta)
│   ├── FilterableMongoCrudProvider<ENTITY, ID, INPUT, OUTPUT>             (mongodb — abstracta)
│   └── SpringRepositoryCrudProvider<ENTITY, ID, INPUT, OUTPUT>            (spring-data — abstracta)
└── JpaProjectedCrudProvider<ENTITY, ID, PROJECTION, INPUT, OUTPUT>        (jpa — abstracta)

OwnedCrudProvider<OWNER_ID, ID, INPUT, OUTPUT>                                        (core — interfaz)
├── OwnedEntityCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT>                      (core — abstracta)
│   ├── OwnedMemoryCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT>                  (memory — abstracta)
│   ├── FilterableOwnedJpaCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT>           (jpa — abstracta)
│   └── FilterableOwnedMongoCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT>         (mongodb — abstracta)
└── JpaOwnedProjectedCrudProvider<ENTITY, OWNER_ID, ID, PROJECTION, INPUT, OUTPUT>    (jpa — abstracta)
```

`JpaCrudProvider` y `FilterableJpaCrudProvider` son hermanas, no padre/hija — la segunda simplemente agrega un hook `predicateFilter` y hints de query sobre la misma base `EntityCrudProvider` (igual con sus contrapartes de Mongo). `JpaProjectedCrudProvider`/`JpaOwnedProjectedCrudProvider` implementan
`CrudProvider`/`OwnedCrudProvider` directamente en lugar de extender la base de entidad, ya que su camino de lectura devuelve una proyección en vez de la entidad.

---

## Requisitos

- Java 25+
- Jakarta Validation API 3.1+
- Spring Boot 4.0+ *(solo módulo spring-data)*

---

## Licencia

Licenciado bajo [Apache License 2.0](LICENSE).
