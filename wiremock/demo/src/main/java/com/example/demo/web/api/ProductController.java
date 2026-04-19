package com.example.demo.web.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// 本番化時: STUB_PRODUCTS を削除し、Service を注入して呼び出す
@RestController
@RequestMapping("/api/v1")
public class ProductController {

    private static final List<Map<String, Object>> STUB_PRODUCTS = List.of(
            Map.of("id", "P001", "name", "北欧風ソファ 3人掛けやったー",    "category", "家具",  "price", 89800,  "stock", 15, "status", "AVAILABLE"),
            Map.of("id", "P002", "name", "シングルベッドフレーム",   "category", "家具",  "price", 34900,  "stock",  8, "status", "AVAILABLE"),
            Map.of("id", "P003", "name", "ダイニングテーブルセット", "category", "家具",  "price", 128000, "stock",  3, "status", "AVAILABLE"),
            Map.of("id", "P004", "name", "フロアスタンドライト",     "category", "照明",  "price", 12800,  "stock", 22, "status", "AVAILABLE"),
            Map.of("id", "P005", "name", "収納ボックス 5個セット",   "category", "収納",  "price", 3980,   "stock",  0, "status", "OUT_OF_STOCK")
    );

    @GetMapping("/products")
    public Map<String, Object> getProducts() {
        return Map.of("products", STUB_PRODUCTS, "total", STUB_PRODUCTS.size());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable String id) {
        return STUB_PRODUCTS.stream()
                .filter(p -> id.equals(p.get("id")))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
