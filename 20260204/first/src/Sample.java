public class Sample {
    public static void main(String[] args) throws Exception {
        int a;
        int b;
        int c;

        a = 10;
        b = 20;
        
        c = a;//a→c
        a = b;//b→a
        b = c;//c→b

        System.out.println("a:" + a);
        System.out.println("b:" + b);
        System.out.println("c:" + c);

    }
}
