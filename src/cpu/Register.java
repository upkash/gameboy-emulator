package cpu;

public class Register {
    int value;

    public Register () {
        this.value = 0;
    }
    public Register (int value) {
        this.value = value;
    }
    public int read() {
        return value;
    }

    public void set(int val) {
        value = val;
    }

    public void increment() {
        this.value++;
    }

    public void decrement() {
        this.value--;
    }
}
