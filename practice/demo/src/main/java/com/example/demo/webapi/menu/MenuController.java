package com.example.demo.webapi.menu;

import com.example.demo.query.menu.MenuItemView;
import com.example.demo.query.menu.MenuQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * メニューAPI コントローラ。
 *
 * <p>メニュー項目に関するREST APIエンドポイントを提供する。
 * 主に参照（Query）系の操作をサポートする。</p>
 *
 * <h3>エンドポイント一覧</h3>
 * <ul>
 *   <li>{@code GET /api/menu} - メニュー一覧取得</li>
 *   <li>{@code GET /api/menu/available} - 提供可能メニュー一覧取得</li>
 * </ul>
 *
 * @author store-order-system
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/menu")
@Tag(name = "メニューAPI", description = "メニュー項目の参照を行うAPI")
@CrossOrigin(origins = "http://localhost:3000")
public class MenuController {

    private final MenuQueryService menuQueryService;

    /**
     * コンストラクタ。
     *
     * @param menuQueryService メニュークエリサービス
     */
    public MenuController(MenuQueryService menuQueryService) {
        this.menuQueryService = menuQueryService;
    }

    /**
     * メニュー一覧を取得する。
     *
     * @param category カテゴリフィルタ（任意）
     * @return メニュー項目ビュー一覧
     */
    @GetMapping
    @Operation(summary = "メニュー一覧を取得する", description = "カテゴリで絞り込み可能なメニュー項目一覧を取得する")
    @ApiResponse(responseCode = "200", description = "取得成功")
    public ResponseEntity<List<MenuItemView>> getMenuItems(
            @Parameter(description = "カテゴリフィルタ",
                    example = "MAIN_COURSE")
            @RequestParam(required = false) String category) {

        List<MenuItemView> items;
        if (category != null) {
            items = menuQueryService.findMenuItemsByCategory(category);
        } else {
            items = menuQueryService.findAllMenuItems();
        }

        return ResponseEntity.ok(items);
    }

    /**
     * 提供可能なメニュー一覧を取得する。
     *
     * <p>お客様向けの注文画面で使用する。提供不可のメニューは除外される。</p>
     *
     * @return 提供可能なメニュー項目ビュー一覧
     */
    @GetMapping("/available")
    @Operation(summary = "提供可能なメニュー一覧を取得する",
            description = "現在注文可能なメニュー項目のみを取得する。注文画面で使用。")
    @ApiResponse(responseCode = "200", description = "取得成功")
    public ResponseEntity<List<MenuItemView>> getAvailableMenuItems() {
        return ResponseEntity.ok(menuQueryService.findAvailableMenuItems());
    }
}
