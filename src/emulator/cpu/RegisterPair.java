package emulator.cpu;

public class RegisterPair {
    private final Register reg1;
    private final Register reg2;
    public RegisterPair (Register r1, Register r2) {
        this.reg1 = r1;
        this.reg2 = r2;
    }

    public int read() {
        return ((reg1.read() << 8) + reg2.read()) & 0xFFFF;
    }

    public void set(int val) {
        reg1.set((val >> 8) & 0xFF);
        reg2.set(val & 0x00FF);
    }

    public void increment() {
        set((read()+1) & 0xFFFF);
    }

    public void decrement() {
        set((read()-1) & 0xFFFF);
    }
}
