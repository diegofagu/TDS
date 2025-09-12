package com.example.cryptoalerts.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility service for computing common technical indicators. All methods
 * operate on simple lists of doubles. In a production system you might
 * consider optimising these routines or delegating them to a dedicated
 * quantitative library. For demonstration purposes they are implemented
 * from scratch here.
 */
public class IndicatorService {

    /**
     * Compute the simple moving average (SMA) for the last element of a series.
     *
     * @param values the series to smooth
     * @param period the number of samples in the moving average
     * @return the most recent SMA or NaN if insufficient data
     */
    public double sma(List<Double> values, int period) {
        if (values == null || values.size() < period || period <= 0) {
            return Double.NaN;
        }
        double sum = 0.0;
        for (int i = values.size() - period; i < values.size(); i++) {
            sum += values.get(i);
        }
        return sum / period;
    }

    /**
     * Compute an exponential moving average (EMA) for the entire series and
     * return only the last value. The EMA is initialised using the first
     * sample in the series.
     *
     * @param values series of values
     * @param period smoothing period
     * @return last EMA value
     */
    public double ema(List<Double> values, int period) {
        if (values == null || values.isEmpty() || period <= 0) {
            return Double.NaN;
        }
        double alpha = 2.0 / (period + 1.0);
        double ema = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            double price = values.get(i);
            ema = price * alpha + ema * (1.0 - alpha);
        }
        return ema;
    }

    /**
     * Compute an entire EMA series. This helper is used for MACD and
     * signal calculations.
     *
     * @param values input series
     * @param period period for EMA
     * @return list of EMA values, same length as {@code values}
     */
    public List<Double> emaSeries(List<Double> values, int period) {
        List<Double> result = new ArrayList<>();
        if (values == null || values.isEmpty() || period <= 0) {
            return result;
        }
        double alpha = 2.0 / (period + 1.0);
        double ema = values.get(0);
        result.add(ema);
        for (int i = 1; i < values.size(); i++) {
            double price = values.get(i);
            ema = price * alpha + ema * (1.0 - alpha);
            result.add(ema);
        }
        return result;
    }

    /**
     * Compute the Relative Strength Index (RSI) for a series. RSI is computed
     * using the classic formula. The result returned is the last RSI value.
     *
     * @param values price series
     * @param period look back period (e.g. 14)
     * @return last RSI value in percent
     */
    public double rsi(List<Double> values, int period) {
        if (values == null || values.size() < period + 1 || period <= 0) {
            return Double.NaN;
        }
        double gain = 0.0;
        double loss = 0.0;
        for (int i = values.size() - period; i < values.size(); i++) {
            double change = values.get(i) - values.get(i - 1);
            if (change >= 0) {
                gain += change;
            } else {
                loss -= change;
            }
        }
        double avgGain = gain / period;
        double avgLoss = loss / period;
        if (avgLoss == 0.0) {
            return 100.0;
        }
        double rs = avgGain / avgLoss;
        return 100.0 - 100.0 / (1.0 + rs);
    }

    /**
     * Compute MACD, signal and histogram. Only the last values are returned.
     *
     * @param values price series
     * @return array of three doubles: [macd, signal, histogram]
     */
    public double[] macd(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return new double[]{Double.NaN, Double.NaN, Double.NaN};
        }
        // Standard MACD parameters: fast=12, slow=26, signal=9
        int fastPeriod = 12;
        int slowPeriod = 26;
        int signalPeriod = 9;
        List<Double> emaFast = emaSeries(values, fastPeriod);
        List<Double> emaSlow = emaSeries(values, slowPeriod);
        List<Double> macdLine = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            double fast = i < emaFast.size() ? emaFast.get(i) : emaFast.get(emaFast.size() - 1);
            double slow = i < emaSlow.size() ? emaSlow.get(i) : emaSlow.get(emaSlow.size() - 1);
            macdLine.add(fast - slow);
        }
        List<Double> macdSignal = emaSeries(macdLine, signalPeriod);
        int lastIndex = macdLine.size() - 1;
        double macd = macdLine.get(lastIndex);
        double signal = macdSignal.get(lastIndex);
        double hist = macd - signal;
        return new double[]{macd, signal, hist};
    }

    /**
     * Compute Bollinger bands. Returns an array of three values: [middle,
     * upper, lower].
     *
     * @param values price series
     * @param period length of window
     * @param stdDevK number of standard deviations (commonly 2.0)
     * @return double array
     */
    public double[] bollingerBands(List<Double> values, int period, double stdDevK) {
        if (values == null || values.size() < period) {
            return new double[]{Double.NaN, Double.NaN, Double.NaN};
        }
        int lastIndex = values.size() - 1;
        double sma = 0.0;
        for (int i = lastIndex - period + 1; i <= lastIndex; i++) {
            sma += values.get(i);
        }
        sma /= period;
        // Compute standard deviation
        double variance = 0.0;
        for (int i = lastIndex - period + 1; i <= lastIndex; i++) {
            double diff = values.get(i) - sma;
            variance += diff * diff;
        }
        variance /= period;
        double stdDev = Math.sqrt(variance);
        double upper = sma + stdDevK * stdDev;
        double lower = sma - stdDevK * stdDev;
        return new double[]{sma, upper, lower};
    }

    /**
     * Compute annualised volatility for the last {@code period} elements. The
     * volatility is estimated as the standard deviation of percentage
     * changes. The result is annualised by multiplying by the square root
     * of the number of samples per year. For intraday data you may wish to
     * adjust this multiplier accordingly.
     *
     * @param values price series
     * @param period look back window
     * @return annualised volatility (e.g. 0.05 corresponds to 5%)
     */
    public double volatility(List<Double> values, int period) {
        if (values == null || values.size() < period + 1) {
            return Double.NaN;
        }
        int start = values.size() - period - 1;
        List<Double> returns = new ArrayList<>();
        for (int i = start + 1; i <= values.size() - 1; i++) {
            double prev = values.get(i - 1);
            double curr = values.get(i);
            if (prev == 0.0) {
                returns.add(0.0);
            } else {
                returns.add((curr - prev) / prev);
            }
        }
        double mean = 0.0;
        for (double r : returns) {
            mean += r;
        }
        mean /= returns.size();
        double variance = 0.0;
        for (double r : returns) {
            double diff = r - mean;
            variance += diff * diff;
        }
        variance /= returns.size();
        double stdDev = Math.sqrt(variance);
        // Annualise using sqrt of periods per year (assume 365). For crypto trading this is a rough approximation.
        double annualised = stdDev * Math.sqrt(365.0);
        return annualised;
    }
}