package cpu;

import memory.MMU;

import static java.lang.Math.abs;

public class CPU {

    // GP registers, stack pointers, prog. counter, register pairings
    private final Register A;
    private final Register B;
    private final Register C;
    private final Register D;
    private final Register E;
    private final Register H;
    private final Register L;
    private final Register sp;
    private final Register pc;
    private final RegisterPair BC;
    private final RegisterPair DE;
    private final RegisterPair HL;

    // flags
    private boolean carry;
    private boolean half_carry;
    private boolean operation;
    private boolean zero;

    // memory
    private final MMU mmu;

    public CPU () {
        A = new Register();
        B  = new Register();
        C = new Register();
        D = new Register();
        E = new Register();
        H = new Register();
        L = new Register();
        sp = new Register();
        pc = new Register();
        BC = new RegisterPair(B, C);
        DE = new RegisterPair(D, E);
        HL = new RegisterPair(H, L);
        mmu = new MMU();
    }
    private void add(Register dst, Register src, int carry){
        int val = dst.read() + src.read() + carry;
        zero = val == 0;
        operation = false;
        this.carry = val > 255;
        dst.set(val & 255);
    }
    private void add_ind(Register dst, RegisterPair src, int carry){
        int val = dst.read() + mmu.readByte(src.read()) + carry;
        zero = val == 0;
        operation = false;
        this.carry = val > 255;
        dst.set(val & 255);
    }

    private void sub(Register dst, Register src, int carry){
        int val = dst.read() - src.read() - carry;
        zero = val == 0;
        operation = true;
        this.carry = val < 0;
        dst.set(abs(val));
    }

    private void sub_ind(Register dst, RegisterPair src, boolean carry){
    }

    private void load(Register dst, Register src) {
        dst.set(src.read());
    }


    private void load(RegisterPair dst, int loc) {
        dst.set(mmu.readWord(loc));
    }

    private void load(Register dst, int loc) {
        dst.set(mmu.readByte(loc));
    }

    private void load_dst_ind(RegisterPair dst, Register src) {
        mmu.writeByte(src.read(), dst.read());
    }

    private void load_src_ind(Register dst, RegisterPair src) {
        dst.set(mmu.readByte(src.read()));
    }

    private void inc(Register dst) {
        dst.increment();
    }

    private void dec(Register dst) {
        dst.decrement();
    }

    private void and(Register dst, Register src) {
        dst.set(dst.read() & src.read());
    }

    private void or(Register dst, Register src) {
        dst.set(dst.read() | src.read());
    }

    private void xor(Register dst, Register src) {
        dst.set(dst.read() ^ src.read());
    }

    private Register get_register(int reg) {
        if (reg == 0) return B;
        else if (reg == 1) return C;
        else if (reg == 2) return D;
        else if (reg == 3) return E;
        else if (reg == 4) return H;
        else if (reg == 5) return L;
        return A;
    }

    private void pop(RegisterPair reg_pair) {
        reg_pair.set(mmu.readWord(reg_pair.read()));
        sp.decrement();
        sp.decrement();
    }

    private void push(RegisterPair reg_pair) {
        mmu.writeWord(sp.read(), (byte) reg_pair.read());
        sp.increment();
        sp.increment();
    }

