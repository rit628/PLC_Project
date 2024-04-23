public class Main {

    public static void main(String[] args) {
        System.exit(new Main().main());
    }

    int main() {
        int num = 1;
        while (num < 10) {
            System.out.println(num + "\n");
            System.out.println('\b');
            System.out.println('\n');
            System.out.println('\r');
            System.out.println('\t');
            System.out.println('\\');
            System.out.println('\'');
            System.out.println('\"');
            System.out.println("\b" + "\n" + "\r" + "\t" + "\\" + "\'" + "\"");
            num = num + 1;
        }
    }

}