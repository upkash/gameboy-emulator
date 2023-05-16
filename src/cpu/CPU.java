package cpu;

import memory.MMU;
import static java.lang.Math.abs;

public class CPU implements Runnable {

    // GP registers, stack pointers, prog. counter, register pairings
    private final Register A;
    private final Register B;
    private final Register C;
    private final Register D;
    private final Register E;
    private final Register H;
    private final Register L;
    private final SpecialRegister sp;
    private final SpecialRegister pc;
    private final RegisterPair BC;
    private final RegisterPair DE;
    private final RegisterPair HL;
    public String testOutput = "";

    public static final int[] op_cycles = {
            // 1  2  3  4  5  6  7  8  9  A  B  C  D  E  F
            1, 3, 2, 2, 1, 1, 2, 1, 5, 2, 2, 2, 1, 1, 2, 1, // 0
            0, 3, 2, 2, 1, 1, 2, 1, 3, 2, 2, 2, 1, 1, 2, 1, // 1
            2, 3, 2, 2, 1, 1, 2, 1, 2, 2, 2, 2, 1, 1, 2, 1, // 2
            2, 3, 2, 2, 3, 3, 3, 1, 2, 2, 2, 2, 1, 1, 2, 1, // 3
            1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, // 4
            1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, // 5
            1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, // 6
            2, 2, 2, 2, 2, 2, 0, 2, 1, 1, 1, 1, 1, 1, 2, 1, // 7
            1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, // 8
            1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, // 9
            1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, // A
            1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, // B
            2, 3, 3, 4, 3, 4, 2, 4, 2, 4, 3, 0, 3, 6, 2, 4, // C
            2, 3, 3, 0, 3, 4, 2, 4, 2, 4, 3, 0, 3, 0, 2, 4, // D
            3, 3, 2, 0, 0, 4, 2, 4, 4, 1, 4, 0, 0, 0, 2, 4, // E
            3, 3, 2, 1, 0, 4, 2, 4, 3, 2, 4, 1, 0, 0, 2, 4, // F
    };

    // flags
    private final Flags F;

    // memory
    private final MMU mmu;

    public boolean stop;
    private boolean halt;

    public CPU (MMU mmu) {
        A = new Register(0x01);
        B  = new Register(0x00);
        C = new Register(0x13);
        D = new Register(0x00);
        E = new Register(0xD8);
        H = new Register(0x01);
        L = new Register(0x4D);
        F = new Flags(0xB0);
        sp = new SpecialRegister(0xFFFE);
        pc = new SpecialRegister(0x0100);
        BC = new RegisterPair(B, C);
        DE = new RegisterPair(D, E);
        HL = new RegisterPair(H, L);
        stop = false;
        halt = false;
        this.mmu = mmu;
    }
    private void add(Register dst, Register src, int carry){
        int val = dst.read() + src.read() + carry;
        F.setZero(((val & 0xFF) == 0) ? 1 : 0);
        F.setOperation(0);
        F.setCarry((val > 255) ? 1 : 0);
        if (halfCarryAdd8(dst.read(), src.read()) == 1 ||
                halfCarryAdd8(dst.read(), carry) == 1 ||
                halfCarryAdd8(carry, src.read()) == 1 ||
                halfCarryAdd8(dst.read() + carry, src.read()) == 1) {
            F.setHalfCarry(1);
        } else F.setHalfCarry(0);
        dst.set(val & 0xFF);
    }

    private void add(Register dst, int value, int carry){
        int val = dst.read() + value + carry;
        F.setZero(((val & 0xFF) == 0) ? 1 : 0);
        F.setOperation(0);
        F.setCarry((val > 255) ? 1 : 0);
        if (halfCarryAdd8(dst.read(), value) == 1 ||
                halfCarryAdd8(dst.read(), carry) == 1 ||
                halfCarryAdd8(carry, value) == 1 ||
                halfCarryAdd8(dst.read() + carry, value) == 1) {
            F.setHalfCarry(1);
        } else F.setHalfCarry(0);
        dst.set(val & 0xFF);
    }

    private void add(RegisterPair dst, RegisterPair src) {
        int val = dst.read() + src.read();
        F.setOperation(0);
        F.setCarry(val > 0xFFFF ? 1 : 0);
        F.setHalfCarry(halfCarryAdd16(dst.read(),src.read()));
        dst.set(val);
    }

    private void add(RegisterPair dst, Register src) {
        int val = dst.read() + src.read();
        F.setOperation(0);
        F.setCarry(val > 0xFFFF ? 1 : 0);
        dst.set(val);
    }

    private void addInd(Register dst, RegisterPair src, int carry){
        int read = mmu.readByte(src.read());
        int val = dst.read() + read + carry;
        F.setZero((val == 0) ? 0 : 1);
        F.setOperation(0);
        F.setCarry((val > 255) ? 1 : 0);
        F.setHalfCarry(halfCarryAdd8(dst.read(), read));
        dst.set(val & 255);
    }

