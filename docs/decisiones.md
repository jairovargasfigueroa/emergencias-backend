# Decisiones de evidencias, IA y atención en el lugar

Este documento registra lo que se decidió en el módulo de evidencias e IA y por qué. Las decisiones internas del análisis (prompt, campos del resumen, reglas sobre la respuesta del modelo) están en [`emergencias-mia/docs/decisiones.md`](../../emergencias-mia/docs/decisiones.md).

Cada decisión se revisa con tres preguntas:

- ¿Así se trabaja en una emergencia real?
- ¿Se puede defender?
- ¿Es fácil de cambiar después?

**Estados:**

- **Decidida:** se aplica o se va a aplicar.
- **Propuesta:** está diseñada, pero falta confirmarla con el equipo.
- **Abierta:** todavía está en discusión.
- **Pospuesta:** queda fuera por ahora; el diseño se conserva para retomarlo.

| # | Decisión | Estado | Fecha |
|---|---|---|---|
| B1 | Cada evidencia se analiza una vez y el resumen se rehace con todo | Decidida | 2026-10-02 |
| B2 | Por ahora solo foto y audio | Decidida | 2026-10-04 |
| B3 | La galería solo existe en pruebas | Decidida | 2026-10-04 |
| B4 | Límites de evidencias | Decidida (audio en revisión) | 2026-10-02 |
| B5 | Rechazar evidencias que no sirven y liberar su lugar | Propuesta | 2026-10-04 |
| B6 | Avisar al ciudadano qué se hace con lo que envía | Decidida | 2026-10-04 |
| B7 | Cerrar el envío de evidencias cuando llega la primera unidad | Pospuesta | 2026-10-04 |
| B8 | Varios heridos: la tripulación pide apoyo | Pospuesta | 2026-10-04 |
| B9 | Avisar a la tripulación solo si cambia algo importante | Decidida | 2026-10-04 |
| B10 | Leer el resumen en voz, como una operadora | Decidida (por ahora, voz del teléfono) | 2026-10-04 |
| B11 | Mostrar la gravedad estimada, en rojo si es alta | Decidida | 2026-10-04 |
| B12 | Menos información sobre la IA en la app del paramédico | Decidida | 2026-10-04 |
| B13 | Fotos en la app del paramédico | Abierta | — |
| B14 | El panel muestra el resumen completo y las fotos | Decidida | 2026-10-04 |
| B15 | Retención y consentimiento | Abierta (entrenamiento decidido) | 2026-10-03 |

## B1. Cada evidencia se analiza una vez y el resumen se rehace con todo

**Decidida, 2026-10-02.**

- **Decisión.**
  - Cuando se confirma la subida, la evidencia se analiza una sola vez y el resultado se guarda en `analisis_evidencia`.
  - Después se pide un resumen nuevo con todas las alertas del incidente y todos los análisis guardados.
  - mia no vuelve a leer los archivos ni recibe el resumen anterior.
- **Por qué.**
  - Lo caro es analizar el archivo. El resumen es una llamada solo con texto.
  - Si el resumen depende solo de sus fuentes, el orden de llegada no lo cambia y el error de una versión no pasa a la siguiente.
- **Descartado.** Sumar lo nuevo al resumen anterior, porque acumula errores y depende del orden.
- **Riesgo conocido.** Al rehacer el resumen, el modelo puede omitir un peligro mencionado antes. Se resuelve en mia (M3).

## B2. Por ahora solo foto y audio

**Decidida, 2026-10-04.**

- **Decisión.**
  - El ciudadano envía fotos y audios.
  - El video sale de esta entrega: la app no lo ofrece y el backend no acepta formatos de video. Esto se controla con una propiedad de configuración nueva.
  - El análisis de video de mia se conserva sin uso.
- **Por qué.**
  - El audio es lo que más va a usar la gente, porque reemplaza a la llamada. La foto muestra la escena.
  - El video pesa más, su análisis puede tardar unos 265 s y cuesta más. Además, no hay tiempo de probarlo bien.
- **Cómo se cambia.** Se reactiva la propiedad y el botón.

## B3. La galería solo existe en pruebas

**Decidida, 2026-10-04.**

