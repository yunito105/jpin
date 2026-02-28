package com.example.demo.infrastructure.database;

import com.example.demo.domain.model.menu.*;
import com.example.demo.domain.type.Money;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * メニュー初期データ投入コンポーネント。
 *
 * <p>アプリケーション起動時にサンプルのメニューデータを投入する。
 * DB不使用のため、インメモリリポジトリに直接データを登録する。</p>
 *
 * @author store-order-system
 * @since 1.0.0
 */
@Component
public class MenuDataInitializer implements CommandLineRunner {

    private final MenuItemRepository menuItemRepository;

    /**
     * コンストラクタ。
     *
     * @param menuItemRepository メニュー項目リポジトリ
     */
    public MenuDataInitializer(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    @Override
    public void run(String... args) {
        // 前菜
        saveMenuItem("appetizer-001", "シーザーサラダ", 580, MenuCategory.APPETIZER);
        saveMenuItem("appetizer-002", "枝豆", 380, MenuCategory.APPETIZER);
        saveMenuItem("appetizer-003", "冷奴", 350, MenuCategory.APPETIZER);

        // メイン料理
        saveMenuItem("main-001", "ハンバーグステーキ", 1280, MenuCategory.MAIN_COURSE);
        saveMenuItem("main-002", "チキン南蛮", 980, MenuCategory.MAIN_COURSE);
        saveMenuItem("main-003", "サーモンのグリル", 1480, MenuCategory.MAIN_COURSE);
        saveMenuItem("main-004", "和風パスタ", 980, MenuCategory.MAIN_COURSE);

        // サイドメニュー
        saveMenuItem("side-001", "ライス", 200, MenuCategory.SIDE_DISH);
        saveMenuItem("side-002", "味噌汁", 150, MenuCategory.SIDE_DISH);
        saveMenuItem("side-003", "フライドポテト", 380, MenuCategory.SIDE_DISH);

        // ドリンク
        saveMenuItem("drink-001", "烏龍茶", 250, MenuCategory.DRINK);
        saveMenuItem("drink-002", "コーラ", 280, MenuCategory.DRINK);
        saveMenuItem("drink-003", "生ビール", 550, MenuCategory.DRINK);
        saveMenuItem("drink-004", "オレンジジュース", 300, MenuCategory.DRINK);

        // デザート
        saveMenuItem("dessert-001", "バニラアイス", 350, MenuCategory.DESSERT);
        saveMenuItem("dessert-002", "チョコレートケーキ", 480, MenuCategory.DESSERT);
    }

    private void saveMenuItem(String id, String name, int price, MenuCategory category) {
        MenuItem item = new MenuItem(
                MenuItemId.of(id),
                name,
                Money.of(price),
                category
        );
        menuItemRepository.save(item);
    }
}
