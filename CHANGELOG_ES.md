# Registro de cambios

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [4.1.0] — 2026-09-25

Suma la carga masiva: cargar registros desde un archivo — Excel o CSV — a través de un
`CreateProvider`, con una vista previa de lo que el archivo haría antes de hacerlo, una plantilla
descargable y controllers REST que exponen las tres cosas. Nada de 4.0.0 cambia: la versión es
aditiva, y lo único que se toca de un módulo existente son nuevos helpers de composición para
`EntityCrudEvents`.

### Añadido

#### `freddy-cruder-bulk-import` (nuevo)

- `BulkImportProvider<INPUT, PREVIEW, PREVIEW_META, OUTPUT>` y su contraparte con dueño
  `OwnedBulkImportProvider` — el contrato de una carga: `execute(InputStream)`,
  `preview(InputStream)` y `template()`. Agnóstico de la fuente: no sabe nada de Excel, CSV ni HTTP.
  Los registros se crean a través de un `CreateProvider`, así que la validación, el mapeo y los eventos
  de ciclo de vida corren igual que en un `create` individual.
- `ImportConversion<INPUT>` — lo que una fuente dice de un registro: `converted(input)`,
  `unconvertible(problems)` o `skipped(reason)`. `ImportConversion.attempt(Supplier)` convierte en
  `unconvertible` una conversión que lanza una excepción. Un registro omitido no es un error: se
  informa y la carga sigue.
- `ImportBinder` — junta los problemas de un registro en vez de detenerse en el primero, para que el
  usuario reciba todos los errores de una fila de una vez.
- `BulkImportPreview` con `ImportRecordPreview` (`Created`, `Skipped`, `Rejected`), `BulkImportResult`
  (`created`, `skipped`), `ImportProblem` (mensaje y campo opcional), `ImportTemplate` y
  `BulkImportRecordException` (lleva la posición y los problemas del registro que falló).
- Ganchos `created(output)` y `createFailed(position, failure)`, para reaccionar a cada registro creado
  y para traducir los fallos de persistencia a algo que el usuario pueda corregir.

#### `freddy-cruder-bulk-import-excel` (nuevo)

- `ExcelBulkImportProvider` — carga el libro completo; lee `.xls` y `.xlsx` y evalúa fórmulas.
- `StreamingExcelBulkImportProvider` — lee `.xlsx` fila a fila con `excel-streaming-reader`, una
  dependencia **opcional** que agregas tú. Las fórmulas se leen como su valor guardado. La memoria se
  mantiene plana sin importar el archivo: en nuestras mediciones el lector clásico necesita cerca de 150
  veces el tamaño del archivo en heap, mientras que el de streaming corrió con 32 MB fijos.
- Variantes con dueño: `OwnedExcelBulkImportProvider` y `OwnedStreamingExcelBulkImportProvider`.
- `ExcelRow`, `ExcelCell` (`text`, `textOrNull`, `textAs`, `flag`, `integer`, `date`, `enumeration`, …),
  `ExcelRowBinder`, `ExcelLayout` (fila de cabecera, primera fila de datos, primera columna) y
  `ExcelMetadata` (las cabeceras, para la vista previa).
- `ExcelSheetMissingException` cuando el archivo no tiene la hoja con el nombre esperado.

#### `freddy-cruder-bulk-import-csv` (nuevo)

- `CsvBulkImportProvider` y `OwnedCsvBulkImportProvider`, sobre Apache Commons CSV, con `charset()`,
  `format()`, `header()` y `firstColumn()` para adaptarlos al archivo.
- `CsvRow`, `CsvCell`, `CsvRowBinder` y `CsvMetadata`, espejo de los de Excel.

#### `freddy-cruder-spring-web-bulk-import` (nuevo)

- `BulkImportController<PREVIEW, PREVIEW_META>` y `OwnedBulkImportController` exponen un provider como
  `POST /import`, `POST /import/preview` y `GET /import/template`, cada uno envuelto en `CrudContext`
  con los `SpringCrudOptions` de la petición. La plantilla se escribe directo a la respuesta mediante un
  `StreamingResponseBody`, sin almacenarla en memoria.
