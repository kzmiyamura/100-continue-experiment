package com.example.continueexperiment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class TestBodyController {

    private static final Logger log = LoggerFactory.getLogger(TestBodyController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 受け取ったボディを解析して結果を返す
     * 様々な形式のJSONボディをテストするためのエンドポイント
     */
    @PostMapping("/test-body")
    public ResponseEntity<Map<String, Object>> testBody(HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();

        // ボディを生のバイト列で読む
        byte[] bodyBytes;
        try {
            bodyBytes = request.getInputStream().readAllBytes();
        } catch (IOException e) {
            result.put("error", "ボディ読み取り失敗: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }

        String bodyText = new String(bodyBytes, StandardCharsets.UTF_8);
        result.put("length", bodyBytes.length);
        result.put("raw", bodyText);

        // ケース判定
        if (bodyBytes.length == 0) {
            result.put("case", "EMPTY_BODY");
            result.put("description", "ボディが空（length=0）");
            result.put("parseResult", "失敗");
            result.put("parseError", "No content to map");
            return ResponseEntity.status(400).body(result);
        }

        // JSONパース試行
        try {
            JsonNode json = objectMapper.readTree(bodyText);
            result.put("case", "VALID_JSON");
            result.put("parseResult", "成功");

            // idフィールドの値確認
            if (json.has("id")) {
                JsonNode idNode = json.get("id");
                if (idNode.isNull()) {
                    result.put("idValue", null);
                    result.put("idCase", "NULL_VALUE");
                    result.put("description", "idはJSONに存在するがnull");
                } else if (idNode.asText().isEmpty()) {
                    result.put("idValue", "");
                    result.put("idCase", "EMPTY_STRING");
                    result.put("description", "idが空文字列");
                } else {
                    result.put("idValue", idNode.asText());
                    result.put("idCase", "NORMAL");
                    result.put("description", "正常なid値");
                }
            } else {
                result.put("idCase", "NO_ID_FIELD");
                result.put("description", "idフィールドなし");
            }

        } catch (Exception e) {
            result.put("case", "INVALID_JSON");
            result.put("parseResult", "失敗");
            result.put("parseError", e.getMessage());
            result.put("description", "不正なJSON（途中で切れているなど）");
            return ResponseEntity.status(400).body(result);
        }

        return ResponseEntity.ok(result);
    }
}
