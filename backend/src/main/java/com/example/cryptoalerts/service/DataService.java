package com.example.cryptoalerts.service;

import com.example.cryptoalerts.model.Candle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Responsible for retrieving OHLCV data from the configured data source. When
 * running in production the service can connect to Binance's REST API. In
 * offline or development environments the service falls back to a
 * deterministic mock generator that produces synthetic candles.
 */
@Service
public class DataService {

    /**
     * Possible values are BINANCE or MOCK. If unspecified defaults to MOCK.
     */
    @Value("${app.datasource:MOCK}")
    private String datasource;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Fetch candles for a given symbol and interval. If the data source is
     * BINANCE the method will perform an HTTP request to the Binance public
     * API. Otherwise it falls back to a mock generator.
     *
     * @param symbol  trading pair, e.g. BTCUSDT
     * @param interval timeframe (e.g. 1m, 5m, 1h, 1d)
     * @param limit   maximum number of candles to return
     * @return list of candles in ascending chronological order
     */
    @SuppressWarnings("unchecked")
    public List<Candle> getCandles(String symbol, String interval, int limit) {
        if ("BINANCE".equalsIgnoreCase(datasource)) {
            try {
                String url = String.format(
                        "https://api.binance.com/api/v3/klines?symbol=%s&interval=%s&limit=%d",
                        symbol.toUpperCase(), interval, limit);
                ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
                Object body = response.getBody();
                List<Candle> candles = new ArrayList<>();
                if (body instanceof List) {
                    for (Object row : ((List<?>) body)) {
                        if (row instanceof List) {
                            List<?> data = (List<?>) row;
                            // Kline array: [openTime, open, high, low, close, volume, ...]
                            long openTime = ((Number) data.get(0)).longValue();
                            double open = Double.parseDouble((String) data.get(1));
                            double high = Double.parseDouble((String) data.get(2));
                            double low = Double.parseDouble((String) data.get(3));
                            double close = Double.parseDouble((String) data.get(4));
                            double volume = Double.parseDouble((String) data.get(5));
                            candles.add(new Candle(Instant.ofEpochMilli(openTime), open, high, low, close, volume));
                        }
                    }
                }
                return candles;
            } catch (Exception ex) {
                // If the HTTP request fails for any reason, fall back to mock
                return generateMock(symbol, interval, limit);
            }
        }
        return generateMock(symbol, interval, limit);
    }

    /**
     * Generate a synthetic candle series. Prices are generated using a
     * bounded random walk with slight drift. This helps illustrate the
     * functionality of the signal engine without relying on external APIs.
     *
     * @param symbol  trading pair
     * @param interval timeframe string (m, h, d)
     * @param limit   number of candles
     * @return list of synthetic candles
     */
    public List<Candle> generateMock(String symbol, String interval, int limit) {
        List<Candle> candles = new ArrayList<>();
        long now = System.currentTimeMillis();
        long step = intervalToMillis(interval);
        Random random = new Random(symbol.hashCode());
        // Start price depending on symbol for determinism
        double price = 1000.0 + (Math.abs(symbol.hashCode() % 5000));
        for (int i = 0; i < limit; i++) {
            // simulate some random walk around the price
            double pctChange = (random.nextDouble() - 0.5) * 0.02; // ±1% change
            double close = price * (1.0 + pctChange);
            double high = Math.max(price, close) * (1.0 + random.nextDouble() * 0.005);
            double low = Math.min(price, close) * (1.0 - random.nextDouble() * 0.005);
            double open = price;
            double volume = 10 + random.nextDouble() * 100;
            long time = now - (long) (limit - i) * step;
            candles.add(new Candle(Instant.ofEpochMilli(time), open, high, low, close, volume));
            price = close;
        }
        // ensure ascending order by time
        Collections.sort(candles, (c1, c2) -> c1.getTimestamp().compareTo(c2.getTimestamp()));
        return candles;
    }

    /**
     * Convert an interval string into milliseconds. Supports Binance style
     * intervals such as 1m, 3m, 5m, 15m, 1h, 4h, 1d, 1w. Anything else
     * defaults to minutes.
     *
     * @param interval string containing a number followed by a unit letter
     * @return milliseconds per interval
     */
    private long intervalToMillis(String interval) {
        if (interval == null || interval.isEmpty()) {
            return 60_000L;
        }
        String numberPart = interval.substring(0, interval.length() - 1);
        char unit = interval.charAt(interval.length() - 1);
        long quantity;
        try {
            quantity = Long.parseLong(numberPart);
        } catch (NumberFormatException ex) {
            quantity = 1L;
        }
        switch (unit) {
            case 'm': // minutes
            case 'M':
                return quantity * 60_000L;
            case 'h': // hours
            case 'H':
                return quantity * 60_000L * 60L;
            case 'd': // days
            case 'D':
                return quantity * 60_000L * 60L * 24L;
            case 'w': // weeks
            case 'W':
                return quantity * 60_000L * 60L * 24L * 7L;
            default:
                return quantity * 60_000L;
        }
    }
}