# API de SIGRA Mobile

Backend: [SIGRA](https://github.com/JKGhost01/SIGRA). Contrato **v1** compatible
con SIGRA 1.3.1 y posteriores; las versiones de web y Android son independientes.

- [Catálogo de sismos](https://github.com/JKGhost01/SIGRA/blob/main/docs/API_SISMOS.md)
- [Red ciudadana](https://github.com/JKGhost01/SIGRA/blob/main/docs/API_RED.md)
- [OpenAPI](https://github.com/JKGhost01/SIGRA/blob/main/docs/api/openapi.yaml)

## Configuración

`local.properties`, excluido de Git, define `sigra.baseUrl`, `sigra.apiKey` y
`sigra.eventsApiKey`. Las propiedades Gradle `-P` tienen prioridad. Si falta la
clave de eventos, se reutiliza la de red. Recompila al cambiar estos valores.

La URL predeterminada es `https://sigra.azyrondev.com/`.

| Operación | Ruta | Autenticación |
| --- | --- | --- |
| Salud de aplicación y MySQL | GET `/api/health` | Pública |
| Estadísticas | GET `/api/red/red` | Pública |
| Catálogo | GET `/api/sismos/recent` | Bearer de APP_API_TOKEN |
| Alertas cercanas | GET `/api/red/alertas` | X-SIGRA-KEY; token opcional |
| Registro inicial | POST `/api/red/registro` | X-SIGRA-KEY |
| Renovación de registro | POST `/api/red/registro` | Clave y token vigente |
| Latido, detección y baja | POST `/api/red/estado`, `/deteccion`, `/desactivar` | Clave y token |

La clave de red corresponde a `red.appKey`, con el fallback definido por el
backend. Las credenciales de un despliegue no forman parte del código público.
Un token de dispositivo solo es válido en el servidor que lo emitió.

## Consultas

Se admiten SGC, USGS, EMSC, GEONET, GEOFON e INGV. Periodos de 24, 72, 120, 168
o 336 horas; magnitud mínima opcional; radio y orden por cercanía requieren
coordenadas propias. Los filtros se aplican en el servidor antes del límite de
1000 eventos. La búsqueda por texto se aplica a los resultados recibidos.

Fuente, periodo, magnitud, radio y orden se guardan en DataStore. Sin ubicación
se eliminan solo los requisitos de distancia. El catálogo se conserva en memoria;
el historial de avisos sí es persistente.

Las respuestas deben indicar `status: ok`. Se manejan errores legacy y errores
estructurados v1, autorización, DNS, TLS, saturación y fallos de disponibilidad.
`closer_towns` acepta texto, JSON estructurado o null. Los reportes ciudadanos
pendientes no se publican como eventos; `citizen_reports_count` no equivale a `felt`.

## Diagnóstico

En Android: **Ajustes → Conexión con SIGRA → Comprobar conexión**. Health confirma
servicio, contrato y MySQL; catálogo y estadísticas muestran estados separados.
Un health público correcto no demuestra que una clave autenticada sea válida.

Desde el proyecto:

```bash
python3 tools/check-api.py
```

Solo realiza GET de salud, red y una consulta por fuente. Lee las claves locales
sin imprimirlas. No registra dispositivos, envía detecciones ni inventa coordenadas.

Las pruebas Retrofit interceptan solicitudes sin sockets: rutas, parámetros,
credenciales separadas, rotación de token y errores. No sustituyen la comprobación
del despliegue, FCM, sensores o permisos en Android.