    private void sub(Register dst, Register src, int carry){
        int val = dst.read() - src.read() - carry;
        F.setZero(((val & 0xff) == 0) ? 1 : 0);
        F.setOperation(1);
        F.setCarry((val < 0) ? 1 : 0);

        if (carry == 0) F.setHalfCarry(halfCarrySub8(dst.read(), src.read()));

        else if (src.read() == 0) F.setHalfCarry(halfCarrySub8(dst.read(), carry));
        else if (dst.read() == 0) F.setHalfCarry(1);
        else {
            if (halfCarrySub8(dst.read()- src.read(), carry) == 1 || halfCarrySub8(dst.read() , src.read()) == 1) {
                F.setHalfCarry(1);
            } else {
                F.setHalfCarry(0);
            }
        }
        dst.set(0xFF & val);
    }

    private void sub(Register dst, int value, int carry){
        int val = dst.read() - value - carry;
        F.setZero(((val & 0xFF)== 0) ? 1 : 0);
        F.setOperation(1);
        F.setCarry((val < 0) ? 1 : 0);
        if (carry == 0) F.setHalfCarry(halfCarrySub8(dst.read(), value));
        else if (value == 0) F.setHalfCarry(halfCarrySub8(dst.read(), carry));
        else if (dst.read() == 0) F.setHalfCarry(1);
        else {
            if (halfCarrySub8(dst.read()- value, carry) == 1 || halfCarrySub8(dst.read() , value) == 1) {
                F.setHalfCarry(1);
            } else {
                F.setHalfCarry(0);
            }
        }
        dst.set(val & 255);
    }

    private void subInd(Register dst, RegisterPair src, int carry){
        int read = mmu.readByte(src.read());
        int val = dst.read() - read - carry;
        F.setZero((val == 0) ? 0 : 1);
        F.setOperation(1);
        F.setCarry((val < 0) ? 1 : 0);
        F.setHalfCarry(halfCarrySub8(dst.read(), read));
        dst.set(abs(val));
    }

    private void load(Register dst, Register src) {
        dst.set(src.read());
    }

    private void loadImm8(Register dst, int val) {
        dst.set(val);
    }

    private void loadImm16(RegisterPair dst, int val) {
        dst.set(val);
    }

    private void loadImm16(int val, Register src) {
        mmu.writeWord(val, src.read());
    }

    private void loadDstInd(RegisterPair dst, Register src) {
        mmu.writeByte(dst.read(), src.read());
    }

    private void loadDstInd(int location, Register src) {
        mmu.writeByte(location, src.read());
    }

    private void loadSrcInd(Register dst, RegisterPair src) {
        dst.set(mmu.readByte(src.read()));
    }

    private void inc(Register dst) {
        F.setOperation(0);
        F.setHalfCarry(halfCarryAdd8(dst.read(), 1));
        F.setZero(dst.increment() == 0 ? 1 : 0);
    }

    private void inc(RegisterPair dst) {
        dst.increment();
    }

    private void incInd(RegisterPair dst) {
        F.setOperation(0);
        int val = mmu.readByte(dst.read());
        F.setHalfCarry(halfCarryAdd8(val, 1));
        val++;
        mmu.writeByte(dst.read(), val);
        F.setZero(val == 0 ? 1 : 0);
    }

    private void decInd(RegisterPair dst) {
        F.setOperation(1);
        int val = mmu.readByte(dst.read());
        F.setHalfCarry(halfCarrySub8(val, 1));
        val--;
        mmu.writeByte(dst.read(), val);
        F.setZero(val == 0 ? 1 : 0);
    }

    private void dec(RegisterPair dst) {
        dst.decrement();
    }

    private void dec(Register dst) {
        F.setOperation(1);
        F.setHalfCarry(halfCarrySub8(dst.read(), 1));
        F.setZero(dst.decrement() == 0 ? 1 : 0);
    }

    private void and(Register dst, Register src) {
        int val = dst.read() & src.read();
        F.setOperation(0);
        F.setZero(val == 0 ? 1 : 0);
        F.setCarry(0);
        F.setHalfCarry(1);
        dst.set(val & 0xFF);
    }

    private void and(Register dst, int value) {
        int val = dst.read() & value;
        F.setOperation(0);
        F.setZero(val == 0 ? 1 : 0);
        F.setCarry(0);
        F.setHalfCarry(1);
        dst.set(val & 0xFF);
    }

    private void andInd(Register dst, RegisterPair src) {
        int val = dst.read() & mmu.readByte(src.read());
        F.setOperation(0);
        F.setZero(val == 0 ? 1 : 0);
        F.setCarry(val > 0xFF ? 1 : 0);
        F.setHalfCarry(1);
        dst.set(dst.read() & mmu.readByte(src.read()));
    }

    private void or(Register dst, Register src) {
        int val = dst.read() | src.read();
        F.setOperation(0);
        F.setZero(val == 0 ? 1 : 0);
        F.setCarry(0);
        F.setHalfCarry(0);
        dst.set(val & 0xFF);
    }

    private void or(Register dst, int value) {
        int val = dst.read() | value;
        F.setOperation(0);
        F.setZero(val == 0 ? 1 : 0);
        F.setCarry(0);
        F.setHalfCarry(0);
        dst.set(val & 0xFF);
    }

