package com.example.cryptoalerts.service;

import com.example.cryptoalerts.model.Candle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Client service for interacting with the optional Python ML microservice.
 * It converts candle data into CSV, sends multipart/form-data requests
 * to the configured endpoint and parses the returned labels. If the
 * microservice is unavailable or returns an error, the caller should
 * handle the exception gracefully.
 */
@Service
public class PythonMlService {

    @Value("${ml.service.url:http://localhost:8001}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send candles to the rule-based engine in the Python microservice.
     * Returns the textual label (BUY/HOLD/SELL).
     */
    public String callRules(List<Candle> candles) {
        return callEngine(candles, "/signal/rules");
    }

    /**
     * Send candles to the ML model engine in the Python microservice.
     * Returns the textual label (BUY/HOLD/SELL).
     */
    public String callModel(List<Candle> candles) {
        return callEngine(candles, "/signal/model");
    }

    private String callEngine(List<Candle> candles, String path) {
        // Prepare CSV in memory
        String csv = toCsv(candles);
        // Build multipart request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new InMemoryMultipartFile("data.csv", csv.getBytes(StandardCharsets.UTF_8)));
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        // Make POST request
        String url = mlServiceUrl + path;
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        Map<?, ?> map = response.getBody();
        if (map != null && map.containsKey("label")) {
            return map.get("label").toString();
        }
        return "HOLD";
    }

    /**
     * Serialise candles to CSV format expected by the Python API.
     */
    private String toCsv(List<Candle> candles) {
        String[] headers = {"date", "open", "high", "low", "close", "volume"};
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out)) {
            writer.println(String.join(",", headers));
            for (Candle c : candles) {
                writer.printf("%s,%f,%f,%f,%f,%f%n",
                        c.getTimestamp().atZone(ZoneOffset.UTC).toInstant().toString(),
                        c.getOpen(), c.getHigh(), c.getLow(), c.getClose(), c.getVolume());
            }
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    /**
     * Custom ByteArrayResource with a filename for multipart requests.
     */
    private static class InMemoryMultipartFile extends ByteArrayResource {
        private final String filename;
        public InMemoryMultipartFile(String filename, byte[] byteArray) {
            super(byteArray);
            this.filename = filename;
        }
        @Override
        public String getFilename() {
            return this.filename;
        }
    }
}