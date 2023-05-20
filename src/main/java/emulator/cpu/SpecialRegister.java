package emulator.cpu;

public class SpecialRegister extends Register{
    public SpecialRegister(int value) {
        super(value);
    }

    @Override
    public int increment() {
        this.value++;
        this.value &= 0xFFFF;
        return this.value;
    }

    @Override
    public int decrement() {
        this.value--;
        this.value &= 0xFFFF;
        return this.value;
    }
}
