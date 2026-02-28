public class First {
    public static void main(String[] args) throws Exception {
        //int型しか扱わない配列型変数arrayを宣言
        //3つのint型の値を扱える配列を作って代入する
        int[] array = new int[3];
        //arrayの0番目に10,1番目に20,2番目に30を代入する
        array[0] = 10;
        array[1] = 20;
        array[2] = 30;

        //この長かったコードを以下にまとめられるよ
        //3つのint型の値を扱える配列を作って代入する
        int[] array2 = {10,20,30,40,50};

        //int型変数numを宣言し、arrayのi番目を代入する
        //初期化、条件、増減
        for (int i = 0; i < array2.length; i++) {
            int num = array2[i];
            System.out.println(num);

        }
    }
}