    private void orInd(Register dst, RegisterPair src) {
        int val = dst.read() | mmu.readByte(src.read());
        F.setOperation(0);
        F.setZero(val == 0 ? 1 : 0);
        F.setCarry(0);
        F.setHalfCarry(0);
        dst.set(val & 0xFF);
    }

    private void xor(Register dst, Register src) {
        int val = dst.read() ^ src.read();
        F.setOperation(0);
        F.setZero((val & 0xFF) == 0 ? 1 : 0);
        F.setCarry(0);
        F.setHalfCarry(0);
        dst.set(val & 0xFF);
    }

    private void xor(Register dst, int value) {
        int val = dst.read() ^ value;
        F.setOperation(0);
        F.setZero(val == 0 ? 1 : 0);
        F.setCarry(0);
        F.setHalfCarry(0);
        dst.set(val & 0xFF);
    }

    private void xorInd(Register dst, RegisterPair src) {
        int val = dst.read() ^ mmu.readByte(src.read());
        F.setOperation(0);
        F.setZero(val == 0 ? 1 : 0);
        F.setCarry(0);
        F.setHalfCarry(0);
        dst.set(val & 0xFF);
    }

    private void cp(Register dst, Register src) {
        int val = dst.read() - src.read();
        F.setZero((val == 0) ? 1 : 0);
        F.setOperation(1);
        F.setCarry((val < 0) ? 1 : 0);
        F.setHalfCarry(halfCarrySub8(dst.read(), src.read()));
    }

    private void cp(Register dst, int value) {
        int val = dst.read() - value;
        F.setZero((val == 0) ? 1 : 0);
        F.setOperation(1);
        F.setCarry((val < 0) ? 1 : 0);
        F.setHalfCarry(halfCarrySub8(dst.read(), value));
    }

    private void cpInd(Register dst, RegisterPair src) {
        int val = dst.read() - mmu.readByte(src.read());
        F.setZero((val == 0) ? 1 : 0);
        F.setOperation(1);
        F.setCarry((val < 0) ? 1 : 0);
        F.setHalfCarry(halfCarrySub8(dst.read(), src.read()));
    }

    private void rlc(Register dst) {
        F.setCarry(dst.read() >> 7);
        int value = Integer.rotateLeft(dst.read(), 1);
        value |= F.getCarry();
        value &= 0xff;
        F.setZero(value == 0 ? 1 : 0);
        dst.set(value);

    }

    private void rlc(RegisterPair dst) {
        int value = Integer.rotateLeft(mmu.readByte(dst.read()), 1);
        F.setCarry(value >> 7);
        value |= value >> 7;
        F.setZero(value == 0 ? 1 : 0);
        dst.set(value);
    }

    private void rl(Register dst) {
        int prev = F.getCarry();
        F.setCarry(dst.read() >> 7);
        int value = Integer.rotateLeft(dst.read(), 1);
        value |= prev;
        value &= 0xff;
        F.setZero(value == 0 ? 1 : 0);
        dst.set(value & 0xFF);
    }

    private void rl(RegisterPair dst) {
        int value = Integer.rotateLeft(mmu.readByte(dst.read()), 1);
        value |= F.getCarry();
        F.setCarry(value >> 7);
        value &= 0xffff;
        F.setZero(value == 0 ? 1 : 0);
        dst.set(value);
    }

    private void sla(Register dst) {
        F.setCarry(dst.read() >> 7);
        int value = dst.read() << 1;
        value &= 0xFE;
        value &= 0xff;
        F.setZero(value == 0 ? 1 : 0);
        dst.set(value);
    }

    private void sla(RegisterPair dst) {
        int value = mmu.readByte(dst.read()) << 1;
        F.setCarry(value >> 7);
        value &= 0xFE;
        value &= 0xff;
        F.setZero(value == 0 ? 1 : 0);
        dst.set(value);
    }

    private void swap(Register dst) {
        int r = dst.read();
        int value = ((r << 4) | (r >> 4)) & 0xFF;
        dst.set(value);
        F.setZero(value == 0 ? 1 : 0);
        F.setOperation(0);
        F.setHalfCarry(0);
        F.setCarry(0);
    }

    private void swap(RegisterPair dst) {
        int r = mmu.readByte(dst.read());
        int value = ((r << 4) | (r >> 4)) & 0xFF;
        mmu.writeByte(HL.read(), value);
        F.setZero(value == 0 ? 1 : 0);
        F.setOperation(0);
        F.setHalfCarry(0);
        F.setCarry(0);
    }

    private void rrc(Register dst) {
        F.setCarry(dst.read() & 0x01);
        int value = Integer.rotateRight(dst.read(), 1);
        value |= F.getCarry() << 7;
        value &= 0xff;
        F.setZero(value == 0 ? 1 : 0);
        dst.set(value & 0xFF);
    }

    private void rrc(RegisterPair dst) {
        int value = Integer.rotateRight(mmu.readByte(dst.read()), 1);
        F.setCarry(value & 0x01);
        value &= value << 7;
        value &= 0xff;
        F.setZero(value == 0 ? 1 : 0);
        dst.set(value);
    }

