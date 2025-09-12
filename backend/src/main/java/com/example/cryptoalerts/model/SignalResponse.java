package com.example.cryptoalerts.model;

import java.util.List;

/**
 * Response object returned by the {@code /api/signals} endpoint. It
 * encapsulates the most recent trading signal along with the underlying
 * candlestick data that was used to derive the signal. This structure makes
 * it easy for front‑end applications to draw charts and present the
 * explanation to users.
 */
public class SignalResponse {
    private String symbol;
    private String interval;
    private double price;
    private String action;
    private String explanation;
    private double score;
    private List<Candle> candles;

    public SignalResponse() {
    }

    public SignalResponse(String symbol, String interval, double price, String action, String explanation, double score, List<Candle> candles) {
        this.symbol = symbol;
        this.interval = interval;
        this.price = price;
        this.action = action;
        this.explanation = explanation;
        this.score = score;
        this.candles = candles;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public List<Candle> getCandles() {
        return candles;
    }

    public void setCandles(List<Candle> candles) {
        this.candles = candles;
    }
}