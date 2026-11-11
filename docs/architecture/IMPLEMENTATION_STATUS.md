# Estado de implementación

Esta matriz contrasta el plano maestro con el repositorio. Se actualiza al cerrar cada incremento y no sustituye las pruebas ni el historial de Git.

| Incremento | Estado | Evidencia actual | Pendiente principal |
|---|---|---|---|
| I0 Base | EXISTE | `/api/v1/health`, pantalla React, proxy Vite y pruebas HTTP | Validación manual a través de NGINX en el entorno Ubuntu |
| I1 Persistencia | PARCIAL | Flyway, JPA, migración de áreas y `GET /api/v1/areas` | Conectar y validar contra el PostgreSQL 17 existente en Ubuntu |
| I2 Identidad demo | PARCIAL | Sesión HttpOnly/SameSite, CSRF, roles, provisión BCrypt por entorno, login y rutas privadas React | Verificación por compilador y smoke login contra PostgreSQL real omitidos por indicación del usuario |
| I3 Archivo/proceso | PARCIAL | Carga multipart, almacenamiento privado, SHA-256 en streaming, metadatos, proceso, checkpoints, lista y timeline real en React | Verificación por compilador y smoke contra PostgreSQL/volumen real omitidos por indicación del usuario; falta política automatizada de retención |
| I4 Cambio negocio | PARCIAL | Registro real por proceso, edición autorizada con optimistic locking, auditoría transaccional, checkpoints de éxito/fallo, panel React y auditoría paginada | Verificación por compilador y concurrencia real contra PostgreSQL omitidas por indicación del usuario |
| I5 Áreas y semáforos | PARCIAL | Eventos append-only, idempotencia, secuencia A/B/C, permisos por área, semáforos derivados y operación React | Verificación por compilador, reloj/umbrales y concurrencia real omitida por indicación del usuario |
| I6 Dashboard | PARCIAL | Conteos reales, resumen de semáforos, diez eventos recientes y polling React cancelable cada 5 s | Verificación por compilador y contraste manual de agregados contra PostgreSQL omitidos por indicación del usuario |
| I7 Observabilidad | PARCIAL | `requestId` en header/MDC/errores/auditoría, logs JSON y eventos de negocio seguros; guía Dynatrace | Confirmar detección JVM, HTTP/JDBC, ingestión de logs y enriquecimiento de trazas con OneAgent real |
| I8 Despliegue | FALTA | — | NGINX, Compose y scripts manuales tras inspeccionar la VM |

## Decisiones vigentes

- El backend continúa como un único monolito modular Spring Boot.
- Flyway en `backend/src/main/resources/db/migration` es la única fuente de migraciones.
- PostgreSQL es la fuente transaccional; Dynatrace nunca sustituye datos funcionales.
- Las credenciales se proporcionan por variables de entorno y no se versionan.
- La autenticación usa sesión de servidor y CSRF; el navegador no persiste passwords ni tokens en `localStorage`.
- Los archivos se escriben fuera de la raíz web con nombre generado por servidor; PostgreSQL conserva sólo metadatos y la ruta interna nunca forma parte del contrato REST.
- La carga admite CSV, TXT, JSON y PDF hasta 5 MiB; calcula SHA-256 en streaming y crea un proceso con checkpoints iniciales en la misma operación funcional.
- Cada proceso recibe un registro de negocio demostrativo; su actualización, auditoría exitosa y checkpoints comparten transacción. Los fallos se registran en una transacción nueva best-effort porque una caída de PostgreSQL impide persistir incluso el diagnóstico.
- Los eventos de área son append-only e idempotentes por proceso; los semáforos se derivan del último evento y de los umbrales configurados, sin guardar el color como fuente de verdad.
- La finalización de A, B y C completa automáticamente el proceso; un evento terminal fallido lo marca `FAILED`. El dashboard consulta esos estados y eventos reales con polling de 5 segundos.
- Cada petición usa un `requestId` validado o generado, propagado en respuesta, MDC y errores. No se fabrica `traceId`: la correlación de traza queda condicionada a OneAgent y a la configuración real del entorno.
- Las pruebas usan H2 en modo de compatibilidad PostgreSQL únicamente para verificar el contexto, la migración y JPA. La compatibilidad real con PostgreSQL sigue requiriendo smoke en el entorno autorizado.
- No se crean pipelines, runners, despliegues ni cambios sobre la VM automáticamente.
