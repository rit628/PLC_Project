public class Main {

    int x;
    double y;
    String z;
    final Object immute = 3;
    Object[] ls = {1, "kmp", 'c', 9.0};

    public static void main(String[] args) {
        System.exit(new Main().main());
    }

    int main() {
        System.out.println(Math.pow(2, 12) + 1);
        System.out.println(Math.pow(2, (12 + 1)));
        System.out.println(Math.pow((2 + 1), 12));
    }

}