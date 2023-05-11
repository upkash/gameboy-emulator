package cpu;

public class SpecialRegister extends Register{
    public SpecialRegister(int value) {
        super(value);
    }

    @Override
    public int increment() {
        this.value++;
        return this.value & 0xFFFF;
    }

    @Override
    public int decrement() {
        this.value--;
        return this.value & 0xFFFF;
    }
}
