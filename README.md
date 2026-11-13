# SIGRA Mobile

App Android nativa con Kotlin, Jetpack Compose y OpenStreetMap para explorar el
catálogo oficial de SIGRA y participar en su red experimental de teléfonos.
Código del backend y contrato API: [SIGRA](https://github.com/JKGhost01/SIGRA).

Versión Android: **v1.0.0.0** · Android 8.0 o posterior.

## Configuración

JDK 17, SDK 35, Gradle 8.9, Android Gradle Plugin 8.7.3 y Kotlin 2.0.21.
Android mínimo: 8.0/API 26. Abre esta carpeta directamente en Android Studio.

En una instalación nueva, copia `local.properties.example` como `local.properties`.
Si ya tienes el archivo, conserva sus claves y ajusta las propiedades necesarias:

```properties
sdk.dir=/home/TU_USUARIO/Android/Sdk
sigra.baseUrl=https://sigra.azyrondev.com/
sigra.apiKey=CLAVE_DE_LA_RED
# Si APP_API_TOKEN difiere de red.appKey:
# sigra.eventsApiKey=TOKEN_DEL_CATALOGO
```

La URL predeterminada también apunta a `https://sigra.azyrondev.com/`.
Las propiedades Gradle `-P` tienen prioridad. No se publican `local.properties`
ni `google-services.json`. Hay que recompilar al cambiar URL o claves.
Las claves del despliegue no se obtienen del repositorio público: deben coincidir
con `red.appKey` y `APP_API_TOKEN` del servidor. Configura las claves del despliegue en `local.properties`, excluido del repositorio.

Para desarrollo local puedes usar `http://10.0.2.2:8080/` en el emulador o la IP
LAN del equipo en un teléfono. Debug admite HTTP local; producción debe usar HTTPS.

## Compilar y ejecutar

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

En Android Studio, **Run ▶** compila e instala la app. **Build APK(s)** genera el
APK debug en `app/build/outputs/apk/debug/app-debug.apk`.

## Experiencia

- **Inicio:** resumen del catálogo seleccionado, evento de mayor magnitud recibido,
  accesos al mapa y listado, sensor, historial y cifras de la red. Las cifras solo
  se presentan como confirmadas después de una consulta exitosa.
- **Mapa:** página propia, marcadores con magnitud y colores por intervalo, controles
  con etiquetas fuera del mapa, encuadre, ubicación y paneles de filtros y estado.
  El mapa adapta sus colores al tema y conserva la cámara entre pestañas.
- **Sismos:** listado, búsqueda sin tildes, fuente, radio, orden, periodo y magnitud.
  Cabecera, búsqueda y filtros se desplazan con la lista para pantallas pequeñas.
- **Alertas:** historial persistente de hasta 50 avisos, leído/sin leer, avisos
  públicos separados y filtro de fuente. Las acciones masivas están en el menú
  de tres puntos; vaciar requiere confirmación.
- **Ajustes:** tema, monitoreo, notificaciones, permisos, radio y magnitud de alertas,
  diagnóstico de conexión, privacidad y licencias.

La línea visual toma como referencia los recursos del APK de SismoDetector
proporcionado por el usuario: superficies oscuras, acentos turquesa, títulos Chakra
Petch y lectura/datos IBM Plex. Las licencias SIL OFL 1.1 se incluyen en assets y se
pueden leer en Ajustes. Las instalaciones sin preferencia de tema guardada usan
oscuro; claro y automático siguen disponibles.

Hay navegación lateral en pantallas anchas y en horizontal desde 600 dp cuando
la altura es menor de 480 dp. Accesos y botones admiten texto ampliado; hay previews
Compose para 320 dp, fuente doble, claro, oscuro y ancho en `DesignPreviews.kt`.
Los previews están preparados, pendientes de renderizar y revisar en Android Studio.

## API y fuentes

Contrato contrastado con SIGRA **1.3.1 / API v1**. Se conservan las rutas públicas
y autenticadas documentadas en el backend, sin consultar ingestas externas directamente.

El catálogo incluye **SGC, USGS, EMSC, GeoNet, GEOFON e INGV**. Admite periodos de
24 horas a 14 días, magnitud mínima y filtros de distancia antes del límite de
1000 eventos. Fuente, radio, orden, periodo y magnitud se guardan en DataStore.
Sin ubicación propia se eliminan solo los requisitos de distancia.

[API_INTEGRATION.md](docs/API_INTEGRATION.md) explica rutas, claves, token de
rotación y los límites de la comprobación del despliegue real. Para comprobar
lecturas desde tu equipo, sin registrar teléfonos ni enviar detecciones:

```bash
python3 tools/check-api.py
```

En la app: **Ajustes → Conexión con SIGRA → Comprobar conexión**. Se comprueban
salud, contrato y disponibilidad de la base de datos; se presentan por separado
los resultados de catálogo y red. No se muestran las claves.

Las consultas autenticadas requieren las claves del servidor. Si aparece 401,
comprueba la configuración de catálogo y recompila antes de volver a instalar.

## Monitoreo y privacidad

Puedes explorar sin activar sensores ni crear una cuenta. El monitoreo comprueba
acelerómetro y permisos, usa un servicio en primer plano y envía posibles detecciones
con ubicación reciente. Las coordenadas de consulta proceden de este teléfono.

El registro se serializa y una respuesta antigua no restaura una identidad abandonada.
La renovación de un registro incluye su token vigente, como exige el backend actual.
Las consultas visibles no registran dispositivos. Abandonar conserva el historial
local y la deduplicación; la confirmación explica qué datos permanecen.

Los avisos pasan por una recepción común, con reserva persistente de UUID antes
de notificar, filtros de preferencias y tiempos que no se reinician al reabrir.
El backend y Firebase pueden mostrar push mixtos de forma automática; ver
[BACKEND_PUSH_CONTRACT.md](docs/BACKEND_PUSH_CONTRACT.md). Firebase es opcional;
sin su configuración se conservan las consultas por polling.

El catálogo se conserva solo en memoria. El historial de avisos es persistente.
SIGRA es experimental y no garantiza alertas anticipadas.

## Verificación

La compilación Kotlin independiente usa Kotlin 2.0.21, plugins Compose/serialización,
SDK 35 y dependencias locales. Los nuevos símbolos de fuentes se generaron para
esa comprobación; no sustituye el enlazado Android. AAPT2 compiló los recursos.
**Pasaron 70 pruebas JVM en 13 clases.** Cubren datos, filtros, almacenamiento, concurrencia, alertas,
solicitudes Retrofit y errores de API sin contactar producción.

El APK de distribución se compila con Kotlin y las herramientas oficiales Android,
se firma con el certificado del proyecto y se valida con AAPT2, apksigner y zipalign.
No equivale a una certificación de sensores, recepción FCM o interfaz en dispositivos.

[VALIDACION_ANDROID.md](docs/VALIDACION_ANDROID.md) recoge los casos de validación.
El workflow [.github/workflows/android.yml](.github/workflows/android.yml) prepara
pruebas, lint y APK sin credenciales; no realiza llamadas al servidor durante los tests.
La versión Android es v1.0.0.0, independiente de la versión del backend.


## Descarga y firma

[Descargas versionadas en GitHub Releases](https://github.com/JKGhost01/SIGRA_MOBILE/releases).
La primera versión es **v1.0.0.0**, código Android 2 y paquete `com.sigra.app`.
La instalación de desarrollo `com.sigra.app.debug` permanece separada.

Para generar futuras actualizaciones con Gradle, configura localmente:

```properties
sigra.signingStoreFile=RUTA_AL_CERTIFICADO.jks
sigra.signingStorePassword=CLAVE_DEL_ALMACEN
sigra.signingKeyAlias=ALIAS
sigra.signingKeyPassword=CLAVE_DE_LA_LLAVE
```

Ejecuta `./gradlew :app:assembleRelease`. Sin estas propiedades, el release de
Gradle queda sin firmar. Conserva el mismo certificado para permitir actualizaciones.
Los archivos de firma y sus contraseñas no se versionan.

