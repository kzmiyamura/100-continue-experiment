package com.example.continueexperiment.controller;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class JsonbTestController {

    private static final Logger log = LoggerFactory.getLogger(JsonbTestController.class);
    private final Jsonb jsonb = JsonbBuilder.create();

    public static class ScreenData {
        public String id;
    }

    /**
     * JSON-Bで実際にパースして JsonbException が起きるか確認するエンドポイント
     */
    @PostMapping("/test-jsonb")
    public ResponseEntity<Map<String, Object>> testJsonb(HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();

        // ボディを読む
        byte[] bodyBytes;
        try {
            bodyBytes = request.getInputStream().readAllBytes();
        } catch (IOException e) {
            result.put("step", "BODY_READ");
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }

        String bodyText = new String(bodyBytes, StandardCharsets.UTF_8);
        result.put("length", bodyBytes.length);
        result.put("raw", bodyText);

        // JSON-Bでパース（OpenLibertyと同じ処理）
        try {
            ScreenData data = jsonb.fromJson(bodyText, ScreenData.class);

            result.put("step", "PARSE_OK");
            result.put("parseResult", "成功");
            result.put("id", data.id);

            // nullチェックなしで使う（本番でよくあるパターン）
            result.put("idLength", data.id == null ? "null → NullPointerException発生！" : data.id.length());

            return ResponseEntity.ok(result);

        } catch (JsonbException e) {
            log.error("JsonbException発生: body=[{}] error={}", bodyText, e.getMessage());
            result.put("step", "PARSE_ERROR");
            result.put("exception", "JsonbException");
            result.put("message", e.getMessage());
            result.put("cause", e.getCause() != null ? e.getCause().getMessage() : null);
            return ResponseEntity.status(400).body(result);
        }
    }
}
