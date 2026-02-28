package com.example.demo.infrastructure.database;

import com.example.demo.domain.model.order.*;
import com.example.demo.domain.type.TableNumber;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 注文リポジトリのインメモリ実装。
 *
 * <p>ヘキサゴナルアーキテクチャにおけるアダプタ（出力アダプタ）として、
 * 注文の永続化をメモリ上で実現する。DB不要の検証用実装。</p>
 *
 * <p>スレッドセーフな {@link ConcurrentHashMap} を使用し、
 * 同時アクセスに対応する。</p>
 *
 * @author store-order-system
 * @since 1.0.0
 * @see OrderRepository
 */
@Repository
public class InMemoryOrderRepository implements OrderRepository {

    /** インメモリストレージ */
    private final Map<String, Order> store = new ConcurrentHashMap<>();

    @Override
    public void save(Order order) {
        store.put(order.getId().getValue(), order);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(store.get(id.getValue()));
    }

    @Override
    public List<Order> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public List<Order> findActiveByTableNumber(TableNumber tableNumber) {
        return store.values().stream()
                .filter(order -> order.getTableNumber().equals(tableNumber))
                .filter(Order::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return store.values().stream()
                .filter(order -> order.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(OrderId id) {
        store.remove(id.getValue());
    }
}