- Los controllers declaran `getBulkImportService()` en lugar de `getService()`, para que un mismo
  controller pueda implementar también `CrudController`.
- Los errores no se mapean: `BulkImportRecordException` y, en Excel, `ExcelSheetMissingException` llegan
  a tu manejador de excepciones, que decide cómo responder.
- El módulo es solo Spring MVC — de ahí el `web` en su nombre — y vive aparte de
  `freddy-cruder-spring-data`, donde REST es opcional, porque todo lo que contiene es un controller.

#### `freddy-cruder-memory` (nuevo)

- `MemoryCrudProvider` y `OwnedMemoryCrudProvider` — `EntityCrudProvider`s en memoria para pruebas y
  prototipos. Crear, actualizar y eliminar corren en una transacción simulada que se revierte si falla.
  `MemoryStore` y `MemoryIds` son el almacén y la estrategia de identificadores.

#### `freddy-cruder-core`

- `EntityCrudEvents.of(...)` (varargs o `Iterable`) y `andAll(Iterable)` componen varios manejadores de
  eventos en uno, despachando en orden y deteniéndose en el primero que lance una excepción. Los
  manejadores `DEFAULT` se descartan y las composiciones anidadas se aplanan.

### Cambiado

- Build: JUnit 6.1.3 y Maven Surefire 3.6.0 quedan gestionados en el `pom.xml` padre.

### Migración

No hay nada que migrar. Para usar la carga masiva, agrega el módulo de la fuente que necesites
(`freddy-cruder-bulk-import-excel` o `freddy-cruder-bulk-import-csv`) y, para exponerla por REST,
`freddy-cruder-spring-web-bulk-import`. Para leer `.xlsx` en streaming, agrega además
`com.github.pjfanning:excel-streaming-reader`.

---

## [4.0.0] — 2026-09-10

Suma una operación de listado sin paginar junto a `page`, para consumidores de proceso — exports,
reportes, procesos masivos — que hoy arman un `Page` solo para descartarlo via
`Pagination.unpaginated()`.

### Por qué 4.0.0

`EntityCrudProvider` y `OwnedEntityCrudProvider` ganan un nuevo contrato de persistencia requerido,
`internalList`, igual que ya lo es `internalPage`. Cualquier clase que extienda alguna de las dos
directamente — fuera de los módulos JPA, MongoDB y Spring Data, que ya lo implementan — necesita
agregar ese método para seguir compilando.

### Añadido

#### `freddy-cruder-core`

- `ListProvider<OUTPUT>` y `OwnedListProvider<OWNER_ID, OUTPUT>` — contrapartes sin paginar de
  `PageProvider`/`OwnedPageProvider`. Deliberadamente **no** están compuestas en `ReadProvider`/
  `OwnedReadProvider`, ni expuestas por ningún controller: poder paginar no implica que listar todo
  un recurso sea sensato.
- `EntityCrudProvider`/`OwnedEntityCrudProvider` implementan `ListProvider`/`OwnedListProvider`,
  siguiendo el mismo camino que `page` — misma proyección, mismo mapeo y mismos eventos de ciclo de vida.
- `CrudOperation.LIST`.

#### `freddy-cruder-jpa`

- `internalList` en `JpaCrudProvider`, `FilterableJpaCrudProvider` y `FilterableOwnedJpaCrudProvider`,
  reutilizando el constructor sin paginar que ya tenía `EntityListQuery`.
- `JpaProjectedCrudProvider`/`JpaOwnedProjectedCrudProvider` implementan `ListProvider`/
  `OwnedListProvider` directamente — sin método abstracto nuevo, ya que `list` reutiliza los mismos
  hooks `selection()`/`groupBy()`/`searchPredicate()` que ya usa `page`.

#### `freddy-cruder-mongodb`

- `internalList` en `MongoCrudProvider`, `FilterableMongoCrudProvider` y
  `FilterableOwnedMongoCrudProvider`.
- `MongoQueryHelpers.find(MongoCollection, Bson, Sort)` — sobrecarga sin paginar.

#### `freddy-cruder-spring-data`

