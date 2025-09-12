package com.example.cryptoalerts.model;

import java.time.Instant;

/**
 * Represents a single OHLCV candle. A candle aggregates trading data for a
 * specific time interval. It contains an open price, a high price, a low
 * price, a close price, and the traded volume for that interval.
 */
public class Candle {
    private Instant timestamp;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;

    public Candle() {
    }

    public Candle(Instant timestamp, double open, double high, double low, double close, double volume) {
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }
}