    private void rr(Register dst) {
        int value = Integer.rotateRight(dst.read(), 1);
        value &= 0xFF;
        value |= (F.getCarry() << 7);
        F.setCarry(dst.read() & 0x01);
        F.setZero((value == 0) ? 1 : 0);
        F.setOperation(0);
        F.setHalfCarry(0);
        dst.set(value);
    }

    private void rr(RegisterPair dst) {
        int value = Integer.rotateRight(mmu.readByte(dst.read()), 1);
        value |= F.getCarry() << 7;
        F.setCarry(value & 0x01);

        dst.set(value);
    }

    private void srl(Register dst) {
        F.setCarry(dst.read() & 0x01);
        int value = dst.read() >> 1;
        value &= 0x7F;
        F.setZero(value == 0 ? 1 : 0);
        F.setOperation(0);
        F.setHalfCarry(0);
        dst.set(value);
    }

    private void srl(RegisterPair dst) {
        int value = mmu.readByte(dst.read()) >> 1;
        F.setCarry(value & 0x01);
        value &= 0x7F;
        dst.set(value);
    }

    private void sra(Register dst) {
        F.setCarry(dst.read() & 0x01);
        int value = dst.read() >> 1;
        int prev = dst.read() & 0x80;
        value |= prev;
        F.setZero(((value & 0xFF) == 0 ? 1 : 0));
        dst.set(value);
    }

    private void sra(RegisterPair dst) {
        int value = mmu.readByte(dst.read()) >> 1;
        int prev = dst.read() & 0x80;
        F.setCarry(value & 0x01);
        value |= prev;
        dst.set(value);
    }

    private void bit(Register reg, int n) {
        F.setHalfCarry(1);
        F.setOperation(0);
        F.setZero(((reg.read()>>n) & 0x01) == 0 ? 1 : 0);
    }

    private void bit(RegisterPair reg, int n) {
        F.setHalfCarry(1);
        F.setOperation(0);
        F.setZero((((mmu.readByte(reg.read())>>n) & 0x01) == 0) ? 1 : 0);
    }

    private void set(Register reg, int n) {
        reg.set(reg.read() | (0x01 << n));
    }

    private void set(RegisterPair reg, int n) {
        reg.set(mmu.readByte(reg.read()) | (0x01 << n));
    }

    private void res(Register reg, int n) {
        reg.set(reg.read() & ~(0x1 << n));
    }

    private void res(RegisterPair reg, int n) {
        reg.set(mmu.readByte(reg.read()) & ~(0x1 << n));
    }

    private Register getRegister(int reg) {
        if (reg == 0) return B;
        else if (reg == 1) return C;
        else if (reg == 2) return D;
        else if (reg == 3) return E;
        else if (reg == 4) return H;
        else if (reg == 5) return L;
        else if (reg == 7) {
            return A;
        }
        return null;
    }

    private void pop() {
        int value = mmu.readByte(sp.read());
        sp.increment();
        value |= mmu.readByte(sp.read()) << 8;
        sp.increment();
        pc.set(value-1);
    }

    private void pop(Register reg) {
        reg.set(mmu.readByte(sp.read()));
        sp.increment();
    }



    private void push(RegisterPair reg_pair) {
        sp.decrement();
        mmu.writeByte(sp.read(), reg_pair.read() >> 8);
        sp.decrement();
        mmu.writeByte(sp.read(), reg_pair.read() & 0x00FF);
    }

    private void push(SpecialRegister reg) {
        sp.decrement();
        mmu.writeByte(sp.read(), reg.read() >> 8);
        sp.decrement();
        mmu.writeByte(sp.read(), reg.read() & 0x00FF);
    }

    private void push(int val) {
        sp.decrement();
        mmu.writeByte(sp.read(), val >> 8);
        sp.decrement();
        mmu.writeByte(sp.read(), val & 0x00FF);
    }

    private int halfCarryAdd8(int firstNum, int secondNum) {
        return (((firstNum & 0x0F) + (secondNum & 0x0F))) > 0x0F ? 1 : 0;
//        return (((first_num & 0x0F) + (second_num & 0x0F)) & 0x10) == 0x10 ? 1 : 0;
    }

    private int halfCarryAdd16(int firstNum, int secondNum) {
        return ((firstNum & 0x0fff) + (secondNum & 0x0fff)) > 0x0fff ? 1: 0;
    }


    private int halfCarrySub8(int firstNum, int secondNum) {
        return (firstNum & 0x0F) - (secondNum & 0x0F) < 0 ? 1 : 0;
    }

    private int getSignedInt(int n) {
        return (-256) | (n);
    }

