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

    public int increment() {
        return ++this.value;
    }

    public int decrement() {
        return --this.value;
    }

    public void mask16() {
        this.value &= 0xFFFF;
    }
}
