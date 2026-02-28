import java.util.Scanner;

public class Play {
    public static void main(String[] args) {
        // int型を受け入れる配列変数型のtestScoreを宣言し、要素を5つ受け入れるint型を代入してください。
        int[] testScore = new int[5];
        // 平均点の算出方法は、クラスにいる生徒数のテストの合計点/クラスの生徒数となります。
        // クラスの生徒数のテストの合計点をint型の変数sumを用いて表してください。
        int testScoreSum = 0;
        // クラスの生徒数をint型の変数studentCountであらわしてください。
        int studentsNum = testScore.length;

        //最高得点、最低得点を表示するために、変数を用意してください。
        int testMaxScore = Integer.MIN_VALUE;
        int testMinScore = Integer.MAX_VALUE; // 

        //ユーザーがCLIで入力するために、Scannerクラスを用意してください。
        Scanner scan = new Scanner(System.in);

        // 0 ~ 100点の間で入力してください。とメッセージを出す。
        System.out.println("0 ~ 100点の間で入力してください");

        // testScoreのインデックスに、CLIで入力したテストの点数を生徒数の数だけ代入します。
        // 代入方法はFor文を用いてください。
        for (int i = 0; i < testScore.length; i++) {
            System.out.print((i + 1) + "人目入力をお願いします。>");
            testScore[i] = scan.nextInt();
            testScoreSum = testScoreSum + testScore[i];            
            //System.out.println("ここまでの平均点は" + testScoreSum / (i + 1));

            if (testMaxScore < testScore[i]) {
                testMaxScore = testScore[i];
            }

            if (testMinScore > testScore[i]) {
                testMinScore = testScore[i];
            }
        }
        
        // testScoreに5人のテストの点数が格納されています。
        // 2回目のfor分でtestScoreの中身を変数sumに代入しましょう。

        // 最後にクラスの生徒数とクラスのテストの合計点数を用いて、平均点を出力してください。     
        System.out.println("*********************************"); 
        System.out.println("合計点数は" + testScoreSum + "でした");
        System.out.println("平均点は" + testScoreSum/studentsNum + "でした");       
        System.out.println("最高点は" + testMaxScore + "でした");
        System.out.println("最低点は" + testMinScore + "でした");
        System.out.println("*********************************");
    }

}
