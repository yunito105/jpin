/**
 * Enumを使ったDB連携の使用例
 * Enumの値変換とDB操作のシミュレーション
 */
public class SampleEnumTest {
    public static void main(String[] args) {
        // ===== DBに登録するパターン（数値で保存） =====
        System.out.println("=== DB登録時 ===");
        SampleEnum enumValue = SampleEnum.A;
        int dbValue = enumValue.getValue();  // Aは1で保存
        System.out.println("フロント値: " + enumValue.getName());
        System.out.println("DB保存値: " + dbValue);
        
        // ===== DBから取得するパターン（数値で取得） =====
        System.out.println("\n=== DBから取得時 ===");
        int receivedFromDB = 2;  // DBから取得した数値（2を取得）
        SampleEnum convertedEnum = SampleEnum.fromValue(receivedFromDB);  // 2 → B に変換
        
        if (convertedEnum != null) {
            System.out.println("DB取得値: " + receivedFromDB);
            System.out.println("Enum変換: " + convertedEnum.getName());  // Bを取得
            System.out.println("フロント返却値: " + convertedEnum.getName());  // "B"をフロントに返す
        }
        
        // ===== 複数のDB操作例 =====
        System.out.println("\n=== 複数行のDB取得例 ===");
        int[] dbValues = {1, 3, 2};  // DBから複数の値を取得
        
        for (int dbVal : dbValues) {
            SampleEnum converted = SampleEnum.fromValue(dbVal);
            if (converted != null) {
                System.out.println("DB値: " + dbVal + " → フロント値: " + converted.getName());
            }
        }
        
        // ===== Enumの情報を表示 =====
        System.out.println("\n=== 全Enum一覧 ===");
        for (SampleEnum e : SampleEnum.values()) {
            System.out.print("DB値: " + e.getValue() + " → ");
            e.displayInfo();
        }
    }
}