- **Decisión.** "Elegir de la galería" aparece solo en las compilaciones de desarrollo. En producción, la evidencia se captura en el momento con la cámara o el micrófono.
- **Por qué.**
  - Una foto de la galería puede ser vieja o de otro lugar, y facilita fingir una emergencia.
  - Capturar en el momento es la forma más simple de que la evidencia corresponda a lo que está pasando.
- **Dónde.** En la app del ciudadano: `src/features/evidencias/captura.ts` y `src/features/seguimiento/PasoEvidencias.tsx`.

## B4. Límites de evidencias

**Decidida, 2026-10-02. El límite de audio está en revisión.**

| Límite | Valor |
|---|---|
| Por alerta | 5 |
| Por incidente | 15 |
| Foto | 10 MiB. La app la reduce a 1600 px y le quita los metadatos EXIF. |
| Audio | 2 min y 5 MiB |

- Las evidencias descartadas no cuentan, y tampoco las rechazadas (B5).
- **En revisión.** En la reunión del 3 de octubre, los 2 min de audio parecieron largos. Antes de cambiarlo hay que probar 60 s grabando casos reales.

## B5. Rechazar evidencias que no sirven y liberar su lugar

**Propuesta, 2026-10-04.**

- **Problema.**
  - Hoy, una foto negra o sin nada de la emergencia, o un audio en silencio, se guarda como analizada.
  - Esa evidencia ocupa uno de los 5 lugares de la alerta y entra al resumen. Si es la única, se convierte en el resumen.
- **Flujo propuesto.**
  1. mia descarta primero lo que se detecta sin el modelo: el audio en silencio (ya existe) y, si se agrega, la imagen casi negra o de un solo color.
  2. Si la evidencia pasa ese filtro, la misma llamada al modelo que la analiza dice si sirve (`usable`) y, si no sirve, por qué (`unusableReason`, de una lista cerrada). No se agrega otra llamada.
  3. Si no sirve, el backend:
     - marca la evidencia como `RECHAZADA`, con el motivo;
     - borra el archivo de S3 después del commit;
     - no guarda el análisis completo;
     - no pide un resumen nuevo.
  4. `RECHAZADA` no cuenta para los límites, así que el ciudadano recupera el lugar.
  5. El ciudadano recibe un push en el canal `avisos`, y la fila de esa evidencia cambia a "No se pudo usar: está muy oscura. Puedes enviar otra."
- **Reglas.**
  - Ante la duda, se acepta: perder una evidencia útil es peor que guardar una que no aporta.
  - De una evidencia rechazada solo se guardan el motivo y la fecha, para medir cuántas se rechazan.
  - Después de 3 rechazos en la misma alerta, no se aceptan más evidencias de esa alerta. Así se evita el abuso.
- **Por qué un estado nuevo y no `DESCARTADA`.** `DESCARTADA` significa que la evidencia se abandonó o venció. Tener dos estados permite darle al ciudadano un mensaje preciso y medir los rechazos.
- **Lo que falta.** No hay un canal por evidencia hacia el ciudadano: hoy la app se queda en "Enviada". Hacen falta el push nuevo y su manejo en la app.
- **Video.** Si se reactiva (B2), sigue este mismo flujo.
- **Respuestas del 2026-10-04.**
  - Canal hacia el ciudadano (push y endpoint nuevos): sí.
  - De una evidencia rechazada se guardan solo el motivo y la fecha.
  - `RECHAZADA` como estado nuevo o `DESCARTADA` con motivo: pospuesto, porque es lógica del backend y no se toca por ahora. Mientras no se decida, la parte del backend no se implementa.

## B6. Avisar al ciudadano qué se hace con lo que envía

**Decidida, 2026-10-04.**

- **Decisión.** Antes de capturar, la app muestra una nota explícita. Texto base:

  > Lo que envíes lo verán el equipo de la ambulancia y la central para prepararse antes de llegar. Un sistema automático lo revisa para resumir lo que pasa. Se guarda N días y después se borra. No se publica ni se usa para otra cosa.

- **Por qué.** Las fotos y los audios pueden mostrar a otras personas heridas. Quien los envía tiene que saber quién los ve, para qué y por cuánto tiempo.
- **Condición.** El texto tiene que decir la verdad:
  - el plazo depende de B15;
  - mencionar o no el servicio externo depende de la decisión M9 de mia.