- `SearchEngine`/`SearchRepository` ganan un `findAllBySearch(..., Sort)` nativo y sin paginar que
  retorna `List<T>` directamente — sin `Pageable`/`Page` de por medio. Una implementación propia de
  `SearchEngine` también debe agregarlo. `internalList` en `SpringRepositoryCrudProvider` lo llama
  directamente.

#### `freddy-cruder-spring-data-jpa`

- `JpaSearchEngine.findAllBySearch(..., Sort)` implementa la sobrecarga nativa reutilizando
  `EntityListQuery` con `Pageable.unpaged(sort)` — un detalle interno; el método del fragmento en sí
  no recibe ni retorna `Pageable`/`Page`.

#### `freddy-cruder-spring-data-mongodb`

- `MongoSearchEngine.findAllBySearch(..., Sort)` implementa la sobrecarga nativa via
  `Query.with(Sort)` — sin `Pageable` en absoluto, ni siquiera internamente.

### Arreglado

#### `freddy-cruder-spring-data`

- `PeluwareToSpringAdapters.toPageable` descartaba en silencio el `Sort` pedido cuando `Pagination`
  no estaba paginado, porque retornaba `Pageable.unpaged()` en lugar de `Pageable.unpaged(sort)`.
  Cualquier llamada a `page(search, query, Pagination.unpaginated(), sort)` sobre un
  `SpringRepositoryCrudProvider` — el mismo patrón que `list()` reemplaza en esta versión — devolvía
  resultados en un orden no especificado, ignorando `sort` por completo.

### Guía de migración

Si extendés `EntityCrudProvider` u `OwnedEntityCrudProvider` directamente (no via
`FilterableJpaCrudProvider`, `MongoCrudProvider`, o `SpringRepositoryCrudProvider`), implementá el
nuevo método `internalList`:

```java
// EntityCrudProvider
protected List<ENTITY> internalList(@Nullable String search, @Nullable String query, Sort sort) {
    // retorna cada entidad que coincida con search/query, ordenada
}
```

---

## [3.2.1] — 2026-09-09

### Cambiado

#### `freddy-cruder-jpa`

- `searchPredicate` ya no es `final` en `JpaCrudProvider`, `FilterableJpaCrudProvider`,
  `FilterableOwnedJpaCrudProvider`, `JpaProjectedCrudProvider` y `JpaOwnedProjectedCrudProvider`. Era
  el único punto de personalización que había quedado no-overridable mientras `predicateFilter`,
  `buildIdPredicate`, `buildOwnerPredicate` y `getQueryHints()` ya lo eran; esto elimina esa
  inconsistencia.

#### `freddy-cruder-spring-data-jpa`

- `searchPredicate` y `count` de `JpaSearchEngine` ahora son `protected` en lugar de `private`, para
  que una subclase pueda reutilizarlos.

Esto es puramente una relajación de visibilidad — nada que fuera overridable antes cambia de
comportamiento por defecto ahora; el código existente no se ve afectado de ninguna forma.

---

## [3.2.0] — 2026-08-29

### Arreglado

#### `freddy-cruder-jpa`

- `JpaUtils.findPath` ya no fuerza un join solo para leer el id de una asociación de un solo valor
  (p. ej. `"customer.id"`). Cuando el último segmento del path es el propio id del target, ahora
  navega el path directamente (`from.get("customer").get("id")`), que la mayoría de proveedores JPA
  resuelven contra la columna FK local sin tocar la tabla asociada. Esto también arregla los checks
  `isNull`/`isNotNull` sobre ese tipo de path, que antes nunca podían matchear: un join `INNER`
  descartaba en silencio las filas con la asociación en null antes de que el predicado se evaluara.

  Este atajo solo evita el join en el lado **dueño** de la asociación (`@ManyToOne`, o un `@OneToOne`
  dueño con `@JoinColumn`) — ahí el id ya es la columna FK local, así que el `joinType` pedido
  (`INNER`/`LEFT`) no importa de todas formas. Para el lado **inverso** de un `@OneToOne` (`mappedBy`),
  igual se necesita un join y sigue ocurriendo; la única brecha conocida es que la navegación implícita
  de path usada para llegar a él por defecto es `INNER` según JPQL, sin importar el `joinType`
  originalmente pedido — así que ordenar/filtrar por el id de un `@OneToOne` inverso nullable podría
  descartar filas que un `LEFT` pedido explícitamente hubiera conservado. Las asociaciones inversas de
  un solo valor son raras en un dominio bien modelado; este caso borde queda como limitación conocida
  por ahora, no como riesgo de pérdida silenciosa de datos — el join siempre ocurre, solo puede diferir
  su tipo del que se pidió.
