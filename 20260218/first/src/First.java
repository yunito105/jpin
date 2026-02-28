/**
 * 再帰を使用して九九表を作成するクラス
 */
public class First {

    /**
     * メインメソッド
     */
    public static void main(String[] args) {
        //九九表を再帰で作成
        printTable(1, 1);
    }

    /**
     * 再帰を使用して九九表を出力します
     * @param row 行番号（1-9）
     * @param col 列番号（1-9）
     */
    public static void printTable(int row, int col) {
        // 終了条件：行が9を超えたら終了
        if (row > 9) {
            return;
        }
        
        // 列が9を超えたら改行して次の行へ
        if (col > 9) {
            System.out.println();  // 改行
            printTable(row + 1, 1);  // 次の行へ再帰
            return;
        }
        
        // 九九の計算結果を出力
        System.out.printf("%d*%d=%2d ", row, col, row * col);
        
        // 同じ行の次の列へ再帰
        printTable(row, col + 1);
    }
}
