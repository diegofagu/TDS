package com.example.cryptoalerts.controller;

import com.example.cryptoalerts.model.Candle;
import com.example.cryptoalerts.model.SignalResponse;
import com.example.cryptoalerts.service.DataService;
import com.example.cryptoalerts.service.SignalService;
import com.example.cryptoalerts.service.PythonMlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing a simple endpoint for retrieving trading
 * recommendations. The endpoint returns the most recent signal along with
 * candlestick data. Optional query parameters allow the client to
 * customise the symbol, interval and number of candles.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SignalController {

    private final DataService dataService;
    private final SignalService signalService;

    @Autowired
    private final PythonMlService pythonMlService;

    public SignalController(DataService dataService, SignalService signalService, PythonMlService pythonMlService) {
        this.dataService = dataService;
        this.signalService = signalService;
        this.pythonMlService = pythonMlService;
    }

    /**
     * Retrieve a trading signal for the specified symbol and interval. If
     * limit is omitted a default of 300 candles is used. The returned JSON
     * includes the computed signal and the underlying candle series.
     *
     * Example: /api/signals?symbol=BTCUSDT&interval=1h&limit=300
     *
     * @param symbol  trading symbol, default BTCUSDT
     * @param interval timeframe (e.g. 1h)
     * @param limit   number of candles (max 1000)
     * @return {@link SignalResponse}
     */
    @GetMapping("/signals")
    public SignalResponse getSignal(
            @RequestParam(name = "symbol", defaultValue = "BTCUSDT") String symbol,
            @RequestParam(name = "interval", defaultValue = "1h") String interval,
            @RequestParam(name = "limit", defaultValue = "300") int limit,
            @RequestParam(name = "engine", defaultValue = "java") String engine) {
        // Cap the limit to avoid excessive memory usage
        int safeLimit = Math.max(10, Math.min(limit, 1000));
        List<Candle> candles = dataService.getCandles(symbol, interval, safeLimit);
        // Delegate depending on requested engine
        if (engine != null) {
            String eng = engine.toLowerCase();
            if (eng.equals("python_rules")) {
                // Call Python rules engine
                try {
                    String label = pythonMlService.callRules(candles);
                    String explanation = "Señal generada por motor Python (reglas)";
                    double lastPrice = candles.get(candles.size() - 1).getClose();
                    return new SignalResponse(symbol, interval, lastPrice, label, explanation, 0.0, candles);
                } catch (Exception e) {
                    // Fallback to Java if Python service fails
                    SignalResponse fallback = signalService.analyse(symbol, interval, candles);
                    fallback.setExplanation(fallback.getExplanation() + "; fallback por error en motor Python");
                    return fallback;
                }
            } else if (eng.equals("python_model")) {
                // Call Python model engine
                try {
                    String label = pythonMlService.callModel(candles);
                    String explanation = "Señal generada por modelo Python";
                    double lastPrice = candles.get(candles.size() - 1).getClose();
                    return new SignalResponse(symbol, interval, lastPrice, label, explanation, 0.0, candles);
                } catch (Exception e) {
                    SignalResponse fallback = signalService.analyse(symbol, interval, candles);
                    fallback.setExplanation(fallback.getExplanation() + "; fallback por error en modelo Python");
                    return fallback;
                }
            }
        }
        return signalService.analyse(symbol, interval, candles);
    }
}