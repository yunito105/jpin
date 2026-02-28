package com.example.demo.webapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI（Swagger）設定クラス。
 *
 * <p>APIドキュメントのメタ情報を定義する。
 * {@code /swagger-ui/index.html} でSwagger UIにアクセス可能。
 * {@code /v3/api-docs} でOpenAPI JSON仕様を取得可能。</p>
 *
 * @author store-order-system
 * @since 1.0.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * OpenAPIのカスタム設定Bean。
     *
     * @return OpenAPI設定
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("店舗ご注文システム API")
                        .version("1.0.0")
                        .description("""
                                実店舗で使用する注文管理システムのREST API仕様書。
                                
                                ## 概要
                                - テーブル単位での注文管理
                                - メニュー項目の参照
                                - 注文ステータスのライフサイクル管理
                                
                                ## アーキテクチャ
                                ヘキサゴナルアーキテクチャ + 簡易CQRS を採用。
                                
                                ## ステータス遷移
                                ```
                                PLACED → PREPARING → READY → SERVED
                                  ↓         ↓
                                CANCELLED  CANCELLED
                                ```
                                """)
                        .contact(new Contact()
                                .name("開発チーム")
                                .email("dev@example.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("ローカル開発環境")
                ));
    }
}
