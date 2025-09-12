package com.example.cryptoalerts.service;

import com.example.cryptoalerts.model.Candle;
import com.example.cryptoalerts.model.SignalResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Combines indicator values into a simple scoring system and produces
 * actionable trading signals. The logic here follows the heuristic rules
 * described in the technical specification. To fine‑tune the behaviour you
 * can adjust the weighting factors and thresholds accordingly.
 */
@Service
public class SignalService {

    private final IndicatorService indicatorService;

    public SignalService(IndicatorService indicatorService) {
        this.indicatorService = indicatorService;
    }

    /**
     * Analyse a series of candles and produce a signal along with an
     * explanation. The explanation lists the technical factors that
     * contributed to the final score.
     *
     * @param symbol   trading symbol (e.g. BTCUSDT)
     * @param interval timeframe (e.g. 1h)
     * @param candles  historical candles ordered by time ascending
     * @return {@link SignalResponse} containing the signal and data used
     */
    public SignalResponse analyse(String symbol, String interval, List<Candle> candles) {
        if (candles == null || candles.size() < 5) {
            return new SignalResponse(symbol, interval, 0.0, "HOLD", "Datos insuficientes", 0.0, candles);
        }
        // Extract close prices
        List<Double> closes = candles.stream()
                .map(Candle::getClose)
                .collect(Collectors.toList());
        int lastIndex = closes.size() - 1;

        // Compute indicators
        double ema50 = indicatorService.ema(closes, 50);
        double ema200 = indicatorService.ema(closes, 200);
        double rsi14 = indicatorService.rsi(closes, 14);
        double[] macd = indicatorService.macd(closes);
        double[] macdPrev = indicatorService.macd(closes.subList(0, lastIndex));
        double lastMacd = macd[0];
        double lastMacdSignal = macd[1];
        double prevMacd = macdPrev[0];
        double prevMacdSignal = macdPrev[1];
        double[] bb = indicatorService.bollingerBands(closes, 20, 2.0);
        double bbMid = bb[0];
        double vol14 = indicatorService.volatility(closes, 14);
        double lastClose = closes.get(lastIndex);

        // Scoring rules
        int score = 0;
        List<String> reasons = new ArrayList<>();
        // Trend: EMA50 vs EMA200
        if (!Double.isNaN(ema50) && !Double.isNaN(ema200)) {
            if (lastClose > ema50 && ema50 > ema200) {
                score += 2;
                reasons.add("tendencia alcista (EMA50 > EMA200)");
            } else if (lastClose < ema50 && ema50 < ema200) {
                score -= 2;
                reasons.add("tendencia bajista (EMA50 < EMA200)");
            }
        }
        // MACD cross
        if (!Double.isNaN(lastMacd) && !Double.isNaN(lastMacdSignal) &&
            !Double.isNaN(prevMacd) && !Double.isNaN(prevMacdSignal)) {
            if (lastMacd > lastMacdSignal && prevMacd <= prevMacdSignal) {
                score += 2;
                reasons.add("cruce alcista de MACD");
            } else if (lastMacd < lastMacdSignal && prevMacd >= prevMacdSignal) {
                score -= 2;
                reasons.add("cruce bajista de MACD");
            }
        }
        // RSI
        if (!Double.isNaN(rsi14)) {
            if (rsi14 < 30) {
                score += 1;
                reasons.add("RSI indica sobreventa");
            } else if (rsi14 > 70) {
                score -= 1;
                reasons.add("RSI indica sobrecompra");
            } else {
                reasons.add("RSI neutral");
            }
        }
        // Bollinger
        if (!Double.isNaN(bbMid)) {
            if (lastClose > bbMid) {
                score += 1;
                reasons.add("precio por encima de la media de Bollinger");
            } else {
                score -= 1;
                reasons.add("precio por debajo de la media de Bollinger");
            }
        }
        // Volatility penalty: high volatility reduces confidence
        if (!Double.isNaN(vol14)) {
            if (vol14 > 0.7) { // extremely high volatility (approx 70% annualised)
                score -= 2;
                reasons.add("volatilidad muy alta");
            } else if (vol14 > 0.4) { // moderate volatility
                score -= 1;
                reasons.add("volatilidad alta");
            }
        }
        // Determine action
        String action;
        if (score >= 3) {
            action = "COMPRA";
        } else if (score <= -3) {
            action = "VENTA";
        } else {
            action = "HOLD";
        }
        // Build explanation
        String explanation = String.join(", ", reasons);
        return new SignalResponse(symbol, interval, lastClose, action, explanation, score, candles);
    }
}