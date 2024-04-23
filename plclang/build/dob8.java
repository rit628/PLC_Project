public class Main {

    final int num = 2;

    public static void main(String[] args) {
        System.exit(new Main().main());
    }

    int main() {
        switch (num) {
            case 1:
                System.out.println("num is 1.");
                break;
            case 2:
                System.out.println("num is 2.");
                break;
            case 3:
                break;
            default:
                System.out.println("num is greater than 2.");
        }
        if (num > 1) {
            System.out.println(Math.pow(num, 99));
        }
    }

}