- `SearchPredicateBuilder.build` (y la implementación de `OmniSearchPredicateAdapter`) ahora anota su
  tipo de retorno como `@Nullable`, coincidiendo con el contrato "`null` significa sin restricción"
  que el resto del modelo de queries (`JpaPredicate`) ya seguía. Sin cambio de comportamiento — esto
  corrige un javadoc desactualizado que decía que el retorno "no debe ser null".

---

## [3.1.0] — 2026-08-22

Sube la integración opcional con `omni-search` a 2.5.0. Ninguna API Java de `freddy-cruder` cambia — es un bump de dependencia transitiva — pero cambia el **comportamiento** de la búsqueda RSQL, así que leé la nota de abajo antes de actualizar si tu proyecto busca sobre campos nulables.

### ⚠️ Cambio de comportamiento — `field==null` en queries RSQL

`omni-search` 2.5.0 introduce operadores de nulidad dedicados y sin valor, y deja de tratar el texto literal `"null"` como un valor mágico:

| Query            | 2.4.0 y anteriores | 2.5.0+                                        |
|------------------|--------------------|-----------------------------------------------|
| `field==null`    | chequeo `IS NULL`  | comparación literal contra el string `"null"` |
| `field=null=`    | *(no soportado)*   | chequeo `IS NULL`                             |
| `field=notnull=` | *(no soportado)*   | chequeo `IS NOT NULL`                         |

Si alguna query RSQL que tu aplicación envía — desde un filtro guardado, un frontend, o un script — usa `field==null` para decir "es nulo", **dejará de matchear esas filas en silencio** tras actualizar, porque ahora matchea la palabra literal "null" en su lugar. Buscá `field==null` en tu código y reemplazalo por `field=null=`, y tratá `field==''` como el string vacío de ahora en más — ya no se coerciona a `null`.

Esto solo afecta a quienes consumen `freddy-cruder-jpa`/`freddy-cruder-mongodb` usando
`OmniSearchPredicateAdapter`/`OmniSearchFilterAdapter` (el `SearchPredicateBuilder`/`SearchFilterBuilder`
por defecto) con queries RSQL que filtran por nulidad. Un `SearchPredicateBuilder`/`SearchFilterBuilder`
propio no se ve afectado.

### Cambiado

- La dependencia opcional `omni-search-jpa`/`omni-search-mongodb` pasa a 2.5.0, que bifurca el parser RSQL a un fork mantenido (`io.github.nstdio:rsql-parser`, mismos paquetes `cz.jirutka.rsql.parser.*`
  — no hace falta cambiar código fuente). Si tu proyecto también declara
  `cz.jirutka.rsql:rsql-parser` directamente, quítala para evitar dos jars proveyendo los mismos paquetes.

---

## [3.0.1] — 2026-07-25

### Añadido

#### `freddy-cruder-jpa`

- `JpaGroupBy` — nuevo eje del modelo de queries, que construye la cláusula `GROUP BY`.
  `JpaGroupBy.self()` agrupa por el source de la query como un todo, permitiendo que una proyección seleccione libremente cualquiera de sus columnas junto con un agregado sobre una asociación plural unida; `JpaGroupBy.none()` (el default) no aplica agrupamiento.
- `ListQuery`/`FindQuery` ganaron un parámetro `groupBy` (opcional — los constructores existentes sin él siguen funcionando), y `JpaProjectedCrudProvider`/`JpaOwnedProjectedCrudProvider` ganaron un hook
  `groupBy()` aplicado a `page`/`find`, con default de ningún agrupamiento.