## B7. Cerrar el envío de evidencias cuando llega la primera unidad

**Pospuesta, 2026-10-04.** Queda fuera de esta entrega; el diseño se conserva.

- **Problema.** Hoy se aceptan evidencias hasta que el incidente se cierra, incluso con la tripulación atendiendo o ya en el hospital.
- **Propuesta.**
  - Cuando la primera unidad marca la llegada, la app del ciudadano cierra la tarjeta de evidencias y `Evidencia.registrar` rechaza las nuevas.
  - Las que ya se estaban subiendo pueden terminar.
- **Por qué.**
  - Desde la llegada, la fuente confiable es la tripulación en el lugar, y el análisis previo pierde valor.
  - Coincide con lo planteado en las reuniones del 27 de septiembre y del 3 de octubre.
- **Precedente.** Completar detalles y retirar la alerta ya funcionan así (`existeLlegadaVigentePorIncidente`).

## B8. Varios heridos: la tripulación pide apoyo

**Pospuesta, 2026-10-04.** Queda fuera de esta entrega; el diseño se conserva.

- **Cómo funciona hoy.**
  - Un incidente puede tener varias atenciones.
  - Llegar no cierra ni cancela nada. El incidente sigue abierto: otra unidad puede sumarse ("Otra unidad ya acude · Sumarme") y la central puede enviar una.
  - Una atención está activa mientras su unidad va en camino, está en el lugar, lleva al paciente o está en el hospital. Pasa a resuelta cuando entrega al paciente o termina sin traslado.
  - Nadie cierra el incidente a mano. Cada vez que una atención se resuelve o se cancela, el backend revisa el incidente (`AtencionService.evaluarIncidente`):
    - si queda otra atención activa, el incidente sigue abierto;
    - si no queda ninguna y alguien entregó a un paciente, el incidente pasa a `ATENDIDO`;
    - si todas terminaron sin traslado, el estado depende del motivo (`FALSA_ALARMA`, `ATENDIDO_EXTERNAMENTE` o `ATENDIDO`);
    - si todas se cancelaron, el incidente vuelve a `ACTIVO` para buscar otra unidad.
  - Liberar la unidad no cuenta: el incidente se cierra con la entrega, no con la liberación.
  - Mientras la primera unidad atiende, el incidente sigue visible para las demás y cualquiera puede sumarse, aunque no haya más heridos. Nadie avisa a las otras unidades: solo verían el incidente en la lista o el mapa con "1 unidad en camino".
- **El hueco.**
  - Si va una sola unidad y hay tres heridos, al entregar a su paciente el incidente se cierra como `ATENDIDO` y los otros dos quedan fuera.
  - Nadie puede avisar que faltan unidades: `cantidadAfectados` es solo informativo, y "sin cubrir" solo detecta incidentes sin ninguna unidad.
- **Propuesta.** La tripulación en el lugar marca cómo está la escena. Hay dos opciones:
  - **"Situación controlada":**
    - el incidente no se cierra, pero queda marcado;
    - deja de aparecer como incidente al que se puede sumar otra unidad;
    - las unidades que ya van en camino reciben un aviso, "Situación controlada por la unidad X", con un botón "Volver". Ese botón cancela su atención con un motivo nuevo, `ESCENA_CONTROLADA`. No se cancela sola, para que la otra tripulación o la central puedan decidir seguir.
  - **"Se necesitan más unidades" (con cantidad):**
    - el incidente queda con apoyo pendiente;
    - aparece en "sin cubrir" en el panel y se avisa a las unidades cercanas;
    - la unidad puede irse con su paciente;
    - mientras falten unidades, el incidente no se cierra solo: si termina la última atención, vuelve a `ACTIVO`.
