package com.example.demo.query.menu;

import com.example.demo.domain.model.menu.MenuCategory;
import com.example.demo.domain.model.menu.MenuItem;
import com.example.demo.domain.model.menu.MenuItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * メニュークエリサービス（Query側）。
 *
 * <p>メニュー項目に関する参照系の操作を提供するサービス。
 * CQRSのQuery側として、読み取り専用のDTOを返却する。</p>
 *
 * <h3>責務</h3>
 * <ul>
 *   <li>メニュー一覧の取得（カテゴリ別フィルタ対応）</li>
 *   <li>提供可能メニューの取得</li>
 *   <li>ドメインモデルからQuery用DTOへの変換</li>
 * </ul>
 *
 * @author store-order-system
 * @since 1.0.0
 * @see MenuItemView
 */
@Service
public class MenuQueryService {

    private final MenuItemRepository menuItemRepository;

    /**
     * コンストラクタ。
     *
     * @param menuItemRepository メニュー項目リポジトリ
     */
    public MenuQueryService(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    /**
     * 全メニュー項目一覧を取得する。
     *
     * @return メニュー項目ビュー一覧
     */
    public List<MenuItemView> findAllMenuItems() {
        return menuItemRepository.findAll().stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    /**
     * 提供可能なメニュー項目一覧を取得する。
     *
     * <p>お客様向けに表示する場合はこのメソッドを使用する。</p>
     *
     * @return 提供可能なメニュー項目ビュー一覧
     */
    public List<MenuItemView> findAvailableMenuItems() {
        return menuItemRepository.findAvailable().stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    /**
     * カテゴリ別にメニュー項目一覧を取得する。
     *
     * @param category カテゴリ名
     * @return 該当カテゴリのメニュー項目ビュー一覧
     */
    public List<MenuItemView> findMenuItemsByCategory(String category) {
        MenuCategory menuCategory = MenuCategory.valueOf(category);
        return menuItemRepository.findByCategory(menuCategory).stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    /**
     * MenuItemエンティティをMenuItemViewに変換する。
     */
    private MenuItemView toView(MenuItem menuItem) {
        return new MenuItemView(
                menuItem.getId().getValue(),
                menuItem.getName(),
                menuItem.getPrice().getAmount(),
                menuItem.getPrice().withTax().getAmount(),
                menuItem.getCategory().name(),
                menuItem.getCategory().getDisplayName(),
                menuItem.isAvailable()
        );
    }
}
