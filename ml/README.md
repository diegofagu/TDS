# Crypto Alerts – Microservicio de IA (Python)

Este submódulo implementa un **microservicio FastAPI** para generar
señales de trading de criptomonedas mediante reglas técnicas y un
**modelo de machine learning supervisado**. El servicio expone
endpoints HTTP que pueden consumirse desde el backend Java o
directamente desde herramientas de línea de comandos como `curl`.

## Estructura

- `data.py` – funciones para leer archivos OHLCV en CSV y calcular
  indicadores (EMA, RSI, MACD, Bandas de Bollinger, volatilidad,
  retornos). También construye el conjunto de características para el
  modelo.
- `rules.py` – motor de reglas que combina los indicadores en
  recomendaciones BUY/HOLD/SELL.
- `model.py` – utilidades para entrenar un modelo de clasificación
  (*GradientBoostingClassifier* de scikit‑learn), guardarlo y hacer
  inferencias.
- `backtest.py` – función sencilla para probar señales y calcular
  métricas como CAGR, drawdown y ratio de Sharpe.
- `serve.py` – API FastAPI que expone endpoints para obtener señales
  mediante reglas y modelo, así como para entrenar el modelo.
- `requirements.txt` – dependencias Python necesarias.

## Instalación y ejecución

1. Instala las dependencias en un entorno virtual:

   ```bash
   cd ml
   pip install -r requirements.txt
   ```

2. Inicia el servidor FastAPI en modo desarrollo (auto‑recarga):

   ```bash
   uvicorn serve:app --host 0.0.0.0 --port 8001 --reload
   ```

   El servicio quedará accesible en `http://localhost:8001`.

## Endpoints

### POST `/signal/rules`

Devuelve la señal generada por el motor de reglas. Debe enviarse un
archivo CSV con las velas (`file`) en formato `multipart/form-data`.
Opcionalmente se pueden especificar `asset` y `timeframe` como datos
del formulario. Respuesta:

```json
{
  "asset": "BTCUSDT",
  "timeframe": "1h",
  "last": 1,
  "label": "BUY"
}
```

### POST `/train/model`

Entrena un modelo de clasificación con etiquetas generadas por
retornos futuros. Se envía un CSV de velas junto con parámetros
opcionalmente configurables:

* `horizon` – número de velas hacia adelante para calcular el retorno
  (por defecto 5).
* `up` – umbral de beneficio (ej. `0.03` = +3 %).
* `down` – umbral de pérdida (ej. `-0.03` = −3 %).

Devuelve los informes de las validaciones cruzadas y guarda el modelo
en `model.pkl` en disco.

### POST `/signal/model`

Predice la señal con el modelo entrenado. Si no existe `model.pkl`, el
endpoint devolverá un error. Se envía un CSV con velas y se obtiene
una señal en el formato mostrado en el endpoint de reglas.

## Integración con el backend Java

El backend Java puede invocar este servicio mediante peticiones
HTTP. Se recomienda enviar las velas recientes como CSV (generadas
por el `DataService`), especificando el endpoint deseado en el
parámetro `engine` de la API Java:

- `engine=python_rules` → usa `/signal/rules` para obtener la señal
  basada en reglas.
- `engine=python_model` → usa `/signal/model` para obtener la señal del
  modelo (requiere haber entrenado previamente). Si no se entrena
  manualmente, siempre se puede recurrir a las reglas.

En el README principal se documenta cómo configurar el backend para
realizar estas llamadas.