- **Reglas.**
  - Se puede marcar desde la llegada. Si la tripulación no marcó nada, se le pregunta de forma obligatoria antes de "paciente a bordo". Así nadie se va sin decir cómo quedó la escena.
  - Se puede cambiar mientras la unidad sigue en el lugar, por ejemplo si aparece otro herido. Cuenta la última marca de cualquier tripulación.
  - Cada unidad que llega después también puede marcar.
  - Si hay apoyo pendiente y ninguna unidad queda en el lugar, se vuelve a aceptar evidencias de los ciudadanos, porque sirven a las unidades que vienen. Es opcional.
  - No hace falta un botón de "terminar la emergencia". La emergencia termina sola cuando la última unidad entrega a su paciente o cierra sin traslado y no queda apoyo pendiente.
- **Por qué.**
  - En un servicio real, la primera unidad evalúa la escena y pide los recursos que faltan. No da por terminada una emergencia en la que otros siguen trabajando.
  - Solo la tripulación en el lugar sabe cuántos heridos hay. Lo que reportan los ciudadanos es una estimación: `cantidadAfectados` guarda el máximo reportado.
- **Costo.**
  - Hay que agregar:
    - un campo en `Incidente` con el estado de la escena (sin evaluar, controlada, faltan N);
    - un endpoint;
    - el cambio en `evaluarIncidente`;
    - el aviso a las unidades en camino;
    - el motivo de cancelación `ESCENA_CONTROLADA`;
    - el botón en la app y la marca en el panel.
  - Se reutilizan "sumarse", el despacho y "sin cubrir".

## B9. Avisar a la tripulación solo si cambia algo importante

**Decidida, 2026-10-04.**

- **Problema.** Hoy cada versión del resumen manda un push con sonido a todas las unidades que ocupan el incidente, incluso en el hospital y hasta que se liberan.
- **Decisión.**
  - El backend compara la versión nueva con la anterior en estos puntos:
    - gravedad;
    - tipo de suceso;
    - rango de personas;
    - peligros, incluido un peligro que pasa a "sin novedades" (M3 de mia).
  - Si nada de eso cambia, no hay push. Firebase igual actualiza la pantalla.
  - Un cambio solo de redacción no genera aviso.
  - Solo reciben el aviso las unidades en camino (`EN_CAMINO`). En el lugar, la tripulación ya ve la escena, y en el hospital el aviso ya no aporta.
- **Por qué.**
  - Un aviso que suena por todo deja de escucharse.
  - Lo que cambia la preparación de la tripulación es esto: más personas, un peligro nuevo o más gravedad.
- **Dónde.** `AvisoDeResumenALaTripulacion`, `ResumenIncidenteService.guardar` y `AtencionRepository`.

## B10. Leer el resumen en voz, como una operadora

**Decidida, 2026-10-04. Por ahora se usa la voz del teléfono.**

- **Decisión.**
  - La app del paramédico lee en voz alta el resumen breve:
    - al tomar el incidente;
    - con cada cambio importante (B9), empezando con "Actualización".
  - Deja de leer cuando la unidad llega.
  - Tiene un botón grande "Repetir".
- **Por qué.** En un servicio real, la central le pasa el caso a la tripulación por radio en pocas frases, porque en camino nadie lee una pantalla. La pantalla queda para cuando se detienen.
- **Texto.** Lo arma la app con los puntos clave (M4 de mia) y la gravedad, sin otra llamada a la IA. Ejemplo:

  > Atención, unidad 12. Choque de dos motos. Dos heridos; uno no se mueve. Precaución: sale humo de un auto. Gravedad estimada alta.

- **Motor de voz.** Por ahora, la voz del teléfono (`expo-speech`). Las otras opciones quedan registradas para más adelante.

  | Opción | Costo y licencia | Ventaja | Desventaja |
  |---|---|---|---|
  | Voz del teléfono (`expo-speech`) | Gratis | Funciona sin internet, el texto no sale del teléfono y requiere poco trabajo | La voz depende del teléfono |
  | Kokoro-82M, voz `ef_dora` | Gratis, Apache-2.0 | Voz femenina más natural | Se genera en el servidor: hace falta un endpoint nuevo y guardar el audio en S3 |
  | Piper `es_MX-claude-high` | Gratis, Apache-2.0 | Liviana y rápida en CPU | La misma que Kokoro |
  | Piper `es_AR-daniela-high` | Gratis, CC BY-SA 4.0 (pide atribución) | Femenina, acento rioplatense | La misma que Kokoro |

  - Se pasa a Kokoro solo si la voz del teléfono suena mal en los equipos de prueba.
  - Ninguna voz libre suena "a operadora" por sí sola. Ese efecto lo dan el guion (frases cortas, siempre en el mismo orden) y el ritmo.
  - Con la app en segundo plano, el push muestra el texto y la lectura empieza al abrirla.

