# Third-party notices

SIGRA is MIT-licensed (see [LICENSE](LICENSE)), but it relies on public data,
map services and open-source libraries that keep their own licenses and terms.
This file records them so deployments remain legally compliant.

> If you publish SIGRA, keep the map attribution visible and do not remove the
> source notices from the interface.

## Data sources

| Source | What SIGRA uses | Attribution / terms |
|---|---|---|
| **SGC — Servicio Geológico Colombiano** | Earthquake feed, event metadata, official event maps | Public information from the official agency. SIGRA shows the source and links to the original event. Subject to SGC terms and Colombian public-information rules. <https://www.sgc.gov.co/> |
| **USGS — U.S. Geological Survey** | FDSN/GeoJSON earthquake feed | U.S. Government work, generally public domain. Attribution shown as a courtesy. <https://earthquake.usgs.gov/> |
| **OpenStreetMap** | Base map data via raster tiles | © OpenStreetMap contributors, **ODbL 1.0**. <https://www.openstreetmap.org/copyright> · <https://opendatacommons.org/licenses/odbl/1-0/> |
| **OpenStreetMap Foundation tile servers** | `tile.openstreetmap.org` | Subject to the **OSMF Tile Usage Policy** (attribution, no heavy scraping, valid User-Agent). <https://operations.osmfoundation.org/policies/tiles/> |
| **OpenTopoMap** | Topographic tile layer | Map data © OpenStreetMap contributors, SRTM. Tiles under **CC-BY-SA**. <https://opentopomap.org/about> |
| **Esri / ArcGIS Online** | World Imagery and World Dark Gray Canvas | Subject to Esri terms and attribution requirements. <https://www.esri.com/en-us/legal/terms/full-master-agreement> |
| **ipapi.co** | Optional IP-based approximate location fallback | Subject to ipapi.co terms and privacy policy. <https://ipapi.co/terms/> |

SIGRA stores a normalized copy of the event data for its history. It does not
claim ownership of the original data. Always link back to the official source.

## Web software

| Component | License | Notes |
|---|---|---|
| **CodeIgniter 4** | MIT | © British Columbia Institute of Technology and the CodeIgniter Foundation. |
| **Leaflet** | BSD 2-Clause | © Volodymyr Agafonkin and Leaflet contributors. |
| **Bootstrap** | MIT | © The Bootstrap Authors. |
| **DM Sans** | SIL Open Font License 1.1 | Served through Google Fonts; the service is subject to Google APIs Terms. |
| **Space Grotesk** | SIL Open Font License 1.1 | Served through Google Fonts; the service is subject to Google APIs Terms. |
| **jsDelivr CDN** | CDN service | Serves Bootstrap assets. Subject to jsDelivr terms. <https://www.jsdelivr.com/terms> |
| **unpkg CDN** | CDN service | Serves Leaflet assets. Subject to unpkg terms. <https://unpkg.com/> |
| **Laminas Escaper** | BSD 3-Clause | Composer dependency. |
| **PSR Log** | MIT | Composer dependency. |
| **PHP** | PHP License 3.01 | Runtime. |
| **MySQL / MariaDB** | GPLv2 | External database servers; not distributed with this repository. |

## Android app

| Component | License | Notes |
|---|---|---|
| **AndroidX / Jetpack Compose** | Apache License 2.0 | UI and lifecycle. |
| **Kotlin / kotlinx.coroutines / kotlinx.serialization** | Apache License 2.0 | Language and runtime libraries. |
| **OkHttp / Retrofit** | Apache License 2.0 | HTTP client and API bindings. |
| **osmdroid** | Apache License 2.0 | OpenStreetMap map component. Tiles remain subject to the OSM tile policy. |
| **Gradle / Android Gradle Plugin** | Apache License 2.0 | Build tooling. |
| **Google Play services Location** | Google APIs Terms of Service | Location services. |
| **Firebase Cloud Messaging** | Google APIs Terms of Service | Optional push. Only active when `google-services.json` is present. |

## Optional integrations

- **Firebase / FCM** is optional. Without it, the Android app uses polling.
- **ipapi.co** is only called when the browser denies precise geolocation.
- No advertising, analytics or tracking SDKs are included.

## License texts

When redistributing SIGRA, keep the license texts and notices of the components
you ship. The main texts are available at:

- MIT: <https://opensource.org/license/mit>
- BSD 2-Clause: <https://opensource.org/license/bsd-2-clause>
- BSD 3-Clause: <https://opensource.org/license/bsd-3-clause>
- Apache 2.0: <https://www.apache.org/licenses/LICENSE-2.0>
- ODbL 1.0: <https://opendatacommons.org/licenses/odbl/1-0/>
- SIL OFL 1.1: <https://openfontlicense.org/>
- CC-BY-SA: <https://creativecommons.org/licenses/by-sa/4.0/>
- PHP License 3.01: <https://www.php.net/license/3_01.txt>
- GPLv2: <https://www.gnu.org/licenses/old-licenses/gpl-2.0.html>


## Tipografías de la interfaz

Chakra Petch (Chakra Petch Project Authors, 2018) e IBM Plex Sans/Mono
(IBM Corp., 2017) se distribuyen bajo SIL Open Font License 1.1.
Se incorporaron los archivos de fuente sin modificar disponibles en el APK de
referencia proporcionado por el usuario. Solo se reutilizan estas tipografías
libremente redistribuibles; los componentes de interfaz se implementaron en el proyecto.

Licencias completas incluidas en `app/src/main/assets/licenses/`, accesibles desde
Ajustes → Acerca de SIGRA → Licencias de tipografías.
Referencias de licencia: https://github.com/google/fonts/tree/main/ofl/chakrapetch,
https://github.com/google/fonts/tree/main/ofl/ibmplexsans y
https://github.com/google/fonts/tree/main/ofl/ibmplexmono.
