# Demo de CATastrophe — guía rápida

Tres scripts para levantar, sembrar y parar la aplicación completa en local.

## Arrancar

```bash
./scripts/start-demo.sh --all      # build + datos demo + túnel público
```

O por partes:

```bash
./scripts/start-demo.sh            # solo levanta infra + servicios
./scripts/start-demo.sh --seed     # además, siembra datos de demostración
./scripts/start-demo.sh --tunnel   # además, abre la URL pública (Cloudflare)
./scripts/start-demo.sh --build    # fuerza recompilar los jars
```

Al terminar verás la **URL local** (`http://localhost:8080`), la **URL pública** (si usaste `--tunnel`)
y el acceso de la cuenta demo. Los logs de cada servicio quedan en `logs/`.

## Sembrar datos (si no usaste `--seed`)

```bash
./scripts/seed-demo.sh                          # contra localhost
./scripts/seed-demo.sh https://xxx.trycloudflare.com   # contra la URL del túnel
```

Crea: cuenta **demo / Demo1234!**, dos gatos, varios meows, una aventura activa,
una aventura completada (insignia + historial) y un reto pendiente.

## Parar

```bash
./scripts/stop-demo.sh             # conserva los datos sembrados
./scripts/stop-demo.sh --clean     # borra también los datos (empezar de cero)
```

## Requisitos

- **JDK 21** y **Docker Desktop** (arrancado).
- Para el túnel: `cloudflared` (`brew install cloudflared`). Si no lo tienes, todo lo demás funciona igual en local.
- Claves opcionales para tiempo real: `export OPENWEATHER_KEY=...  CATAPI_KEY=...` antes de arrancar.
  Sin ellas, los circuit breakers usan valores de reserva y la demo funciona.

## Checklist del día de la defensa

1. **La noche antes**: `./scripts/start-demo.sh --all`, comprueba que todo carga y deja los datos sembrados.
2. Graba un **vídeo de 2-3 min** recorriendo el flujo (feed, aventuras, insignias, rankings) como red de seguridad.
3. **Antes de entrar**: arranca de nuevo, abre `http://localhost:8080`, inicia sesión con `demo` y deja la pestaña lista.
4. Si usas la URL pública del túnel, ábrela una vez antes para confirmar que responde.
5. **Durante**: proyecta `http://localhost:8080` (no depende de la wifi). Menciona la URL pública como extra.
6. Si algo falla en directo: reproduces el vídeo. Demostrado igual.
