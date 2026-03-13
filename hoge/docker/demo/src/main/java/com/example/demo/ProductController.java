package com.example.demo;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    /** 商品一覧取得 */
    @GetMapping
    public List<Product> findAll(
            @RequestParam(required = false) String category) {
        if (category != null) {
            return repository.findByCategory(category);
        }
        return repository.findAll();
    }

    /** 商品追加 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@RequestBody CreateProductRequest request) {
        Product product = new Product(
                request.name(),
                request.price(),
                request.category());
        return repository.save(product);
    }

    record CreateProductRequest(String name, Integer price, String category) {
    }
}
