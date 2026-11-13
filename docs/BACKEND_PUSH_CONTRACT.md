# Integración de push: ajuste pendiente en el backend

Esta sesión modifica el cliente Android. No se cambiaron ni desplegaron archivos
del backend. Este documento permite revisar el ajuste de envío por separado.

## Situación observada

`sigra/app/Libraries/FcmService.php` construye mensajes con `notification`, `data`
y `android.notification`. Para el SDK legado usado por esta app, Firebase muestra
automáticamente los mensajes mixtos cuando la app está en segundo plano; sus datos
llegan al intent de la actividad cuando el usuario los toca.
[Comportamiento documentado por Firebase](https://firebase.google.com/docs/cloud-messaging/android/receive-messages).

La app ya procesa esos extras al abrirlos, guarda el aviso y lo marca leído sin
emitir una nueva notificación. Sin embargo, la publicación automática previa no
pasa por `AlertReceiver`: puede duplicar un aviso detectado por polling, ignorar
preferencias locales recién cambiadas o mostrar un aviso que llegó tarde.

## Cambio propuesto para el envío legado

Para que las alertas de este cliente se publiquen solo a través de su receptor,
el envío de alertas sísmicas necesita mensajes de datos, eliminando `notification`
y `android.notification` de ese tipo de envío. No aplicar esa eliminación a otros
consumidores sin comprobar qué contrato utilizan. Mantener la prioridad y definir
un TTL corto, coherente con la vigencia real de la alerta.

Ejemplo de estructura HTTP v1 para revisar, sin credenciales reales:

```json
{
  "message": {
    "token": "TOKEN_DEL_DISPOSITIVO",
    "android": {
      "priority": "high",
      "ttl": "120s"
    },
    "data": {
      "type": "seismic_alert",
      "alert_uuid": "UUID_ESTABLE_DEL_EVENTO",
      "source": "crowd",
      "magnitude": "4.1",
      "latitude": "4.6",
      "longitude": "-74.1",
      "distance_km": "42.0",
      "seconds_to_s_wave": "12.5",
      "severity": "amarilla",
      "confidence": "0.82",
      "device_count": "4",
      "origin_time": "2026-11-12T15:00:00Z",
      "origin_time_ms": "1794495600000",
      "expires_at": "2026-11-12T15:02:00Z"
    }
  }
}
```

Las fechas del ejemplo son ilustrativas; generar origen, vencimiento y ETA
coherentes con el evento real, calculando la ETA lo más cerca posible del envío.
La app usa `RemoteMessage.sentTime` como referencia del tiempo relativo, conserva
la UUID del servidor y no completa fuentes o porcentajes que el mensaje omita.
Todos los valores de `data` se envían como texto.

El límite adicional de frescura del cliente es cinco minutos desde el envío;
un `expires_at` ISO con offset puede acortarlo. El TTL de transporte no sustituye
al vencimiento del evento. Un mensaje antiguo puede quedar en el historial,
pero el cliente no lo publica como una alerta vigente.

## Verificación conjunta pendiente

1. Configurar Firebase en un teléfono de pruebas y usar un evento de simulación
   claramente identificado como `source=simulation`.
2. Probar entrega en primer y segundo plano, con monitoreo activo y detenido.
3. Entregar la misma UUID por push y polling a la vez: comprobar una sola publicación
   de la app, una entrada de historial y ausencia de sonido repetido.
4. Repetir después de leer, vaciar el historial, recrear la actividad y reiniciar el proceso.
5. Retrasar un mensaje y modificar preferencias antes de su llegada: verificar radio,
   magnitud, vigencia y el texto de la estimación.
6. Comprobar batería, restricciones del fabricante y políticas de entrega de FCM;
   la prioridad alta y un mensaje de datos no garantizan entrega inmediata.

Referencias: [tipos de mensaje](https://firebase.google.com/docs/cloud-messaging/customize-messages/set-message-type),
[duración de mensajes](https://firebase.google.com/docs/cloud-messaging/customize-messages/setting-message-lifespan)
y [tiempo de envío de RemoteMessage](https://firebase.google.com/docs/reference/android/com/google/firebase/messaging/RemoteMessage).
