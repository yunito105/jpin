-- ============================================
-- Oracle 初期化スクリプト
-- コンテナ初回起動時に自動実行される
-- ============================================

-- Keycloak用ユーザー作成
CREATE USER keycloak IDENTIFIED BY keycloak_password
    DEFAULT TABLESPACE users
    QUOTA UNLIMITED ON users;

GRANT CONNECT, RESOURCE TO keycloak;
GRANT CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO keycloak;

-- アプリケーション用スキーマ設定
-- (sales_appユーザーはgvenzl/oracle-freeのAPP_USER環境変数で自動作成)
GRANT CREATE VIEW, CREATE MATERIALIZED VIEW TO sales_app;
GRANT CREATE SYNONYM TO sales_app;

-- Query側最適化用: Materialized View権限
GRANT QUERY REWRITE TO sales_app;