## B11. Mostrar la gravedad estimada, en rojo si es alta

**Decidida, 2026-10-04.**

- **Decisión.** La app del paramédico muestra "Gravedad estimada" con su motivo y un color por nivel:

  | Nivel | Color |
  |---|---|
  | Alta | Rojo |
  | Moderada | Ámbar |
  | Baja | Verde |
  | Sin determinar | Gris |

- **Por qué.**
  - En un servicio real, la tripulación recibe una prioridad junto con el caso.
  - La documentación aclara que es una estimación y no un triaje.
  - mia exige un motivo: sin motivo, la gravedad pasa a "sin determinar".
  - Lo que recargaba la pantalla era la información sobre la IA, no la gravedad.

## B12. Menos información sobre la IA en la app del paramédico

**Decidida, 2026-10-04.**

- **Decisión.** A simple vista quedan los puntos clave y la gravedad. El resto va dentro de "Ver todo":
  - "Corroborado por N alertas" pasa a decir "Lo dicen 2 personas", y solo aparece en el detalle.
  - "Deducido, no visto", "Contradicciones" y "Lo que no se puede saber" van en el detalle, y solo cuando tienen contenido.
  - No se indica qué foto o audio respalda cada dato, porque el paramédico no los ve a simple vista.
- **Por qué.**
  - El paramédico necesita los datos de la emergencia, no saber cómo los obtuvo la IA.
  - El detalle de las fuentes sirve para revisar, y eso se hace en el panel (B14).

## B13. Fotos en la app del paramédico

**Abierta.**

| Opción | A favor | En contra |
|---|---|---|
| Visibles, como hoy | Pueden mostrar algo que el resumen no dice | Pueden ser borrosas, inútiles o muy fuertes; gastan datos móviles y distraen en camino |
| Ocultas, con un botón "Ver fotos (N)" | Están disponibles sin recargar la pantalla | Exigen un toque más |
| No se muestran | La pantalla queda más limpia | El paramédico no puede comprobar lo que dice el resumen |

- Si se aplica B5 (rechazo de evidencias), el problema de las fotos inútiles se reduce.

## B14. El panel muestra el resumen completo y las fotos

**Decidida, 2026-10-04.**

- **Decisión.** El detalle del incidente en el panel muestra:
  - el resumen completo;
  - de qué fuente sale cada dato;
  - las versiones;
  - las transcripciones;
  - las fotos.
- **Por qué.** El panel es para mirar y revisar. Ahí se puede comparar lo que dijo la IA con lo que enviaron los ciudadanos.

## B15. Retención y consentimiento

**Abierta. La parte de entrenamiento está decidida desde el 2026-10-03.**

- **Decidido.**
  - Las fotos y los audios no se usan para entrenar modelos sin un consentimiento aparte, explícito y opcional en los términos y condiciones.
  - Hoy solo existe la aceptación del aviso de privacidad al registrarse (`Usuario.privacidadAceptadaEn`).
- **Abierto.**
  - Los archivos se borran de S3 a los 90 días mediante una regla de ciclo de vida que se configuró a mano en AWS y no está en este repo. Hay que verificar que exista.
  - Los análisis guardados (`analisis_evidencia`, con transcripciones) y los resúmenes no se borran nunca. Falta decidir un plazo.

## Hallazgos pendientes (no son decisiones)

- `ResumenIncidenteService.prepararPedido` incluye las alertas canceladas o descartadas, así que sus descripciones entran al resumen. Hay que filtrarlas.
- En la app del paramédico, `DialogoSinTraslado` dice "Con esto se cierra el incidente". No es cierto si otras unidades siguen atendiendo.
- El ciudadano no recibe un push cuando el incidente se cierra solo (`ATENDIDO`, `FALSA_ALARMA`).
- Al confirmar una evidencia, no se vuelve a comprobar que el incidente siga abierto.
