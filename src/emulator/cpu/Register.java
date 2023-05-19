package emulator.cpu;

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

    public int increment() {
        this.value++;
        this.value &= 0xFF;
        return this.value;
    }

    public int decrement() {
        this.value--;
        this.value &= 0xFF;
        return this.value;
    }
}
