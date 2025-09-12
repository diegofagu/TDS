# Crypto Alerts

Este proyecto proporciona una **aplicación full stack** que genera señales de trading para
criptomonedas. Consta de un **backend en Java (Spring Boot)** que calcula
indicadores técnicos y emite recomendaciones (COMPRA/HOLD/VENTA) y un
**frontend en React** que muestra un gráfico de velas junto con la señal y
su explicación.

## Estructura del proyecto

- `backend/` – API REST en Java 17 con Spring Boot.
  - `pom.xml` – archivo Maven con las dependencias.
  - `src/main/java/com/example/cryptoalerts/` – código fuente del servicio.
    - `Candle.java` – modelo de vela OHLCV.
    - `SignalResponse.java` – objeto de respuesta con la señal y las velas.
    - `IndicatorService.java` – implementaciones de SMA, EMA, RSI, MACD, Bandas de Bollinger y volatilidad.
    - `DataService.java` – obtiene los datos de Binance o genera datos simulados (mock).
    - `SignalService.java` – combina los indicadores en un score y genera la recomendación.
    - `SignalController.java` – endpoint `/api/signals`.
  - `src/main/resources/application.properties` – configuración; por defecto
    utiliza datos simulados (`app.datasource=MOCK`).
- `frontend/` – interfaz gráfica en React + Vite.
  - `package.json` – dependencias (React, lightweight‑charts, axios).
  - `vite.config.ts` – configuración de Vite.
  - `public/index.html` – página de entrada.
  - `src/` – código React en TypeScript.
    - `App.tsx` – componente principal con selector de símbolo/intervalo y actualización periódica.
    - `api.ts` – helper para consumir el backend.
    - `components/Chart.tsx` – gráfico de velas con lightweight‑charts.
    - `components/SignalPanel.tsx` – panel con la señal y explicación.
    - `types.ts` – definiciones de tipos para TypeScript.

## Requisitos

- **Backend:** JDK 17 y Maven 3.
- **Frontend:** Node.js >= 18 y npm (o pnpm/yarn) para instalar las
  dependencias.

## Cómo ejecutar el backend

1. Abre una terminal y ve al directorio `backend/`.
2. Instala las dependencias y arranca el servidor:

   ```bash
   mvn spring-boot:run
   ```

   El servicio escuchará por defecto en `http://localhost:8080`. Puedes
   comprobar el endpoint con:

   ```bash
   curl "http://localhost:8080/api/signals?symbol=BTCUSDT&interval=1h&limit=300"
   ```

3. Para obtener datos reales de Binance en lugar de datos simulados,
   establece la variable `app.datasource` en `BINANCE`. Por ejemplo:

   ```bash
   mvn spring-boot:run -Dapp.datasource=BINANCE
   ```

   Si la API de Binance no está disponible o no tienes conectividad, el
   servicio volverá a utilizar datos simulados automáticamente.

## Cómo ejecutar el frontend

1. En otra terminal ve al directorio `frontend/`.
2. Instala las dependencias:

   ```bash
   npm install
   ```

3. Crea un archivo `.env` en el directorio `frontend/` con el
   siguiente contenido para indicar la URL del backend (si es distinta
   a la predeterminada):

   ```env
   VITE_API_BASE_URL=http://localhost:8080/api
   ```

4. Arranca la aplicación en modo desarrollo:

   ```bash
   npm run dev
   ```

   La interfaz estará disponible en `http://localhost:5173`. Podrás
   seleccionar el símbolo (BTC/ETH) y el intervalo (1m–1d) y el sistema
   mostrará el gráfico de velas con la recomendación actual.

## Personalización

- Ajusta los **umbrales y pesos** en `SignalService.java` para adaptar
  la sensibilidad de las señales.
- La fuente de datos puede configurarse en `application.properties` o
  sobreescribirse mediante argumentos (`--app.datasource=BINANCE`).
- Para desplegar en producción, ejecuta `mvn package` y utiliza el
  archivo JAR resultante (`java -jar target/crypto-alerts-0.0.1-SNAPSHOT.jar`).
- El frontend puede compilarse con `npm run build` y desplegarse en
  cualquier servidor estático.

## Notas finales

Este proyecto es un punto de partida funcional que ilustra cómo
combinar análisis técnico en el backend con una visualización
interactiva en el frontend. No está pensado como un sistema de
trading automático de alta frecuencia ni como asesor financiero.

## Integración con el microservicio ML (Python)

El repositorio incluye un subdirectorio `ml/` con un microservicio
FastAPI opcional que ejecuta el mismo motor de reglas y un modelo de
machine learning (Gradient Boosting). Este servicio puede ser
levantado en paralelo al backend Java para experimentar con modelos
personalizados.

### Cómo iniciar el servicio ML

1. Ve al directorio `ml/` y crea un entorno virtual si lo deseas.
2. Instala las dependencias:

   ```bash
   pip install -r requirements.txt
   ```

3. Inicia el servidor:

   ```bash
   uvicorn serve:app --host 0.0.0.0 --port 8001 --reload
   ```

### Uso desde el backend Java

El controlador Java acepta un parámetro opcional `engine` en el
endpoint `/api/signals`:

```
GET /api/signals?symbol=BTCUSDT&interval=1h&limit=300&engine=python_rules
```

- `engine=java` (valor por defecto) – Usa el motor de reglas
  implementado en Java.
- `engine=python_rules` – Envía los candles al microservicio y
  utiliza `/signal/rules` para obtener la recomendación.
- `engine=python_model` – Envía los candles al microservicio y
  utiliza `/signal/model` para obtener la recomendación del modelo.
  **Es necesario entrenar el modelo primero** con el endpoint
  `/train/model`.

### Entrenamiento del modelo

Para entrenar un modelo personalizado se debe preparar un CSV con
series históricas y llamar a:

```bash
curl -F "file=@data.csv" -F "horizon=5" -F "up=0.03" -F "down=-0.03" \
     http://localhost:8001/train/model
```

Esto creará un archivo `model.pkl` en el directorio `ml/` que
posteriormente se utilizará para las predicciones.