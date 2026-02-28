package com.example.demo.infrastructure.database;

import com.example.demo.domain.model.menu.MenuCategory;
import com.example.demo.domain.model.menu.MenuItem;
import com.example.demo.domain.model.menu.MenuItemId;
import com.example.demo.domain.model.menu.MenuItemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * メニュー項目リポジトリのインメモリ実装。
 *
 * <p>ヘキサゴナルアーキテクチャにおけるアダプタ（出力アダプタ）として、
 * メニュー項目の永続化をメモリ上で実現する。DB不要の検証用実装。</p>
 *
 * <p>スレッドセーフな {@link ConcurrentHashMap} を使用し、
 * 同時アクセスに対応する。</p>
 *
 * @author store-order-system
 * @since 1.0.0
 * @see MenuItemRepository
 */
@Repository
public class InMemoryMenuItemRepository implements MenuItemRepository {

    /** インメモリストレージ */
    private final Map<String, MenuItem> store = new ConcurrentHashMap<>();

    @Override
    public void save(MenuItem menuItem) {
        store.put(menuItem.getId().getValue(), menuItem);
    }

    @Override
    public Optional<MenuItem> findById(MenuItemId id) {
        return Optional.ofNullable(store.get(id.getValue()));
    }

    @Override
    public List<MenuItem> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public List<MenuItem> findByCategory(MenuCategory category) {
        return store.values().stream()
                .filter(item -> item.getCategory() == category)
                .collect(Collectors.toList());
    }

    @Override
    public List<MenuItem> findAvailable() {
        return store.values().stream()
                .filter(MenuItem::isAvailable)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(MenuItemId id) {
        store.remove(id.getValue());
    }
}
