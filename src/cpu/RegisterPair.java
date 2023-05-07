package cpu;

public class RegisterPair {
    private Register reg1;
    private Register reg2;
    public RegisterPair (Register r1, Register r2) {
        this.reg1 = r1;
        this.reg2 = r2;
    }

    public int read() {
        return reg1.read() + reg2.read();
    }

    public void set(int val) {
        reg1.set(reg1.read() & 0xf0);
        reg2.set(reg2.read() & 0x0f);
    }
}
