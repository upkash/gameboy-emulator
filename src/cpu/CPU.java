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
    private final Flag F;

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
        F = new Flag();
        sp = new Register();
        pc = new Register();
        BC = new RegisterPair(B, C);
        DE = new RegisterPair(D, E);
        HL = new RegisterPair(H, L);
        mmu = new MMU();
    }
    private void add(Register dst, Register src, int carry){
        int val = dst.read() + src.read() + carry;
        F.set_zero((val == 0) ? 0 : 1);
        F.set_operation(0);
        F.set_carry((val > 255) ? 1 : 0);
        dst.set(val & 255);
    }

    private void add(RegisterPair dst, RegisterPair src) {
        int val = dst.read() + src.read();
        F.set_operation(0);
        F.set_carry(val > 0xFFFF ? 1 : 0);
        dst.set(val);
    }

    private void add(RegisterPair dst, Register src) {
        int val = dst.read() + src.read();
        F.set_operation(0);
        F.set_carry(val > 0xFFFF ? 1 : 0);
        dst.set(val);
    }

    private void add_ind(Register dst, RegisterPair src, int carry){
        int val = dst.read() + mmu.readByte(src.read()) + carry;
        F.set_zero((val == 0) ? 0 : 1);
        F.set_operation(0);
        F.set_carry((val > 255) ? 1 : 0);
        dst.set(val & 255);
    }

    private void sub(Register dst, Register src, int carry){
        int val = dst.read() - src.read() - carry;
        F.set_zero((val == 0) ? 0 : 1);
        F.set_operation(1);
        F.set_carry((val < 0) ? 1 : 0);
        dst.set(abs(val));
    }

    private void sub_ind(Register dst, RegisterPair src, int carry){
        int val = dst.read() - mmu.readByte(src.read()) - carry;
        F.set_zero((val == 0) ? 0 : 1);
        F.set_operation(1);
        F.set_carry((val < 0) ? 1 : 0);
        dst.set(abs(val));
    }

    private void load(Register dst, Register src) {
        dst.set(src.read());
    }


    private void load(Register dst, int loc) {
        dst.set(mmu.readByte(loc));
    }

    private void load(RegisterPair dst, int loc) {
        dst.set(mmu.readWord(loc));
    }

    private void load_imm8(Register dst, int val) {
        dst.set(val);
    }

    private void load_imm16(RegisterPair dst, int val) {
        dst.set(val);
    }

    private void load_imm16(int val, Register src) {
        mmu.writeWord(val, src.read());
    }

    private void load_dst_ind(RegisterPair dst, Register src) {
        mmu.writeByte(src.read(), dst.read());
    }

    private void load_src_ind(Register dst, RegisterPair src) {
        dst.set(mmu.readByte(src.read()));
    }

    private void inc(Register dst) {
        F.set_operation(0);
        F.set_zero(dst.increment() == 0 ? 0 : 1);
    }

    private void inc(RegisterPair dst) {
        dst.increment();
    }

    private void dec(RegisterPair dst) {
        dst.increment();
    }

    private void dec(Register dst) {
        F.set_operation(1);
        F.set_zero(dst.decrement() == 0 ? 0 : 1);
    }

    private void and(Register dst, Register src) {
        int val = dst.read() & src.read();
        F.set_operation(0);
        F.set_zero(val == 0 ? 0 : 1);
        F.set_carry(val > 0xFF ? 1 : 0);
        dst.set(val & 0xFF);
    }

    private void and_ind(Register dst, RegisterPair src) {
        int val = dst.read() & mmu.readByte(src.read());
        F.set_operation(0);
        F.set_zero(val == 0 ? 0 : 1);
        F.set_carry(val > 0xFF ? 1 : 0);
        dst.set(dst.read() & mmu.readByte(src.read()));
    }

    private void or(Register dst, Register src) {
        int val = dst.read() | src.read();
        F.set_operation(0);
        F.set_zero(val == 0 ? 0 : 1);
        F.set_carry(val > 0xFF ? 1 : 0);
        dst.set(val & 0xFF);
    }

    private void or_ind(Register dst, RegisterPair src) {
        int val = dst.read() | mmu.readByte(src.read());
        F.set_operation(0);
        F.set_zero(val == 0 ? 0 : 1);
        F.set_carry(val > 0xFF ? 1 : 0);
        dst.set(val & 0xFF);
    }

    private void xor(Register dst, Register src) {
        int val = dst.read() ^ src.read();
        F.set_operation(0);
        F.set_zero(val == 0 ? 0 : 1);
        F.set_carry(0);
        dst.set(val & 0xFF);
    }

    private void xor_ind(Register dst, RegisterPair src) {
        int val = dst.read() ^ mmu.readByte(src.read());
        F.set_operation(0);
        F.set_zero(val == 0 ? 0 : 1);
        F.set_carry(0);
        dst.set(val & 0xFF);
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
        reg_pair.set(mmu.readByte(sp.read()));
        sp.decrement();
        sp.decrement();
    }

    private void pop() {
        pc.set(mmu.readByte(sp.read()));
        sp.decrement();
    }

    private void pop(Register reg) {
        reg.set(mmu.readByte(sp.read()));
        sp.decrement();
    }



    private void push(RegisterPair reg_pair) {
        mmu.writeWord(sp.read(), reg_pair.read());
        sp.increment();
        sp.increment();
    }

    private void push(Register reg) {
        mmu.writeWord(sp.read(), reg.read());
        sp.increment();
    }

    private void execute_op_code(int op_code) {
        // 0x [d1][d0]
        int d1 = op_code >> 4;
        int d0 = op_code & 0x0F;
        if (op_code == 0x00) {
            return;
        }
        else if (op_code == 0x76) {
            // halt
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
            if (d0 == 0x06 || d0 == 0x0E) {
                add_ind(A, HL, (d0 == 0x0E && F.get_carry() == 1) ? 1 : 0);
            } else {
                int dst = op_code & 0b00000111;
                add(A, get_register(dst), (op_code > 0x87 && F.get_carry() == 1) ? 1 : 0);
            }
        } else if (op_code >= 0x90 && op_code < 0xA0) {
            // sub & sbc
            if (d0 == 0x06 || d0 == 0x0E) {
                sub_ind(A, HL, (d0 == 0x0E && F.get_carry() == 1) ? 1 : 0);
            } else {
                int dst = op_code & 0b00000111;
                sub(A, get_register(dst), (op_code > 0x97 && F.get_carry() == 1) ? 1 : 0);
            }
        } else if (op_code >= 0xA0 && op_code < 0xA8) {
            // and
            if (op_code == 0xA6) {
                // mem
                and_ind(A, HL);
            } else {
                and(A, get_register(op_code & 0b00000111));
            }
        } else if (op_code >= 0xA9 && op_code < 0xB0) {
            // xor
            if (op_code == 0xAe) {
                // mem
                xor_ind(A, HL);
            } else {
                xor(A, get_register(op_code & 0b00000111));
            }
        } else if (op_code >= 0xB0 && op_code < 0xB8) {
            // or
            if (op_code == 0xB7) {
                // mem
                or_ind(A, HL);
            } else {
                or(A, get_register(op_code & 0b00000111));
            }
        } else if (op_code >= 0xB8 && op_code < 0xC0 ) {
            // cp

        } else if (op_code == 0xC0) {
            // RET NZ
            if (F.get_zero() == 0) pop();
        } else if (op_code == 0xC8) {
            // RET Z
            if (F.get_zero() != 0) pop();
        } else if (op_code == 0xD0) {
            // RET NC
            if (F.get_carry() == 0) pop();
        } else if (op_code == 0xD8) {
            // RET C
            if (F.get_carry() != 0) pop();
        } else if (op_code == 0xC9) {
            // RET
           pop();
        } else if (op_code == 0xD9) {
            // RETI
            return;
        } else if (d1 >= 0xC && (d0 == 0x0F || d0 == 0x07)) {
            // RST
            pc.increment();
            push(pc);
            if (op_code == 0xC7) {
                pc.set(0x00);
            } else if (op_code == 0xCF) {
                pc.set(0x08);
            } else if (op_code == 0xD7) {
                pc.set(0x10);
            } else if (op_code == 0xDF) {
                pc.set(0x18);
            } else if (op_code == 0xE7) {
                pc.set(0x20);
            } else if (op_code == 0xEF) {
                pc.set(0x28);
            } else if (op_code == 0xF7) {
                pc.set(0x30);
            } else if (op_code == 0xFF) {
                pc.set(0x38);
            }
        } else if (d1 < 0x04 && ((d0 >= 3 && d0 < 6) || (d0 >= 0xB && d0 < 0xE))) {
            // INC or DEC
            if (d0 == 0x3) {
                if (d1 == 0x0) inc(BC);
                else if (d1 == 0x1) inc(DE);
                else if (d1 == 0x2) inc(HL);
                else if (d1 == 0x3) inc(sp);
            } else if (d0 == 0xB) {
                if (d1 == 0x0) dec(BC);
                else if (d1 == 0x1) dec(DE);
                else if (d1 == 0x2) dec(HL);
                else if (d1 == 0x3) dec(sp);
            } else if (d0 == 0xD) {
                if (d1 == 0x0) dec(C);
                else if (d1 == 0x1) dec(E);
                else if (d1 == 0x2) dec(L);
                else if (d1 == 0x3) dec(A);
            } else if (d0 == 0x5) {
                if (d1 == 0x0) dec(B);
                else if (d1 == 0x1) dec(D);
                else if (d1 == 0x2) dec(H);
                else if (d1 == 0x3) dec(HL);

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
        } else if (d1 < 0x04 && (d0 == 0x06 || d0 == 0x0E)) {
            // LD REG, n
            return;
        } else if (d1 < 0x04 && d0 == 0x01) {
            // LD REG PAIR, nn
            int nn = mmu.readWord(pc.read()+1);
            if (d1 == 0x00) load_imm16(BC, nn);
            else if (d1 == 0x01) load_imm16(DE, nn);
            else if (d1 == 0x02) load_imm16(HL, nn);
            else sp.set(nn);
            return;
        } else if (d1 < 0x04 && d0 == 0x09) {
            // ADD REG PAIR, REG PAIR
            if (d1 == 0x00) {
                add(HL, BC);
            } else if (d1 == 0x01) {
                add(HL, DE);
            } else if (d1 == 0x02) {
                add(HL, HL);
            } else {
                add(HL, sp);
            }
        } else if (op_code == 0x02 || op_code == 0x12) {
            // LD (BC) or (DE), A
            if (op_code == 0x02) {
                load_dst_ind(BC, A);
            } else {
                load_dst_ind(DE, A);
            }
        } else if (op_code == 0x0A || op_code == 0x1A) {
            // LD A, (BC) or (DE)
            if (op_code == 0x0A) {
                load_src_ind(A, BC);
            } else {
                load_src_ind(A, DE);
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
                pop(F);
            }
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
            if (d1 == 0x0C) {
                push(BC);
            } else if (d1 == 0x0D) {
                push(DE);
            } else if (d1 == 0x0E) {
                push(HL);
            } else {
                // flags
                sp.increment();
                mmu.writeByte(sp.read(), A.read());
                sp.increment();
                mmu.writeByte(sp.read(), F.read());
            }
        } else if (op_code == 0x08) {
            // LD (nn) SP
            int nn = mmu.readWord(pc.read()+1);
            load_imm16(nn, sp);
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


    private void run() {
        while (true) {
            int curr_instr = mmu.readByte(pc.read());
            pc.increment();
            execute_op_code(curr_instr);
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
