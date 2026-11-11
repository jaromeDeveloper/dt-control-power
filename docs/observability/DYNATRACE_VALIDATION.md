# Validación de observabilidad y Dynatrace

## Estado implementado

El backend emite logs JSON estructurados en consola mediante el formato `logstash` de Spring Boot. Cada petición recibe un `requestId` seguro: se conserva `X-Request-ID` cuando contiene de 1 a 36 caracteres alfanuméricos, punto, guion o guion bajo; de lo contrario se genera un UUID. El mismo valor se agrega al MDC, al header de respuesta y a los errores `ProblemDetail`.

La aplicación no crea `traceId`, spans ni métricas manuales. Esto evita duplicar la instrumentación automática de OneAgent. Los campos de traza sólo deben considerarse disponibles después de confirmar que OneAgent detecta la JVM, instrumenta HTTP/JDBC y enriquece los logs en el entorno real.

`/api/v1/health` continúa como comprobación pública mínima. `/actuator/health` requiere sesión autenticada y no expone detalles internos.

## Configuración externalizable

- `LOGGING_STRUCTURED_FORMAT_CONSOLE`: formato estructurado soportado por Spring Boot; valor por defecto `logstash`.
- La configuración de OneAgent, endpoint Dynatrace, credenciales y tokens pertenece al entorno. No se almacena en este repositorio.

## Validación controlada en el entorno

1. Invocar `/api/v1/health` con un `X-Request-ID` válido y comprobar que el mismo valor vuelve en el header.
2. Provocar un error de validación y comprobar `requestId` en el header y en el cuerpo `application/problem+json`.
3. Ejecutar una carga, una actualización de registro y un evento de área. Buscar los eventos `business.file_ingested`, `business.record_updated` y `business.area_event_recorded` por `requestId`.
4. Confirmar en Dynatrace que la JVM, las peticiones HTTP y PostgreSQL aparecen por instrumentación automática.
5. Confirmar que los logs ingeridos contienen `requestId` y, sólo si OneAgent los agrega, los identificadores de traza estándar.
6. Verificar que no se registran cookies, tokens, contraseñas, cuerpos de archivos, valores de negocio modificados ni detalles enviados por usuarios.

Hasta completar esos pasos, el código se considera preparado para observabilidad, pero la ingestión y correlación Dynatrace permanecen sin confirmar.