    private void executeOpCode(int opCode) {
        // 0x [d1][d0]
        String out =    "A:" + String.format("%02X", A.read()) +
                        " F:" + String.format("%02X", F.read()) +
                        " B:" + String.format("%02X", B.read()) +
                        " C:" + String.format("%02X", C.read()) +
                        " D:" + String.format("%02X", D.read()) +
                        " E:" + String.format("%02X", E.read()) +
                        " H:" + String.format("%02X", H.read()) +
                        " L:" + String.format("%02X", L.read()) +
                        " SP:" + String.format("%04X", sp.read()) +
                        " PC:" + String.format("%04X", pc.read()) +
                        " PCMEM:"+
                            String.format("%02X",opCode) + "," +
                            String.format("%02X", mmu.readByte(pc.read()+1))+ "," +
                            String.format("%02X", mmu.readByte(pc.read()+2))+ "," +
                            String.format("%02X", mmu.readByte(pc.read()+3));
        System.out.println(out);
        int d1 = opCode >> 4;
        int d0 = opCode & 0x0F;
        if (opCode == 0x10) stop = true;
        else if (opCode == 0x76) halt = true;
        else if (opCode == 0x3A) {
            loadSrcInd(A, HL);
            HL.decrement();
        } else if (opCode == 0xF8) {
            pc.increment();
            int n = mmu.readByte(pc.read());
            int val;
            F.setCarry(((sp.read() & 0xFF) + (n & 0xFF)) > 0xFF ? 1 : 0);
            F.setHalfCarry(((sp.read() & 0xF) + (n & 0xF)) > 0xF ? 1 : 0);
            if (n > 128) n = getSignedInt(n);
            val = n + sp.read();
            F.setZero(0);
            F.setOperation(0);
            HL.set(val & 0xFFFF);
        } else if (opCode == 0xE8) {
            pc.increment();
            int n = mmu.readByte(pc.read());
            int val;
            F.setCarry(((sp.read() & 0xFF) + (n & 0xFF)) > 0xFF ? 1 : 0);
            F.setHalfCarry(((sp.read() & 0xF) + (n & 0xF)) > 0xF ? 1 : 0);
            if (n > 128) n = getSignedInt(n);
            val = n + sp.read();
            F.setZero(0);
            F.setOperation(0);
            sp.set(val & 0xFFFF);
        }
        else if (opCode == 0xF9) {
            sp.set(HL.read());
        } else if (opCode == 0xCB) {
            pc.increment();
            executeCB(mmu.readByte(pc.read()));
        } else if (opCode == 0x22) {
            loadDstInd(HL, A);
            HL.increment();
        } else if (opCode == 0x32) {
            loadDstInd(HL, A);
            HL.decrement();
        } else if (opCode == 0xE6) {
            pc.increment();
            int n = mmu.readByte(pc.read());
            and(A, n);
        } else if (opCode == 0xEE) {
            pc.increment();
            int n = mmu.readByte(pc.read());
            xor(A, n);
        } else if (opCode == 0xF6) {
            pc.increment();
            int n = mmu.readByte(pc.read());
            or(A, n);
        } else if (opCode == 0xFE) {
            pc.increment();
            int n = mmu.readByte(pc.read());
            cp(A, n);
        } else if (opCode == 0xC6 || opCode == 0xCE) {
            pc.increment();
            int n = mmu.readByte(pc.read());
            add(A, n, (opCode == 0xCE && F.getCarry() == 1) ? 1 : 0);
        } else if (opCode == 0xD6 || opCode == 0xDE) {
            pc.increment();
            int n = mmu.readByte(pc.read());
            sub(A, n, (opCode == 0xDE && F.getCarry() == 1) ? 1: 0);
        } else if (d1 == 0x0B && d0 >= 0x08) {
            if (d0 == 0x0E) {
                cpInd(A, HL);
            } else {
                Register dst = getRegister(0b00000111 & opCode);
                assert dst != null;
                cp(A, dst);
            }
        } else if (opCode == 0xFA) {
            int nn = mmu.readWord(pc.read()+1);
            int val = mmu.readByte(nn);
            loadImm8(A, val);
            pc.increment();
            pc.increment();
        } else if (opCode == 0xE9) {
            pc.set(HL.read()-1);
        } else if (opCode == 0xE0) {
            loadDstInd(mmu.readByte(pc.read()+1) + 0xFF00, A);
            pc.increment();
        } else if (opCode == 0xEA) {
            // load (nn), A
            loadDstInd(mmu.readWord(pc.read()+1), A);
            pc.increment();
            pc.increment();
        } else if (opCode == 0xF0) {
            int addr = mmu.readByte(pc.read()+1) + 0xff00;
            int val = mmu.readByte(addr);
            A.set(val);
            pc.increment();
        } else if (d1 == 0x07 && d0 <= 0x07) {
            Register reg = getRegister(d0);
            assert reg != null;
            loadDstInd(HL, reg);
        } else if (opCode >= 0x40 && opCode < 0x80) {
            // ld
            int src = opCode & 0b00000111;
            int dst = (opCode & 0b00111000) >> 3;
            Register srcReg = getRegister(opCode & 0b00000111);
            Register dstReg = getRegister((opCode & 0b00111000) >> 3);
            if (src == 0x06) {
                assert dstReg != null;
                loadSrcInd(dstReg, HL);
            } else if (dst == 0x06) {
                assert srcReg != null;
                loadDstInd(HL, srcReg);
            } else {
                assert dstReg != null;
                assert srcReg != null;
                load(dstReg, srcReg);
            }

        } else if (opCode >= 0x80 && opCode < 0x90) {
            // add & adc
            if (d0 == 0x06 || d0 == 0x0E) {
                addInd(A, HL, (d0 == 0x0E && F.getCarry() == 1) ? 1 : 0);
            } else {
                int dst = opCode & 0b00000111;
                Register srcReg = getRegister(dst);
                assert srcReg != null;
                add(A, srcReg, (opCode > 0x87 && F.getCarry() == 1) ? 1 : 0);
            }
        } else if (opCode >= 0x90 && opCode < 0xA0) {
            // sub & sbc
            if (d0 == 0x06 || d0 == 0x0E) {
                subInd(A, HL, (d0 == 0x0E && F.getCarry() == 1) ? 1 : 0);
            } else {
                int dst = opCode & 0b00000111;
                Register dstReg = getRegister(dst);
                assert dstReg != null;
                sub(A, dstReg, (opCode > 0x97 && F.getCarry() == 1) ? 1 : 0);
            }
        } else if (opCode >= 0xA0 && opCode < 0xA8) {
            // and
            if (opCode == 0xA6) {
                // mem
                andInd(A, HL);
            } else {
                Register dstReg = getRegister(opCode & 0b00000111);
                assert dstReg != null;
                and(A, dstReg);
            }
        } else if (opCode >= 0xA8 && opCode < 0xB0) {
            // xor
            if (opCode == 0xAe) {
                // mem
                xorInd(A, HL);
            } else {
                Register dstReg = getRegister(opCode & 0b00000111);
                assert dstReg != null;
                xor(A, dstReg);
            }
        } else if (opCode >= 0xB0 && opCode < 0xB8) {
            // or
            if (opCode == 0xB6) {
                // mem
                orInd(A, HL);
            } else {
                Register dstReg = getRegister(opCode & 0b00000111);
                assert dstReg != null;
                or(A, dstReg);
            }
        } else if (opCode >= 0xB8 && opCode < 0xC0 ) {
            // cp
            Register dst = getRegister(opCode & 0x07);
            if (dst == null) cpInd(A, HL);
            else cp(A,dst);
        } else if (opCode == 0xC0) {
            // RET NZ
            if (F.getZero() == 0) pop();
        } else if (opCode == 0xC8) {
            // RET Z
            if (F.getZero() != 0) pop();
        } else if (opCode == 0xD0) {
            // RET NC
            if (F.getCarry() == 0) pop();
        } else if (opCode == 0xD8) {
            // RET C
            if (F.getCarry() != 0) pop();
        } else if (opCode == 0xC9) {
            // RET
           pop();
        } else if (opCode == 0xD9) {
            // RETI
            int low = mmu.readByte(sp.read());
            sp.increment();
            int hi = mmu.readByte(sp.read());
            sp.increment();
            int n = hi << 8;
            n |= low;
            pc.set(n-1);
        } else if (d1 >= 0xC && (d0 == 0x0F || d0 == 0x07)) {
            // RST
            pc.increment();
            push(pc);
            if (opCode == 0xC7) {
                pc.set(0x00);
            } else if (opCode == 0xCF) {
                pc.set(0x08);
            } else if (opCode == 0xD7) {
                pc.set(0x10);
            } else if (opCode == 0xDF) {
                pc.set(0x18);
            } else if (opCode == 0xE7) {
                pc.set(0x20);
            } else if (opCode == 0xEF) {
                pc.set(0x28);
            } else if (opCode == 0xF7) {
                pc.set(0x30);
            } else if (opCode == 0xFF) {
                pc.set(0x38);
            }
            pc.decrement();
        } else if (d1 < 0x04 && ((d0 >= 3 && d0 < 6) || (d0 >= 0xB && d0 < 0xE))) {
            // INC or DEC

            if (d0 == 0x3) {
                if (d1 == 0x0) inc(BC);
                else if (d1 == 0x1) inc(DE);
                else if (d1 == 0x2) inc(HL);
                else if (d1 == 0x3) sp.increment();
            } else if (d0 == 0xB) {
                if (d1 == 0x0) dec(BC);
                else if (d1 == 0x1) dec(DE);
                else if (d1 == 0x2) dec(HL);
                else if (d1 == 0x3) sp.decrement();
            } else if (d0 == 0xD) {
                if (d1 == 0x0) dec(C);
                else if (d1 == 0x1) dec(E);
                else if (d1 == 0x2) dec(L);
                else if (d1 == 0x3) dec(A);
            } else if (d0 == 0x5) {
                if (d1 == 0x0) dec(B);
                else if (d1 == 0x1) dec(D);
                else if (d1 == 0x2) dec(H);
                else if (d1 == 0x3) decInd(HL);

            } else if (d0 == 0xC) {
                if (d1 == 0x0) inc(C);
                else if (d1 == 0x1) inc(E);
                else if (d1 == 0x2) inc(L);
                else if (d1 == 0x3) inc(A);
            } else {
                if (d1 == 0x0) inc(B);
                else if (d1 == 0x1) inc(D);
                else if (d1 == 0x2) inc(H);
                else if (d1 == 0x3) inc(HL);
            }
        } else if (opCode == 0x34) {
            incInd(HL);
        } else if (d1 < 0x04 && (d0 == 0x06 || d0 == 0x0E)) {
            // LD REG, n
            int n = mmu.readByte(pc.read()+1);
            Register dst = getRegister((opCode & 0b00111000) >> 3);
            if (dst == null) {
                mmu.writeByte(HL.read(), n);
            } else {
                loadImm8(dst, n);
            }
            pc.increment();
        } else if (d1 < 0x04 && d0 == 0x01) {
            // LD REG PAIR, nn
            int nn = mmu.readWord(pc.read()+1);
            if (d1 == 0x00) loadImm16(BC, nn);
            else if (d1 == 0x01) loadImm16(DE, nn);
            else if (d1 == 0x02) loadImm16(HL, nn);
            else sp.set(nn);
            pc.increment();
            pc.increment();
        } else if (d1 < 0x04 && d0 == 0x09) {
            // ADD REG PAIR, REG PAIR
            if (d1 == 0x00) {
                add(HL, BC);
            } else if (d1 == 0x01) {
                add(HL, DE);
            } else if (d1 == 0x02) {
                add(HL, HL);
            } else {
                int hl = HL.read() & 0xFFFF;
                add(HL, sp);
                F.setHalfCarry(halfCarryAdd16(hl, sp.read() & 0xFFFF));
            }
        } else if (opCode == 0x02 || opCode == 0x12) {
            // LD (BC) or (DE), A
            if (opCode == 0x02) {
                loadDstInd(BC, A);
            } else {
                loadDstInd(DE, A);
            }
        } else if (opCode == 0x0A || opCode == 0x1A) {
            // LD A, (BC) or (DE)
            if (opCode == 0x0A) {
                loadSrcInd(A, BC);
            } else {
                loadSrcInd(A, DE);
            }
        } else if (d1 >= 0x0C && d0 == 0x01) {
            // POP
            if (d1 == 0x0C) {
                pop(C);
                pop(B);
            } else if (d1 == 0x0D) {
                pop(E);
                pop(D);
            } else if (d1 == 0x0E) {
                pop(L);
                pop(H);
            } else {
                // flag pop
                F.set(mmu.readByte(sp.read()));
                sp.increment();
                A.set(mmu.readByte(sp.read()));
                sp.increment();
            }
        } else if ((d1 >= 0x0C && d1 < 0x0E) && (d0 == 0x02 || d0 == 0x0A)) {
            // JP flag nn
            pc.increment();
            int nn = mmu.readWord(pc.read());
            if (opCode == 0xC2 && F.getZero() == 0) pc.set(nn-1);
            else if (opCode == 0xCA && F.getZero() == 1) pc.set(nn-1);
            else if (opCode == 0xD2 && F.getCarry() == 0) pc.set(nn-1);
            else if (opCode == 0xDA && F.getCarry() == 1) pc.set(nn-1);
            else pc.increment();
        } else if (opCode == 0xC3) {
            // JP nn
            int nn = mmu.readWord(pc.read()+1);
            pc.set(nn-1);
        } else if ((d1 >= 0x0C && d1 < 0x0E) && (d0 == 0x04 || d0 == 0x0C)) {
            // CALL flag, nn
            if ((opCode == 0xC4 && F.getZero() == 0) ||
                (opCode == 0xCC && F.getZero() == 1) ||
                (opCode == 0xD4 && F.getCarry() == 0) ||
                (opCode == 0xDC && F.getCarry() == 1)) {
                int nn = mmu.readWord(pc.read()+1);
                push(pc.read()+3);
                pc.set(nn-1);
            } else {
                pc.increment();
                pc.increment();
            }
        } else if (opCode == 0xCD) {
            // CALL nn
            int nn = mmu.readWord(pc.read()+1);
            push(pc.read()+3);
            pc.set(nn-1);
        } else if (d1 >= 0x0C && d0 == 0x05) {
            // PUSH
            if (d1 == 0x0C) {
                push(BC);
            } else if (d1 == 0x0D) {
                push(DE);
            } else if (d1 == 0x0E) {
                push(HL);
            } else {
                // flags
                sp.decrement();
                mmu.writeByte(sp.read(), A.read());
                sp.decrement();
                mmu.writeByte(sp.read(), F.read());
            }
        } else if (opCode == 0x08) {
            // LD (nn) SP
            int nn = mmu.readWord(pc.read()+1);
            loadImm16(nn, sp);
            pc.increment();
            pc.increment();
        } else if (opCode == 0x18) {
            // JR n
            int n = mmu.readByte(pc.read()+1);
            if (n > 128) {
                n = getSignedInt(n);
            }
            pc.set(pc.read() + n);
            pc.increment();
        } else if ((d1 >= 0x02 && d1 < 0x04) && (d0 == 0x00 || d0 == 0x08)) {
            // JR flag nn
            if ((opCode == 0x20 && F.getZero() == 0 )||
                (opCode == 0x28 && F.getZero() == 1) ||
                (opCode == 0x30 && F.getCarry() == 0) ||
                (opCode == 0x38 && F.getCarry() == 1) ) {
                int n = mmu.readByte(pc.read()+1);
                if (n > 128) {
                    n = getSignedInt(n);
                }
                pc.set(pc.read() + n);
            }
            pc.increment();
        } else if (opCode == 0x2A) {
            loadSrcInd(A, HL);
            HL.increment();
        } else if (opCode == 0x0F) {
            // rrc a
            rrc(A);
            F.setOperation(0);
            F.setZero(0);
            F.setHalfCarry(0);
        } else if (opCode == 0x1F) {
            // rr a
            rr(A);
            F.setOperation(0);
            F.setZero(0);
            F.setHalfCarry(0);
        } else if (opCode == 0x2F) {
            // cpl
            int val = ~A.read() & 0xFF;
            A.set(val);
            F.setHalfCarry(1);
            F.setOperation(1);
        } else if (opCode == 0x3F) {
            // ccf
            int val = F.getCarry() == 1 ? 0 : 1;
            F.setCarry(val);
            F.setOperation(0);
            F.setHalfCarry(0);
        } else if (opCode == 0x07) {
            // rlc a
            rlc(A);
            F.setOperation(0);
            F.setHalfCarry(0);
            F.setZero(0);
        } else if (opCode == 0x17) {
            // rl a
            rl(A);
            F.setOperation(0);
            F.setHalfCarry(0);
            F.setZero(0);
        } else if (opCode == 0x27) {
            // daa
        } else if (opCode == 0x37) {
            // scf
            F.setCarry(1);
            F.setOperation(0);
            F.setHalfCarry(0);
        }
    }

