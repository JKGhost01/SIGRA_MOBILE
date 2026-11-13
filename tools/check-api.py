#!/usr/bin/env python3
"""Read-only SIGRA checks. Credentials stay in local.properties and are never printed."""
import argparse
import json
from pathlib import Path
import re
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urljoin, urlparse
from urllib.request import Request, urlopen


def properties(path):
    result = {}
    for line in path.read_text().splitlines() if path.exists() else []:
        line = line.strip()
        if not line or line.startswith(("#", "!")) or "=" not in line:
            continue
        key, value = line.split("=", 1)
        result[key.strip()] = re.sub(
            r"\\u([0-9a-fA-F]{4})|\\(.)",
            lambda match: chr(int(match[1], 16)) if match[1] else {"n": "\n", "r": "\r", "t": "\t"}.get(match[2], match[2]),
            value.strip(),
        )
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", help="Override the server configured in local.properties")
    parser.add_argument("--timeout", type=float, default=8)
    args = parser.parse_args()
    config = properties(Path(__file__).resolve().parents[1] / "local.properties")
    base = (args.base_url or config.get("sigra.baseUrl", "https://sigra.azyrondev.com/")).rstrip("/") + "/"
    parsed = urlparse(base)
    if parsed.scheme not in ("https", "http") or not parsed.hostname or parsed.username or parsed.password or parsed.query or parsed.fragment:
        parser.error("Use an HTTP(S) base URL without credentials, query or fragment")
    key = config.get("sigra.eventsApiKey", config.get("sigra.apiKey", ""))
    print("Servidor:", base)
    checks = [("api/health", {}, {}), ("api/red/red", {}, {})]
    checks += [("api/sismos/recent", {"hours": 24, "limit": 1, "fuente": source},
                {"Authorization": "Bearer " + key})
               for source in ("SGC", "USGS", "EMSC", "GEONET", "GEOFON", "INGV")]
    failures = 0
    for route, query, headers in checks:
        label = route + (" · " + query["fuente"] if query else "")
        url = urljoin(base, route) + ("?" + urlencode(query) if query else "")
        request = Request(url, headers={"Accept": "application/json", **headers})
        try:
            with urlopen(request, timeout=args.timeout) as response:
                if "application/json" not in response.headers.get("Content-Type", "").lower():
                    raise ValueError("la respuesta no es JSON")
                body = json.loads(response.read(4 * 1024 * 1024))
                if not isinstance(body, dict) or body.get("status") != "ok":
                    raise ValueError("status no es ok")
                if response.headers.get("X-API-Version") != "v1":
                    raise ValueError("falta la cabecera de contrato X-API-Version: v1")
                if route == "api/health" and (body.get("service") != "sigra" or body.get("database") != "ok"):
                    raise ValueError("el servidor no confirma SIGRA con base de datos disponible")
                if route.endswith("recent"):
                    events = body.get("events")
                    if not isinstance(events, list) or any(event.get("fuente") != query["fuente"] for event in events):
                        raise ValueError("catálogo o filtro de fuente incompatible")
                elif route.endswith("red") and not isinstance(body.get("stats"), dict):
                    raise ValueError("faltan las estadísticas de la red")
                print("OK   ", label)
        except HTTPError as error:
            failures += 1
            reason = "revisa la clave de catálogo" if error.code in (401, 403) else "revisa el servidor y el despliegue"
            print("ERROR", label, "· HTTP", error.code, "·", reason)
        except (URLError, TimeoutError, OSError):
            print("ERROR", label, "· no se pudo conectar; revisa DNS, HTTPS y acceso a la red")
            return 1
        except (ValueError, AttributeError) as error:
            failures += 1
            print("ERROR", label, "·", str(error))
    print("Comprobaciones:", len(checks), "· fallos:", failures)
    return int(failures > 0)


if __name__ == "__main__":
    sys.exit(main())