    private void execute_op_code(int op_code) {
        // 0x [d1][d0]
        int d1 = op_code >> 4;
        int d0 = op_code & 0x0F;
        if (op_code == 0x00) {
        }
        else if (op_code == 0x76) {
        } else if (op_code >= 0x40 && op_code < 0x80) {
            // ld
            int src = op_code & 0b00000111;
            int dst = (op_code & 0b00111000) >> 3;

            if (src == 0x06) {
                load_src_ind(get_register(dst), HL);
            } else {
                load(get_register(dst), get_register(src));
            }

        } else if (op_code >= 0x80 && op_code < 0x90) {
            // add & adc
            int dst = op_code & 0b00000111;
            add(A, get_register(dst), (op_code > 0x87 && carry) ? 1 : 0);
        } else if (op_code >= 0x90 && op_code < 0xA0) {
            // sub & sbc
            int dst = op_code & 0b00000111;
            sub(A, get_register(dst), (op_code > 0x97 && carry) ? 1 : 0);
        } else if (op_code >= 0xA0 && op_code < 0xA8) {
            // and
            if (op_code == 0xA7) {
                // mem
            } else {
                and(A, get_register(op_code & 0b00000111));
            }
        } else if (op_code >= 0xA9 && op_code < 0xB0) {
            // xor
            if (op_code == 0xAF) {
                // mem
            } else {
                xor(A, get_register(op_code & 0b00000111));
            }
        } else if (op_code >= 0xB0 && op_code < 0xB8) {
            // or
            if (op_code == 0xB7) {
                // mem
            } else {
                or(A, get_register(op_code & 0b00000111));
            }
        } else if (op_code >= 0xB8 && op_code < 0xC0 ) {
            // cp

        } else if (op_code == 0xC0) {
            // RET NZ
        } else if (op_code == 0xC8) {
            // RET Z
        } else if (op_code == 0xD0) {
            // RET NC
        } else if (op_code == 0xD8) {
            // RET C
        } else if (op_code == 0xC9) {
            // RET
        } else if (op_code == 0xD9) {
            // RETI
        } else if (d1 >= 0xC && (d0 == 0x0F || d0 == 0x07)) {
            // RST
        } else if (d1 < 0x04 && ((d0 >= 3 && d0 < 6) || (d0 >= 0xB && d0 < 0xE))) {
            // INC or DEC
        } else if (d1 < 0x04 && (d0 == 0x06 || d0 == 0x0E)) {
            // LD REG, n
        } else if (d1 < 0x04 && d0 == 0x01) {
            // LD REG PAIR, nn
        } else if (d1 < 0x04 && d0 == 0x09) {
            // ADD REG PAIR, nn
        } else if (op_code == 0x02 || op_code == 0x12) {
            // LD (BC) or (DE), A
        } else if (op_code == 0x0A || op_code == 0x1A) {
            // LD A, (BC) or (DE)
        } else if (d1 >= 0x0C && d0 == 0x01) {
            // POP
        } else if ((d1 >= 0x0C && d1 < 0x0E) && (d0 == 0x02 || d0 == 0x0A)) {
            // JP flag nn
        } else if (op_code == 0xC3) {
            // JP nn
        } else if ((d1 >= 0x0C && d1 < 0x0E) && (d0 == 0x04 || d0 == 0x0C)) {
            // CALL flag, nn
        } else if (op_code == 0xCD) {
            // CALL nn
        } else if (d1 >= 0x0C && d0 == 0x05) {
            // PUSH
        } else if (op_code == 0x08) {
            // LD (nn) SP
        } else if (op_code == 0x18) {
            // JR n
        } else if ((d1 >= 0x02 && d1 < 0x04) && (d0 == 0x00 || d0 == 0x08)) {
            // JR flag nn
        } else if (op_code == 0x0F) {
            // rrc a
        } else if (op_code == 0x1F) {
            // rr a
        } else if (op_code == 0x2F) {
            // cpl
        } else if (op_code == 0x3F) {
            // ccf
        } else if (op_code == 0x07) {
            // rlc a
        } else if (op_code == 0x17) {
            // rl a
        } else if (op_code == 0x27) {
            // daa
        } else if (op_code == 0x37) {
            // scf
        }
    }

    public static void main (String[] args) {
        CPU cpu = new CPU();
        cpu.load(cpu.A, 0x01);
        cpu.load(cpu.B, 0x0F);
        cpu.DE.set(0xD111);
        System.out.println(cpu.DE.read());
        cpu.mmu.writeByte(0xD111, 0x11);
        System.out.println(cpu.mmu.readByte(0xD111));
        cpu.load_src_ind(cpu.A, cpu.DE);

//        cpu.add(cpu.A, cpu.B, false);
//        cpu.execute_op_code(0x47);
        System.out.println(cpu.A.read());
    }
}
