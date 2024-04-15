public class Main {

    int x;
    int y = 10;

    public static void main(String[] args) {
        System.exit(new Main().main());
    }

    int main() {
        return x + y;
    }

}