Esto es puramente aditivo — cada parámetro nuevo tiene una sobrecarga/default que lo cubre, así que el código existente sigue compilando y comportándose igual.

---

## [3.0.0] — 2026-07-21

Esta versión suma MongoDB como store de primera clase junto a JPA, y reemplaza los antiguos helpers ad-hoc de la Criteria API por un modelo de queries pequeño y componible. Varias APIs se renombraron o cambiaron de forma en el camino, por eso es una versión mayor.

### Por qué 3.0.0

`freddy-cruder-jpa` había crecido de forma orgánica alrededor de un puñado de helpers estáticos (`JpaQueryHelpers`, `JpaCriteriaExecutor`, `JpaCriteriaCallback`). Funcionaban, pero cada nueva forma de query implicaba más sobrecargas. Esta versión los reemplaza por un pequeño conjunto de piezas componibles — un source, una selección, un filtro, un ordenamiento y un resultado — que se combinan en objetos de query reutilizables y con nombre (`CountQuery`, `ExistsQuery`, `ListQuery`, `FindQuery`, y
sus atajos `Entity*` para el caso común de "la entidad completa"). La misma forma ahora también impulsa MongoDB, así que ambos stores se leen igual.

### Añadido

#### `freddy-cruder-mongodb` *(módulo nuevo)*

- `MongoCrudProvider` / `FilterableMongoCrudProvider` / `FilterableOwnedMongoCrudProvider` — las contrapartes en MongoDB de los providers JPA, construidas sobre el driver síncrono de MongoDB.
- `SearchFilterBuilder` — estrategia de filtrado full-text/RSQL conectable, con `omni-search-mongodb`
  como opción por defecto via `OmniSearchFilterAdapter`.

#### `freddy-cruder-spring-data-mongodb` *(módulo nuevo)*

- `MongoSearchRepository` / `DefaultMongoSearchRepository` / `MongoSearchEngine` — el fragmento de búsqueda para repositorios Spring Data MongoDB, autoconfigurado igual que el de JPA.

#### `freddy-cruder-jpa`

- Un modelo de queries componible en el nuevo paquete `com.peluware.freddy.cruder.jpa.query`: arma una query a partir de piezas independientes (source, selección, filtro, ordenamiento, resultado, hints) y ejecútala con `JpaQueryExecutor`, o encadena `query.exec(entityManager)` directamente.
- Objetos de query reutilizables — `CountQuery`, `ExistsQuery`, `ListQuery`, `FindQuery` — más las variantes `Entity*` para el caso común de una entidad completa a partir de su clase.
- `JpaHints` — constantes con nombre y factories para los hints estándar de Jakarta Persistence (fetch/load graph, timeouts, modo de caché), en lugar de strings de hint escritos a mano.
- `JpaProjectedCrudProvider` / `JpaOwnedProjectedCrudProvider` — leen entidades a través de una proyección liviana (p. ej. un DTO con `cb.construct(...)`) en lugar de cargar la entidad completa, mientras que las escrituras siguen operando sobre la entidad real.

#### `freddy-cruder-core`

- Hooks `afterCreate(INPUT, ENTITY)` / `afterUpdate(INPUT, ENTITY)` en `EntityCrudProvider` y
  `OwnedEntityCrudProvider` (variantes con dueño), ejecutados justo después de persistir, con el DTO de entrada y la entidad ya persistida disponibles. Útiles para persistir entidades dependientes que necesitan el identificador generado del padre y no tienen una relación directa a nivel de store.

### Cambiado

#### `freddy-cruder-spring-data`

- `SearchRepositoryEngine` renombrado a `SearchEngine`.

#### `freddy-cruder-spring-data-jpa`

- `JpaSearchRepositoryEngine` renombrado a `JpaSearchEngine`.

#### `freddy-cruder-jpa`

- `buildIdPredicate` y `buildOwnerPredicate` ahora construyen un `JpaPredicate` reutilizable en lugar de un `Predicate` de Criteria crudo, y reciben solo el valor del id/owner — el source de la entidad lo aporta la query que los usa, no quien los llama.

### Eliminado

