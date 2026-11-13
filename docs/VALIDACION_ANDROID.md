# Validación de Android

Versión: **v1.0.0.0**. Android mínimo: API 26; SDK objetivo: 35.

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Informes: `app/build/reports/`. El workflow Android checks ejecuta pruebas, lint
y empaquetado sin credenciales ni consultas al servidor.

## Casos en dispositivo

| Área | Comprobación |
| --- | --- |
| Instalación | APK firmado, versión 1.0.0.0 y paquete com.sigra.app; actualización con el mismo certificado |
| API | Health, catálogo autenticado, estadísticas y alertas con estados independientes; 401/422/429/503 y ausencia de red |
| Fuentes | Las seis agencias se pueden seleccionar; filtros de periodo, magnitud, radio y orden compartidos y persistentes |
| Ubicación | Solo posición propia; permisos denegados o aproximados y GPS apagado; consultas sin coordenadas no fallan |
| Mapa | Arrastre, zoom, encuadre, cámara entre pestañas y recreación; no tapa cabecera, Estado ni controles al moverse |
| Listado | Búsqueda sin tildes, teclado, desplazamiento y apertura/cierre de detalles; enlaces y compartir |
| Diseño | 320 dp, horizontal, tableta, fuente doble, claro/oscuro/automático y TalkBack |
| Monitoreo | Activación/detención, sensor ausente, servicio en primer plano y restricciones de batería |
| Identidad | Registro nuevo, renovación con token y respuesta tardía después de abandonar |
| Alertas | Push y polling simultáneos, leído/sin leer, reinicio, vaciado sin replay y tiempos originales |
| FCM | Mensajes tardíos, vencidos, inválidos y notification+data en segundo plano |
| Arranque | Android 14+ solicita reactivar monitoreo desde la app |

Hay 70 pruebas JVM en 13 clases, incluidas concurrencia y reapertura de DataStore,
solicitudes Retrofit y errores del contrato. Las pruebas de datos no certifican
la interfaz, la recepción Android ni capacidad de alerta temprana.

Los previews de `DesignPreviews.kt` permiten revisar Inicio y controles del mapa
con varios anchos y tamaños de texto.

## Límites

El catálogo solo se conserva en memoria y está limitado a 1000 eventos por
consulta. La reserva de UUID y la notificación Android no son una transacción
común: una muerte del proceso puede dejar un aviso guardado sin publicarlo.
El push mixto de Firebase tiene límites descritos en
[BACKEND_PUSH_CONTRACT.md](BACKEND_PUSH_CONTRACT.md).