    private void executeCB(int arg) {
        int d1 = arg >> 4;
        int d0 = arg & 0x0F;
        Register reg = getRegister(d0 & 0x07);
        if (d1 <= 0x03) {
            F.setHalfCarry(0);
            F.setOperation(0);
        }
        if (d1 == 0x00) {
            //rotate (rlc)
            if (d0 < 0x08) {
                // left
                if (d0 == 0x06) {
                    // (HL)
                    rlc(HL);
                } else {
                    assert reg != null;
                    rlc(reg);
                    // b-a
                }
            } else {
                // right
                if (d0 == 0x0E) {
                    // (HL)
                    rrc(HL);
                } else {
                    // b-a
                    assert reg != null;
                    rrc(reg);
                }
            }
        } else if (d1 == 0x01) {
            //rotate (rl)
            if (d0 < 0x08) {
                // left
                if (d0 == 0x06) {
                    // (HL)
                    rl(HL);
                } else {
                    // b-a
                    assert reg != null;
                    rl(reg);
                }
            } else {
                // right
                if (d0 == 0x0E) {
                    // (HL)
                    rr(HL);
                } else {
                    // b-a
                    assert reg != null;
                    rr(reg);
                }
            }
        } else if (d1 == 0x02) {
            //shift (sll)
            if (d0 < 0x08) {
                // left
                if (d0 == 0x06) {
                    // (HL)
                    sla(HL);
                } else {
                    // b-a
                    assert reg != null;
                    sla(reg);
                }
            } else {
                // right
                if (d0 == 0x0E) {
                    // (HL)
                    sra(HL);
                } else {
                    // b-a
                    assert reg != null;
                    sra(reg);
                }
            }
        } else if (d1 == 0x03) {
            //shift (sll)
            if (d0 < 0x08) {
                // left
                if (d0 == 0x06) {
                    // (HL)
                    swap(HL);
                } else {
                    // b-a
                    assert reg != null;
                    swap(reg);
                }
            } else {
                // right
                if (d0 == 0x0E) {
                    // (HL)
                    srl(HL);
                } else {
                    // b-a
                    assert reg != null;
                    srl(reg);
                }
            }
        } else if (arg >= 0x40 && arg < 0x80) {
            int b = (arg & 0x38) >> 3;
            if (reg == null) {
                bit(HL, b);
            } else {
                bit(reg, b);
            }
        } else if (arg >= 0x80 && arg < 0xC0) {
            int b = (arg & 0x38) >> 3;
            if (reg == null) {
                res(HL, b);
            } else {
                res(reg, b);
            }
        } else if (arg >= 0xC0) {
            int b = (arg & 0x38) >> 3;
            if (reg == null) {
                set(HL, b);
            } else {
                set(reg, b);
            }
        }
    }

    @Override
    public void run() {
        while (!stop) {
            int currInstr = mmu.readByte(pc.read());
            executeOpCode(currInstr);
            pc.increment();
            if (mmu.readByte(0xff02) == 0x81) {
                testOutput += (char)mmu.readByte(0xff01);
//                System.out.println(testOutput);
                mmu.writeByte(0xff02, 0x00);
            }
        }
    }




    public static void main (String[] args) {
        MMU mmu = new MMU("/Users/utkarsh/IdeaProjects/GameBoyEmulator/src/cpu_instrs/individual/11-op a,(hl).gb");
        CPU cpu = new CPU(mmu);
        mmu.writeByte(0xFF44, 0x90);
        cpu.run();
    }
}
