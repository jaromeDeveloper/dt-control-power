# DT Control Power

DT Control Power es una plataforma de demostración para monitorear procesos de negocio y su trazabilidad. La solución evoluciona como un monolito modular React + Spring Boot, con PostgreSQL como fuente de verdad y observabilidad gradual mediante Dynatrace.

El [plano maestro](docs/architecture/DT_CONTROL_POWER_ARCHITECTURE.md) define la arquitectura objetivo y la [matriz de implementación](docs/architecture/IMPLEMENTATION_STATUS.md) registra el avance real por incremento.

## Arquitectura inicial

```text
Navegador → React + Vite → /api/v1/health → Spring Boot → JSON
```

- `frontend/`: React, TypeScript y Vite.
- `backend/`: Java 21, Spring Boot y Maven Wrapper.
- `database/`: documentación y fixtures; las migraciones Flyway viven en el backend.
- `deploy/`: reservado para Docker Compose, NGINX y scripts manuales.
- `docs/`: documentación de arquitectura y observabilidad.

La base actual incorpora persistencia con Flyway/JPA, catálogo de áreas, autenticación por sesión y el flujo real de carga de archivo → proceso → checkpoints. La instrumentación específica de Dynatrace pertenece a un incremento posterior.

## Requisitos para Windows

- Java 21 disponible en `PATH`.
- Node.js 22 y npm 10 o versiones compatibles con `frontend/package.json`.
- PowerShell.

No es necesario instalar Maven globalmente: el backend incluye Maven Wrapper.

## Instalación

Instala las dependencias del frontend desde la raíz del repositorio:

```powershell
cd frontend
npm ci
```

Las dependencias Maven se descargan automáticamente al ejecutar el wrapper por primera vez.

## PostgreSQL

El backend requiere estas variables de entorno:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/dt_control_power"
$env:DB_USERNAME = "dt_control_power"
$env:DB_PASSWORD = "<valor-local-no-versionado>"
```

El archivo `deploy/env.example` documenta los nombres esperados sin contener credenciales reales. Flyway aplica automáticamente las migraciones al arrancar y JPA valida que el esquema coincida con las entidades. No uses `ddl-auto=create` ni mantengas migraciones duplicadas en `database/`.

## Archivos y procesos

Los archivos se guardan en un directorio privado configurable, nunca dentro de los recursos públicos del frontend:

```powershell
$env:FILE_STORAGE_PATH = "C:\\dt-control-power\\uploads"
```

La carga acepta CSV, TXT, JSON y PDF de hasta 5 MiB. El backend genera el nombre físico, calcula SHA-256 en streaming y persiste únicamente metadatos junto con el proceso y sus checkpoints. La interfaz ofrece carga, listado paginado, detalle y timeline usando datos reales de la API. El rol `AUDITOR` permanece en modo de sólo lectura y los operadores sólo pueden asociar áreas asignadas.

Cada proceso crea además un registro de negocio sintético. Los roles de escritura pueden actualizar su único campo permitido; el backend aplica bloqueo optimista y guarda el cambio, la auditoría y los checkpoints en una misma transacción. `ADMIN`, `SUPERVISOR` y `AUDITOR` disponen de la vista paginada `/audit`.

El detalle permite recorrer Producción A, B y C mediante eventos append-only. Los semáforos se calculan a partir del último evento y los umbrales persistidos; no existe una columna mutable de color. Al completar las tres áreas el proceso pasa a `COMPLETED`, mientras que un fallo terminal lo lleva a `FAILED`. El dashboard muestra conteos, resumen por área y los diez eventos más recientes, con actualización cancelable cada cinco segundos.

## Identidad demo

La aplicación utiliza sesiones de servidor, cookie `HttpOnly`/`SameSite=Lax` y protección CSRF. Las cuentas sintéticas se provisionan al iniciar únicamente cuando están presentes estas variables:

```powershell
$env:DEMO_ADMIN_PASSWORD = "<valor-local-no-versionado>"
$env:DEMO_OPERATOR_PASSWORD = "<valor-local-no-versionado>"
$env:DEMO_AUDITOR_PASSWORD = "<valor-local-no-versionado>"
```

Los usuarios resultantes son `admin`, `operator-a` y `auditor`. Sus contraseñas se almacenan como BCrypt en PostgreSQL y nunca deben incluirse en Git. La UI obtiene un token CSRF antes de cada mutación y conserva la identidad sólo en memoria.

## Ejecución local

Abre una terminal para el backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Abre otra terminal para el frontend:

```powershell
cd frontend
npm run dev
```

URLs de prueba:

- Interfaz: <http://localhost:5173>
- API directa: <http://localhost:8080/api/v1/health>
- Catálogo persistente de áreas: <http://localhost:8080/api/v1/areas>
- Procesos autenticados: <http://localhost:8080/api/v1/processes>
- Actuator: <http://localhost:8080/actuator/health>

La aplicación consulta `fetch('/api/v1/health')`. Durante el desarrollo, Vite redirige las solicitudes que comienzan con `/api` a `http://127.0.0.1:8080`; por eso no se requieren una URL absoluta ni configuración CORS local.

## Verificación y compilación

Backend:

```powershell
cd backend
.\mvnw.cmd clean verify
```

Frontend:

```powershell
cd frontend
npm run lint
npm run build
```

El resultado del frontend se genera en `frontend/dist/` y el artefacto del backend en `backend/target/`. Ambos directorios están excluidos de Git.

## Observabilidad y despliegue

El backend emite logs JSON, correlaciona cada petición mediante `X-Request-ID` y registra eventos de negocio sin payloads ni secretos. No crea spans ni `traceId` manuales para evitar duplicar la instrumentación de OneAgent. La [guía de validación Dynatrace](docs/observability/DYNATRACE_VALIDATION.md) separa lo preparado en código de lo que todavía debe confirmarse en el entorno real.

El objetivo de despliegue sigue siendo PostgreSQL 17, NGINX y Docker Compose en Ubuntu Server, observado mediante Dynatrace OneAgent. Los artefactos de despliegue no se completan ni se aplican hasta inspeccionar la VM y contar con autorización específica.

Los despliegues son manuales: este repositorio no usa GitHub Actions, runners ni despliegues automáticos. Todo cambio debe revisarse e integrarse en GitHub antes de desplegarse manualmente en el servidor.
