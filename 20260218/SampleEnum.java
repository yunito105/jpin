/**
 * 振る舞いを持つEnum型クラスです。
 * 各定数は名前と値を持ち、表示やチェック処理が可能です。
 */
public enum SampleEnum {
    A("A", 1),
    B("B", 2),
    C("C", 3);
    
    private final String name;
    private final int value;
    
    /**
     * Enumのコンストラクタ
     * @param name 名前
     * @param value 値
     */
    SampleEnum(String name, int value) {
        this.name = name;
        this.value = value;
    }
    
    /**
     * 名前を取得します
     * @return String 名前
     */
    public String getName() {
        return name;
    }
    
    /**
     * 値を取得します
     * @return int 値
     */
    public int getValue() {
        return value;
    }
    
    /**
     * 値から対応するEnumを取得します
     * @param value 値
     * @return 対応するSampleEnum、存在しない場合はnull
     */
    public static SampleEnum fromValue(int value) {
        for (SampleEnum e : SampleEnum.values()) {
            if (e.value == value) {
                return e;
            }
        }
        return null;
    }
    
    /**
     * 情報を表示します
     */
    public void displayInfo() {
        System.out.println("Name: " + name + ", Value: " + value);
    }
    
    /**
     * 値の大小を判定します
     * @param other 比較対象
     * @return thisがotherより大きい場合true
     */
    public boolean isGreaterThan(SampleEnum other) {
        return this.value > other.value;
    }
}