#### `freddy-cruder-jpa`

- `JpaQueryHelpers`, `JpaCriteriaExecutor`, `JpaCriteriaCallback` — reemplazados por el modelo de queries de arriba.
- El hook `runQuery` de `FilterableOwnedJpaCrudProvider` — ya no hace falta con el nuevo modelo de queries.

### Guía de migración

**Motores renombrados:**

| Antes                       | Después           |
|-----------------------------|-------------------|
| `SearchRepositoryEngine`    | `SearchEngine`    |
| `JpaSearchRepositoryEngine` | `JpaSearchEngine` |

**Overrides de predicados personalizados**, si sobreescribiste `buildIdPredicate` o `buildOwnerPredicate`:

```java
// Antes
protected Predicate buildIdPredicate(Root<ENTITY> root, CriteriaBuilder cb, ID id) {
    return cb.equal(root.get("id"), id);
}

// Después
protected JpaPredicate<ENTITY> buildIdPredicate(ID id) {
    return (from, cb) -> cb.equal(from.get("id"), id);
}
```

**Queries Criteria personalizadas**, si usabas `JpaQueryHelpers`/`JpaCriteriaExecutor` directamente:

```java
// Antes
JpaQueryHelpers.query(entityManager, Product.class, Product.class, filter, JpaCriteriaExecutor.list(sort, pagination));

// Después
new EntityListQuery<>(Product.class, filter, sort, pagination).exec(entityManager);
```

---

## [2.1.0] — 2026-06-26

### Añadido

#### `freddy-cruder-spring-data-jpa` *(módulo nuevo)*

- `JpaSearchRepository<T>` — interfaz de fragmento para repositorios JPA. Extiéndela junto a `JpaRepository` y el fragmento de búsqueda se cablea automáticamente via `spring.factories`.
- `DefaultJpaSearchRepository<T>` — implementación del fragmento respaldada por `JpaSearchRepositoryEngine`. Implementa `RepositoryMetadataAccess` para que `RepositoryMethodContext` esté disponible durante la ejecución.
- `JpaSearchRepositoryEngine` — implementación de `SearchRepositoryEngine` usando JPA Criteria API. Delega la construcción de predicados a `SearchPredicateBuilder`.
- `FreddyCruderJpaSearchAutoConfiguration` — autoconfigura `JpaSearchRepositoryEngine` y, cuando `omni-search-jpa` está en el classpath, `JpaOmniSearchPredicateBuilder`, `JpaOmniSearch` y `OmniSearchPredicateAdapter` como `SearchPredicateBuilder`.

#### `freddy-cruder-spring-data`

- `SpringRepositoryCrudProvider` ahora expone constructores de tipo intersección `<R extends CrudRepository<E,ID> & SearchRepository<E>>`. Pasa un único repositorio que satisfaga ambos contratos sin necesitar una interfaz nominada intermedia.
- `SpringPage<T>` — tipo puente interno que extiende `Page<T>` de peluware conservando el `Page<T>` original de Spring. Elimina el round trip `SpringPage → PeluwarePage → SpringPage` cuando se usan `PageController` y `SpringRepositoryCrudProvider` juntos.

### Eliminado

#### `freddy-cruder-spring-data`

- **`CrudSearchRepository<ENTITY, ID>`** — eliminado. El mecanismo de fragmentos de Spring Data solo escanea las interfaces directas y no `@NoRepositoryBean` de un repositorio concreto; esta interfaz nunca era alcanzable por ese scan y por tanto nunca funcionó como habilitador de fragmentos. Usa `extends JpaRepository<E,ID>, JpaSearchRepository<E>` en su lugar.
- **`JpaCrudSearchRepository<ENTITY, ID>`** — eliminado por la misma razón.

### Cambiado

#### `freddy-cruder-spring-data`

