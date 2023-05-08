package cpu;

public class Flag extends Register {
    public int get_zero() {
        return super.value >> 7;
    }

    public int get_operation() {
        return super.value >> 6;
    }

    public int get_half_carry() {
        return super.value >> 4;
    }

    public int get_carry() {
        return super.value >> 5;
    }

    public void set_zero(int val) {
        if (val == 1) {
            super.value |= 0x80;
        } else {
            super.value &= 0x70;
        }
    }

    public void set_operation(int val) {
        if (val == 1) {
            super.value |= 0x40;
        } else {
            super.value &= 0xB0;
        }
    }

    public void set_half_carry(int val) {
        if (val == 1) {
            super.value |= 0x20;
        } else {
            super.value &= 0xD0;
        }
    }

    public void set_carry(int val) {
        if (val == 1) {
            super.value |= 0x10;
        } else {
            super.value &= 0xE0;
        }
    }
}
