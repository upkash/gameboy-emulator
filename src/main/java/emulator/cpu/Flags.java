package emulator.cpu;

public class Flags extends Register {
    public Flags(int value) {
        super.value = value;
    }

    public int getZero() {
        return (super.value >> 7) & 0x01;
    }

    public int getOperation() {
        return (super.value >> 6) & 0x01;
    }

    public int getHalfCarry() {
        return (super.value >> 5) & 0x01;
    }

    public int getCarry() {
        return (super.value >> 4) & 0x01;
    }

    public void setZero(int val) {
        if (val == 1) {
            super.value |= 0x80;
        } else {
            super.value &= 0x70;
        }
    }

    public void setOperation(int val) {
        if (val == 1) {
            super.value |= 0x40;
        } else {
            super.value &= 0xB0;
        }
    }

    public void setHalfCarry(int val) {
        if (val == 1) {
            super.value |= 0x20;
        } else {
            super.value &= 0xD0;
        }
    }

    public void setCarry(int val) {
        if (val == 1) {
            super.value |= 0x10;
        } else {
            super.value &= 0xE0;
        }
    }
}
