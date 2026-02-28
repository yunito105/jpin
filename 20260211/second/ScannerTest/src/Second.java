import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;

public class Second {
    public static void main(String[] args) throws Exception {

        //Scannerを用意する
        Scanner scan = new Scanner(System.in);

        //テストの平均点を計算して出力してほしい
        //5人分なら５人分入力を受け付けて
        //平均点を出力してほしい。
        int[] testScore = new int[5];
        int sum = 0;
        int studentCnt = testScore.length;

        for (int i = 0; i < testScore.length; i++) {
            System.out.println(i+1 + "人目の入力をお願いします");
            testScore[i] = scan.nextInt();
        }

        for (int i = 0; i < testScore.length; i++) {
            sum = sum + testScore[i];
        }

        // for (int i = 0; i < testScore.length; i++) {
        //     System.out.println(i + 1 + "人目の入力をお願いします");
        //     sum = sum + scan.nextInt();
        // }

        System.out.println("テストの平均点は：" + sum / studentCnt);

        //int型を受け入れる配列変数型のtestScoreを宣言し、要素を5つ受け入れるint型を代入してください。
        //平均点の算出方法は、クラスにいる生徒数のテストの合計点/クラスの生徒数となります。
        //クラスの生徒数のテストの合計点をint型の変数sumを用いて表してください。
        //クラスの生徒数をint型の変数studentCountであらわしてください。

        //testScoreのインデックスに、CLIで入力したテストの点数を生徒数の数だけ代入します。
        //代入方法はFor分を用いてください。

        //testScoreに5人のテストの点数が格納されています。
        //2回目のfor分でtestScoreの中身を変数sumに代入しましょう。

        //最後にクラスの生徒数とクラスのテストの合計点数を用いて、平均点を出力してください。   




    }
}
