# Microservicio de Tecnologías — HU1: Registrar Tecnologías

Microservicio reactivo (Spring WebFlux + R2DBC/MySQL) que expone el registro de tecnologías siguiendo **arquitectura hexagonal** (puertos y adaptadores). Implementa la **Historia de Usuario 1 (HU1)**: registrar una tecnología con nombre y descripción, validando obligatoriedad, longitudes y unicidad del nombre.

## Tabla de contenido

- [Funcionalidad](#funcionalidad)
- [Arquitectura hexagonal](#arquitectura-hexagonal)
- [Programación reactiva: Mono, Flux y operadores](#programación-reactiva-mono-flux-y-operadores)
- [Recorrido del flujo de una petición](#recorrido-del-flujo-de-una-petición)
- [Reglas de negocio](#reglas-de-negocio)
- [API REST](#api-rest)
- [Estrategia de pruebas](#estrategia-de-pruebas)
- [Cómo ejecutar](#cómo-ejecutar)

## Funcionalidad

El endpoint `POST /api/v1/technologies` recibe un nombre y una descripción y registra una tecnología nueva. Antes de persistir:

1. **Normaliza** los valores aplicando `trim` (elimina espacios al inicio y al final).
2. **Valida** obligatoriedad y longitud: nombre 1–50 caracteres, descripción 1–90 caracteres.
3. **Verifica unicidad** del nombre, sin distinguir mayúsculas/minúsculas (`Java` y `java` se consideran el mismo nombre).
4. **Persiste** en MySQL vía R2DBC y devuelve la tecnología creada con su `id` autogenerado.

Cualquier fallo se traduce a una respuesta HTTP uniforme: `400` (datos inválidos), `409` (nombre duplicado), `500` (error inesperado).

## Arquitectura hexagonal

El código separa el **dominio** (reglas de negocio puras, sin Spring) de la **infraestructura** (adaptadores que hablan con el mundo exterior). El dominio define **puertos** (interfaces) y la infraestructura provee **adaptadores** (implementaciones).

```
src/main/java/com/bootcamp/technology
├── domain                      # Núcleo puro (sin anotaciones de framework)
│   ├── model/Technology            # Modelo de dominio inmutable
│   ├── api/ITechnologyServicePort  # Puerto de ENTRADA (lo consume la capa web)
│   ├── spi/ITechnologyPersistencePort # Puerto de SALIDA (lo implementa persistencia)
│   ├── usecase/TechnologyUseCase   # Reglas de negocio (implementa el puerto de entrada)
│   └── exception/                  # Errores de dominio + códigos
├── application/config          # Cableado de beans (wiring) + config OpenAPI/R2DBC
└── infrastructure/adapters
    ├── driving/webflux         # Adaptador de ENTRADA (HTTP)
    │   ├── router/                 # RouterFunction (rutas funcionales)
    │   ├── handler/                # Handler que compone el pipeline reactivo
    │   ├── dto/                     # Request/Response/Error (transporte)
    │   ├── mapper/                  # DTO <-> dominio
    │   └── exception/               # Handler global de errores reactivo
    └── driven/r2dbc            # Adaptador de SALIDA (base de datos)
        ├── entity/                  # Entidad @Table de R2DBC
        ├── repository/              # ReactiveCrudRepository
        ├── mapper/                  # entidad <-> dominio
        └── adapter/                 # Implementa ITechnologyPersistencePort
```

Ventaja clave: el dominio (`TechnologyUseCase`, `Technology`, los puertos) **no conoce Spring, HTTP ni R2DBC**. Se puede probar de forma unitaria con mocks, y los adaptadores se pueden cambiar (por ejemplo, otra base de datos) sin tocar las reglas de negocio. El cableado se hace en `BeanConfiguration`, por eso las clases de dominio no llevan `@Component`.

## Programación reactiva: Mono, Flux y operadores

Este servicio es **100% no bloqueante**. Nunca se usa `.block()` en el código de producción; todo se compone con operadores de **Project Reactor**.

### ¿Qué son `Mono` y `Flux`?

Son los dos tipos publicadores (Publisher) de Reactor:

- **`Mono<T>`**: un flujo asíncrono que emite **0 o 1** elemento, y luego completa o falla. Aquí lo usamos en todas partes porque registrar una tecnología produce **un** resultado (la tecnología creada) o **un** error.
- **`Flux<T>`**: un flujo asíncrono que emite **0..N** elementos. No lo necesitamos en HU1 (no listamos colecciones), pero es la contraparte de `Mono` para múltiples elementos y se usaría, por ejemplo, en un futuro `GET /technologies` que devuelva varias filas.

La idea central: en lugar de ejecutar y **esperar bloqueando** un resultado, describimos un **pipeline** de transformaciones que se ejecuta cuando alguien se **suscribe** (en WebFlux, el framework se suscribe por nosotros al enviar la respuesta HTTP). Nada se ejecuta hasta la suscripción (evaluación perezosa).

### Operadores usados en este proyecto

| Operador | Dónde | Para qué sirve |
|----------|-------|----------------|
| `flatMap` | `TechnologyUseCase`, `TechnologyHandler`, `TechnologyPersistenceAdapter` | Encadena un paso que **devuelve otro publisher** (asíncrono). Aplana `Mono<Mono<T>>` en `Mono<T>`. Se usa para encadenar validación → unicidad → guardado, donde cada paso es reactivo. |
| `map` | `TechnologyHandler`, `TechnologyPersistenceAdapter` | Transforma el valor con una función **síncrona** (por ejemplo, entidad → dominio, dominio → DTO). No devuelve un publisher. |
| `Mono.just(x)` | `TechnologyUseCase` | Crea un `Mono` que emite un valor ya disponible. |
| `Mono.error(ex)` | `TechnologyUseCase` | Crea un `Mono` que **falla** con una excepción. Cortocircuita el pipeline: los pasos siguientes (`flatMap`) no se ejecutan, así nunca se persiste tras una validación fallida. |
| `Mono.fromCallable(fn)` | `TechnologyPersistenceAdapter.save` | Envuelve una operación potencialmente lanzadora (el mapeo dominio→entidad) dentro del contexto reactivo, difiriéndola hasta la suscripción. |
| `onErrorMap(Clazz, fn)` | `TechnologyPersistenceAdapter.save` | Traduce un tipo de error en otro. Aquí convierte `DataIntegrityViolationException` (choque con la restricción UNIQUE en una posible carrera) en `TechnologyAlreadyExistsException` para reforzar el `409`. |
| `.as(transactionalOperator::transactional)` | `TechnologyPersistenceAdapter.save` | Envuelve el pipeline de escritura en una **transacción reactiva**. |
| `bodyToMono(Clazz)` | `TechnologyHandler` | Deserializa el cuerpo JSON de la petición a un `Mono<DTO>` de forma no bloqueante. |
| `bodyValue(x)` / `ServerResponse.status(...)` | `TechnologyHandler`, `GlobalErrorWebExceptionHandler` | Construye la respuesta HTTP reactiva. |

Nota sobre "las funciones propias como `.list`": en el código de producción de HU1 no se usa un operador `.list` de Reactor (ese patrón, como `Flux#collectList`, aparecería al **listar** varias tecnologías). Donde sí ves listas y generadores es en los **tests de propiedades con jqwik**, mediante `Arbitraries.strings().list()...` para construir datos de prueba. Ver la sección de pruebas.

### La "regla de oro" reactiva

Nunca bloquear. En lugar de `technology = repo.save(...).block()`, componemos: `repo.save(...).map(...).flatMap(...)`. El resultado es un pipeline que Spring WebFlux ejecuta sobre un número reducido de hilos de event-loop, escalando mejor bajo carga que el modelo bloqueante de un hilo por petición.

## Recorrido del flujo de una petición

`POST /api/v1/technologies` con `{ "name": "  Java  ", "description": "Lenguaje" }`:

1. **`TechnologyRouter`** declara la ruta funcional (`RouterFunction`) y la delega en el handler. La documentación OpenAPI se declara con `@RouterOperation` porque las rutas funcionales no se auto-documentan como los `@RestController`.
2. **`TechnologyHandler.register`** compone el pipeline:
   `bodyToMono(TechnologyRequest) → map(toDomain) → flatMap(servicePort::registerTechnology) → map(toResponse) → flatMap(ServerResponse 201)`.
3. **`TechnologyUseCase.registerTechnology`** (dominio) ejecuta las reglas:
   `validate(...) → flatMap(ensureNameIsUnique) → flatMap(persistencePort::save)`.
   - `validate` aplica `trim`, comprueba obligatoriedad y longitudes; ante fallo emite `Mono.error(InvalidTechnologyDataException)`.
   - `ensureNameIsUnique` consulta el puerto; si el nombre existe emite `Mono.error(TechnologyAlreadyExistsException)` **sin** llegar a `save`.
4. **`TechnologyPersistenceAdapter.save`** mapea dominio→entidad, guarda con el repositorio R2DBC, mapea entidad→dominio, todo dentro de una transacción reactiva; traduce violaciones de UNIQUE a `409`.
5. Si algo falla en cualquier punto, el error viaja por el pipeline hasta **`GlobalErrorWebExceptionHandler`**, que lo traduce a un `ErrorResponse` JSON con el código HTTP adecuado.

## Reglas de negocio

| Regla | Detalle | Error / código HTTP |
|-------|---------|---------------------|
| Nombre obligatorio | No puede ser null/vacío/solo espacios (tras `trim`) | `NAME_REQUIRED` → 400 |
| Longitud del nombre | Máximo 50 caracteres (tras `trim`) | `NAME_TOO_LONG` → 400 |
| Descripción obligatoria | No puede ser null/vacía/solo espacios (tras `trim`) | `DESCRIPTION_REQUIRED` → 400 |
| Longitud de la descripción | Máximo 90 caracteres (tras `trim`) | `DESCRIPTION_TOO_LONG` → 400 |
| Unicidad del nombre | Case-insensitive; si ya existe se rechaza | `TechnologyAlreadyExistsException` → 409 |
| No persistir ante error | Ninguna validación/unicidad fallida debe escribir en BD | invariante verificada por tests |

El esquema (`schema.sql`) refuerza estas reglas en la BD: `name VARCHAR(50)`, `description VARCHAR(90)` y `CONSTRAINT uq_technology_name UNIQUE (name)`.

## API REST

### Registrar tecnología

`POST /api/v1/technologies`

Request body:

```json
{
  "name": "Java",
  "description": "Lenguaje de programación orientado a objetos"
}
```

Respuesta `201 Created`:

```json
{
  "id": 1,
  "name": "Java",
  "description": "Lenguaje de programación orientado a objetos"
}
```

Respuesta de error (`400` / `409`):

```json
{
  "status": 409,
  "code": "CONFLICT",
  "message": "La tecnología 'Java' ya está registrada",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

Documentación interactiva (Swagger UI): `http://localhost:8080/swagger-ui.html`.

## Estrategia de pruebas

El reto exige pruebas para cada regla de negocio. Se combinan tres niveles:

### 1. Tests unitarios del caso de uso — `TechnologyUseCaseTest`

JUnit 5 + Mockito + `StepVerifier`. Mockean el puerto de persistencia y verifican **ejemplos y casos borde** de cada regla: registro válido, límites mínimos (1 carácter) y máximos (50/90), normalización por `trim`, nombre/descripción en blanco, longitudes excedidas (51/91) y nombre duplicado. Cada caso de rechazo verifica además `verify(persistencePort, never()).save(any())` — es decir, que **nunca se persiste** ante un error.

`StepVerifier` es la herramienta de Reactor para probar flujos: se suscribe al `Mono`, y permite afirmar el valor emitido (`assertNext`), la finalización (`verifyComplete`) o el error esperado (`expectError`), sin bloquear.

### 2. Property-based tests (jqwik) — `TechnologyUseCaseProperty01..08Test`

En lugar de ejemplos fijos, jqwik genera **cientos de entradas aleatorias** (mínimo 100 iteraciones por propiedad, `@Property(tries = 100)`) y comprueba que una **propiedad universal** se cumple siempre. El puerto de persistencia se mockea, de modo que estas pruebas validan las reglas del dominio de forma aislada. Los generadores usan la API de jqwik (`Arbitraries`, `Arbitrary`, `Combinators`, y construcciones como `Arbitraries.strings().list()` para generar rellenos de espacios).

Propiedades cubiertas:

| Test | Propiedad |
|------|-----------|
| Property 01 | Un registro válido conserva los datos normalizados (`trim`) y persiste exactamente una vez. |
| Property 02 | Un nombre duplicado (case-insensitive) se rechaza y **nunca** persiste. |
| Property 03 | La normalización por `trim` es idempotente en los valores persistidos. |
| Property 04 | Nombre vacío/obligatorio se rechaza sin persistir. |
| Property 05 | Descripción vacía/obligatoria se rechaza sin persistir. |
| Property 06 | Nombre que excede 50 caracteres se rechaza sin persistir. |
| Property 07 | Descripción que excede 90 caracteres se rechaza sin persistir. |
| Property 08 | Invariante global: ante **cualquier** error de validación o unicidad, `save` no se invoca jamás. |

Por qué property-based: en lugar de confiar en unos pocos ejemplos, se prueba la regla frente a un amplio espacio de entradas (nombres con espacios internos, capitalización mixta, longitudes límite, caracteres especiales), aumentando la confianza en que la invariante se sostiene en todos los casos. Si jqwik encuentra un contraejemplo, lo "encoge" (shrinking) hasta el caso mínimo que rompe la propiedad y lo guarda en `.jqwik-database`.

### 3. Tests de integración (Testcontainers) — capa de persistencia y endpoint

Levantan un **MySQL real** en un contenedor Docker mediante Testcontainers, sin mocks de la base de datos.

- **`TechnologyPersistenceAdapterIntegrationTest`**: prueba el adaptador R2DBC de extremo a extremo (adaptador + mapper + repositorio + esquema con UNIQUE) con `StepVerifier`. Verifica que `save` asigna id y persiste la fila, que `existsByNameIgnoreCase` ignora mayúsculas/minúsculas, y que un insert duplicado se traduce a `TechnologyAlreadyExistsException`.
- **`TechnologyEndpointIntegrationTest`**: arranca el contexto completo de Spring Boot y usa `WebTestClient` para ejercitar el endpoint HTTP real (`201`, `409`, `400` en sus variantes), incluyendo la traducción de errores del handler global.

#### Nota técnica: MySQL con Testcontainers en un proyecto solo-R2DBC

Este proyecto **no** incluye el driver JDBC de MySQL (es puramente R2DBC). La clase `MySQLContainer` de Testcontainers, sin embargo, comprueba el arranque del contenedor abriendo una conexión **JDBC** (`SELECT 1`), lo que provocaba `ClassNotFoundException`/`NoDriverFoundException`.

Solución adoptada (**Opción A**): en los tests de integración se levanta MySQL con un `GenericContainer<>("mysql:8.0")` configurado por variables de entorno y una **wait strategy basada en el log de arranque** de MySQL:

```java
new GenericContainer<>("mysql:8.0")
    .withEnv("MYSQL_DATABASE", "technology_db")
    .withEnv("MYSQL_USER", "test")
    .withEnv("MYSQL_PASSWORD", "test")
    .withEnv("MYSQL_ROOT_PASSWORD", "root")
    .withExposedPorts(3306)
    .waitingFor(Wait.forLogMessage(".*port: 3306  MySQL Community Server.*", 1)
        .withStartupTimeout(Duration.ofSeconds(180)));
```

Así se arranca un MySQL real sin depender de ningún driver JDBC, manteniendo la coherencia con el enfoque reactivo. La conexión de la aplicación sigue siendo R2DBC (`spring.r2dbc.*`, enlazada al contenedor mediante `@DynamicPropertySource`).

> Los tests de integración requieren **Docker en ejecución**.

## Cómo ejecutar

Requisitos: JDK 17, Docker (solo para los tests de integración), y un MySQL accesible para ejecutar la app localmente.

Ejecutar la suite de pruebas completa:

```bash
./gradlew clean test
```

Levantar el servicio (usa las variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`; por defecto apunta a `localhost:3306/technology_db`):

```bash
./gradlew bootRun
```

El esquema `schema.sql` se ejecuta automáticamente al arrancar (`spring.sql.init.mode=always`), creando la tabla `technology` si no existe.

Endpoints útiles:

- API: `POST http://localhost:8080/api/v1/technologies`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

**Estado:** HU1 (Registrar Tecnologías) completa — dominio, persistencia y endpoint implementados y cubiertos por tests unitarios, property-based (jqwik) e integración (Testcontainers). Toda la suite pasa en verde.
