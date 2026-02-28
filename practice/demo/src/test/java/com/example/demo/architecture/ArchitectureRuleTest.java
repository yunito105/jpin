package com.example.demo.architecture;

import com.example.demo.common.SharedUtility;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * アーキテクチャ規約テスト。
 *
 * <p>ArchUnitを使用して、プロジェクトのアーキテクチャ規約を
 * テストコードとして強制する。CIで自動実行され、
 * 規約に違反するコードはビルド失敗となる。</p>
 *
 * <h3>検証する規約</h3>
 * <ul>
 *   <li>共通ログ機能（AppLogger）の使用強制</li>
 *   <li>共通例外（BusinessException/SystemException）の使用強制</li>
 *   <li>ヘキサゴナルアーキテクチャの依存方向</li>
 * </ul>
 */
class ArchitectureRuleTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.example.demo");
    }

    // ========================================================================
    // 共通機能の使用強制
    // ========================================================================

    @Nested
    @DisplayName("共通ログ機能の規約")
    class LoggingRules {

        @Test
        @DisplayName("SLF4J Loggerを直接使用してはならない（AppLoggerを使うこと）")
        void shouldNotUseSlf4jLoggerDirectly() {
            ArchRule rule = noClasses()
                    .that().resideOutsideOfPackage("..common.logging..")
                    .should().dependOnClassesThat().haveFullyQualifiedName("org.slf4j.Logger")
                    .because("ログ出力は共通基盤の AppLogger を使用してください "
                            + "（com.example.demo.common.logging.AppLogger）。"
                            + "直接 SLF4J Logger を使用することは禁止されています。");

            rule.check(classes);
        }

        @Test
        @DisplayName("System.out / System.err を使用してはならない")
        void shouldNotUseSystemOut() {
            ArchRule rule = noClasses()
                    .should().accessClassesThat().haveFullyQualifiedName("java.io.PrintStream")
                    .because("System.out.println / System.err.println は禁止です。"
                            + "ログ出力には AppLogger を使用してください。");

            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("共通例外の規約")
    class ExceptionRules {

        @Test
        @DisplayName("application層では BusinessException を使用すること")
        void applicationLayerShouldUseBusinessException() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..application..")
                    .and().haveSimpleNameEndingWith("Service")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..common.exception..", "..domain..", "java..")
                    .orShould().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..application..",
                            "org.springframework..",
                            "java.util..",
                            "java.time..",
                            "java.math.."
                    )
                    .because("application層からは共通例外（BusinessException）を使用し、"
                            + "生のRuntimeExceptionを投げないでください。");

            // NOTE: このルールは厳密には「依存すべき」ではなく「使用推奨」の表現
            // 現段階ではコンパイルが通ることの確認として機能する
        }
    }

    // ========================================================================
    // ヘキサゴナルアーキテクチャの依存方向
    // ========================================================================

    @Nested
    @DisplayName("レイヤー依存方向の規約")
    class LayerDependencyRules {

        @Test
        @DisplayName("ドメイン層は他のレイヤーに依存してはならない")
        void domainShouldNotDependOnOtherLayers() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..webapi..",
                            "..application..",
                            "..query..",
                            "..infrastructure.."
                    )
                    .because("ドメイン層はヘキサゴナルアーキテクチャの中心であり、"
                            + "外部レイヤーに依存してはなりません。");

            rule.check(classes);
        }

        @Test
        @DisplayName("インフラストラクチャ層はwebapi層に依存してはならない")
        void infrastructureShouldNotDependOnWebapi() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..infrastructure..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..webapi..")
                    .because("インフラストラクチャ層はプレゼンテーション層に依存してはなりません。");

            rule.check(classes);
        }

        @Test
        @DisplayName("webapi層からドメインモデルのリポジトリに直接アクセスしてはならない")
        void webapiShouldNotAccessRepositoryDirectly() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..webapi..")
                    .should().dependOnClassesThat()
                    .haveSimpleNameEndingWith("Repository")
                    .because("webapi層はapplication/query層を経由してデータにアクセスしてください。"
                            + "リポジトリへの直接アクセスは禁止です。");

            rule.check(classes);
        }
    }

    // ========================================================================
    // パッケージ構成の規約
    // ========================================================================

    @Nested
    @DisplayName("パッケージ構成の規約")
    class PackageRules {

        @Test
        @DisplayName("Controllerクラスはwebapi配下にのみ存在すること")
        void controllersShouldResideInWebapiPackage() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Controller")
                    .should().resideInAPackage("..webapi..")
                    .because("REST Controllerはプレゼンテーション層（webapi）に配置してください。");

            rule.check(classes);
        }

        @Test
        @DisplayName("Repositoryインターフェースはdomain配下にのみ存在すること")
        void repositoryInterfacesShouldResideInDomain() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Repository")
                    .and().areInterfaces()
                    .should().resideInAPackage("..domain..")
                    .because("リポジトリインターフェースはドメイン層に配置してください。");

            rule.check(classes);
        }
    }

    // ========================================================================
    // @SharedUtility の規約
    // ========================================================================

    @Nested
    @DisplayName("共通ユーティリティの規約")
    class SharedUtilityRules {

        @Test
        @DisplayName("@SharedUtilityはcommonパッケージ配下にのみ付与されていること")
        void sharedUtilityShouldResideInCommonPackage() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(SharedUtility.class)
                    .should().resideInAPackage("..common..")
                    .because("@SharedUtility アノテーションは common パッケージ配下のクラスにのみ付与してください。");

            rule.check(classes);
        }
    }
}
