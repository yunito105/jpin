public class Fourth {
    public static void main(String[] args) throws Exception {
        int a = 10;
        int b = (a++) + (++a) + (a++);
        System.out.println(b);
    }
}