- `SearchRepository` ya no depende de tipos de `peluware-domain`. La sobrecarga `findBySearch(String, String, Pagination, Sort)` — que retornaba `com.peluware.domain.Page<T>` — ha sido eliminada. La interfaz ahora solo declara métodos con tipos de Spring Data (`Pageable`, Spring `Page<T>`). El patrón de delegación mutua entre las dos sobrecargas, que podía producir `StackOverflowError` si ninguna era sobreescrita, desaparece completamente.
- `SearchRepository.findBySearch` renombrado a `findAllBySearch`. El nombre anterior coincidía con el patrón de derivación de queries `findBy*` de Spring Data, lo que causaba `QueryCreationException` al arrancar. El nuevo nombre evita esa colisión.
- `SearchRepository.findAllBySearch` y `countBySearch` son ahora métodos `default` (lanzan `UnsupportedOperationException`) en lugar de abstractos. Esto impide que Spring Data intente derivar queries para métodos que coinciden con sus convenciones de nomenclatura; la implementación del fragmento los sobreescribe antes de que sean invocados.
- `SpringToPeluwareAdapters.toPage()` ahora devuelve `SpringPage` en lugar de `Page` plano, habilitando el cortocircuito en `PeluwareToSpringAdapters`.
- `PeluwareToSpringAdapters.toPage()` desenvuelve `SpringPage` directamente en lugar de re-envolver en `PageImpl`.

### Guía de migración

**Definición del repositorio:**

```java
// Antes (nunca funcionó)
interface ProductoRepository extends CrudSearchRepository<Producto, Long> {
}

// Después
interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSearchRepository<Producto> {
}
```

**Constructor del servicio:**

```java
// Antes
public ProductoService(ProductoRepository repo) {
    super(repo, repo, Producto.class); // había que pasar dos veces
}

// Después — el tipo intersección acepta un único argumento
public ProductoService(ProductoRepository repo) {
    super(repo, Producto.class);
}
```

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

- **`SpringCrudProvider` y `SpringOwnedCrudProvider` eliminados.** Estas interfaces exponían un método default `page(Pageable)` que llamaba a `SpringDataAdapters.page(this, ...)` internamente. En la práctica este patrón era frecuentemente problemático: llamar a `this.page(...)` desde dentro de una subclase concreta del provider saltaba el proxy de Spring, por lo que cualquier advice AOP (`@Transactional`, `@Cacheable`, etc.) no aplicaba silenciosamente. La paginación ahora se maneja a nivel de
  controller mediante `PeluwareToSpringAdapters.page(provider, ...)`, donde la llamada pasa por el proxy correctamente.
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

| Antes                                          | Después                                        |
|------------------------------------------------|------------------------------------------------|
| `SpringDataAdapters.toPeluwarePagination(p)`   | `SpringToPeluwareAdapters.toPagination(p)`     |
| `SpringDataAdapters.toPeluwareSort(s)`         | `SpringToPeluwareAdapters.toSort(s)`           |
| `SpringDataAdapters.toPeluwareOrders(s)`       | `SpringToPeluwareAdapters.toOrders(s)`         |
| `SpringDataAdapters.toSpringSort(s)`           | `PeluwareToSpringAdapters.toSort(s)`           |
| `SpringDataAdapters.toSpringPageable(p, s)`    | `PeluwareToSpringAdapters.toPageable(p, s)`    |
| `SpringDataAdapters.withSpringPageable(p, fn)` | `PeluwareToSpringAdapters.apply(p, fn)`        |
| `SpringDataAdapters.page(provider, ...)`       | `PeluwareToSpringAdapters.page(provider, ...)` |

**Clases Spring eliminadas:**

| Antes                                        | Después                                                                  |
|----------------------------------------------|--------------------------------------------------------------------------|
| `extends SpringEntityCrudProvider<...>`      | `extends EntityCrudProvider<...>`                                        |
| `extends SpringOwnedEntityCrudProvider<...>` | `extends OwnedEntityCrudProvider<...>`                                   |
| `implements SpringCrudProvider<...>`         | `PeluwareToSpringAdapters.page(provider, ...)` en el controller          |
| `implements SpringOwnedCrudProvider<...>`    | `PeluwareToSpringAdapters.page(provider, ownerId, ...)` en el controller |

---

## [1.x]

Ver el [historial de commits](https://github.com/PeluWare/freddy-cruder/commits/main) para cambios anteriores a 2.0.0.
