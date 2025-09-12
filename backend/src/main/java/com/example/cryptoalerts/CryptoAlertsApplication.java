package com.example.cryptoalerts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the crypto alerts backend.
 *
 * The application exposes a small REST API for retrieving trading signals
 * based on real‑time or simulated OHLCV data. See {@code SignalController}
 * for details on the exposed endpoints.
 */
@SpringBootApplication
public class CryptoAlertsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptoAlertsApplication.class, args);
    }
}