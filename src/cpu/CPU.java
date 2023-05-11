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
    private final SpecialRegister sp;
    private final SpecialRegister pc;
    private final RegisterPair BC;
    private final RegisterPair DE;
    private final RegisterPair HL;

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

    private boolean stop;

    public CPU (String rom_path) {
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
        mmu = new MMU(rom_path);
//        mmu = new MMU();
    }
    private void add(Register dst, Register src, int carry){
        int val = dst.read() + src.read() + carry;
        F.set_zero((val == 0) ? 0 : 1);
        F.set_operation(0);
        F.set_carry((val > 255) ? 1 : 0);
        F.set_half_carry(half_carry_add8(dst.read(),src.read()));
        dst.set(val & 255);
    }

    private void add(RegisterPair dst, RegisterPair src) {
        int val = dst.read() + src.read();
        F.set_operation(0);
        F.set_carry(val > 0xFFFF ? 1 : 0);
        F.set_half_carry(half_carry_add16(dst.read(),src.read()));
        dst.set(val);
    }

    private void add(RegisterPair dst, Register src) {
        int val = dst.read() + src.read();
        F.set_operation(0);
        F.set_carry(val > 0xFFFF ? 1 : 0);
        dst.set(val);
    }

    private void add_ind(Register dst, RegisterPair src, int carry){
        int read = mmu.readByte(src.read());
        int val = dst.read() + read + carry;
        F.set_zero((val == 0) ? 0 : 1);
        F.set_operation(0);
        F.set_carry((val > 255) ? 1 : 0);
        F.set_half_carry(half_carry_add8(dst.read(), read));
        dst.set(val & 255);
    }

    private void sub(Register dst, Register src, int carry){
        int val = dst.read() - src.read() - carry;
        F.set_zero((val == 0) ? 0 : 1);
        F.set_operation(1);
        F.set_carry((val < 0) ? 1 : 0);
        F.set_half_carry(half_carry_sub8(dst.read(), src.read()));
        dst.set(abs(val));
    }

    private void sub_ind(Register dst, RegisterPair src, int carry){
        int read = mmu.readByte(src.read());
        int val = dst.read() - read - carry;
        F.set_zero((val == 0) ? 0 : 1);
        F.set_operation(1);
        F.set_carry((val < 0) ? 1 : 0);
        F.set_half_carry(half_carry_sub8(dst.read(), read));
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
        mmu.writeByte(dst.read(), src.read());
    }

    private void load_dst_ind(int location, Register src) {
        mmu.writeByte(location, src.read());
    }

    private void load_src_ind(Register dst, RegisterPair src) {
        dst.set(mmu.readByte(src.read()));
    }

    private void inc(Register dst) {
        F.set_operation(0);
        F.set_half_carry(half_carry_add8(dst.read(), 1));
        F.set_zero(dst.increment() == 0 ? 1 : 0);
//        System.out.println("INC DST to "  + Integer.toHexString(dst.read()) + " z flag " + F.get_zero());
    }

    private void inc(RegisterPair dst) {
        F.set_operation(0);
        F.set_half_carry(half_carry_add16(dst.read(), 1));
        F.set_zero(dst.increment() == 0 ? 1 : 0);
    }

    private void dec(RegisterPair dst) {
        F.set_operation(1);
        F.set_half_carry(half_carry_sub16(dst.read(), 1));
        F.set_carry(dst.read()-1 < 0 ? 1 : 0);
        F.set_zero(dst.decrement() == 0 ? 1 : 0);
    }

    private void dec(Register dst) {
        F.set_operation(1);
        F.set_half_carry(half_carry_sub8(dst.read(), 1));
        F.set_zero(dst.decrement() == 0 ? 1 : 0);
//        System.out.println("DEC DST to "  + Integer.toHexString(dst.read()) + " z flag " + F.get_zero());
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
        else if (reg == 7) {
            return A;
        }
        return null;
    }

    private void pop(RegisterPair reg_pair) {
        int value = mmu.readByte(sp.read());
        sp.increment();
        value |= mmu.readByte(sp.read()) << 8;
        sp.increment();
        reg_pair.set(value);
    }

    private void pop() {
        // System.out.println("POPPING TO PC: " + Integer.toHexString(sp.read()));
        int value = mmu.readByte(sp.read());
        sp.increment();
        value |= mmu.readByte(sp.read()) << 8;
        sp.increment();
//        System.out.println(Integer.toHexString(value));
        pc.set(value-1);
    }

    private void pop(Register reg) {
        // System.out.println("POPPING TO REGISTER: " + Integer.toHexString(sp.read()));
        reg.set(mmu.readByte(sp.read()));
        sp.increment();
    }



    private void push(RegisterPair reg_pair) {
        // System.out.println("PUSHING REGISTER PAIR VAL: " + Integer.toHexString(sp.read()));
        sp.decrement();
        mmu.writeByte(sp.read(), reg_pair.read() >> 8);
//        System.out.println("PUSHED " + Integer.toHexString(reg_pair.read() >> 8)+ " @ " + Integer.toHexString(sp.read()));
        sp.decrement();
        mmu.writeByte(sp.read(), reg_pair.read() & 0x00FF);
//        System.out.println("PUSHED " + Integer.toHexString(reg_pair.read() & 0x00FF) + " @ " + Integer.toHexString(sp.read()));
    }

    private void push(SpecialRegister reg) {
        // System.out.println("PUSHING REGISTER VAL: " + Integer.toHexString(sp.read()));
        sp.decrement();
        mmu.writeByte(sp.read(), reg.read() >> 8);
        sp.decrement();
        mmu.writeByte(sp.read(), reg.read() & 0x00FF);
    }

    private void push(int val) {
//         System.out.println("PUSHING VAL: " + Integer.toHexString(val) + " to " + Integer.toHexString(sp.read()));
        sp.decrement();
        mmu.writeByte(sp.read(), val >> 8);
//        System.out.println("PUSHED " + Integer.toHexString(val >> 8)+ " @ " + Integer.toHexString(sp.read()));
        sp.decrement();
        mmu.writeByte(sp.read(), val & 0x00FF);
//        System.out.println("PUSHED " + Integer.toHexString(val & 0x00FF) + " @ " + Integer.toHexString(sp.read()));
    }

    private int half_carry_add8(int first_num, int second_num)
    {
        return (((first_num & 0x0F) + (second_num & 0x0F)) & 0x10) == 0x10 ? 1 : 0;
    }

    private int half_carry_add16(int first_num, int second_num)
    {
        return (((first_num & 0x00FF) + (second_num & 0x00FF)) & 0x0100) == 0x0100 ? 1: 0;
    }

    private int half_carry_sub8(int first_num, int second_num)
    {
        return (int)(first_num & 0x0F) - (int)(second_num & 0x0F) < 0 ? 1 : 0;
    }

    private int half_carry_sub16(int first_num, int second_num)
    {
        return (int)(first_num & 0x00FF) - (int)(second_num & 0x00FF) < 0 ? 1 : 0;
    }

    private void execute_op_code(int op_code) {
        // 0x [d1][d0]
        String out =    "A: " + String.format("%02X", A.read()) +
                        " F: " + String.format("%02X", F.read()) +
                        " B: " + String.format("%02X", B.read()) +
                        " C: " + String.format("%02X", C.read()) +
                        " D: " + String.format("%02X", D.read()) +
                        " E: " + String.format("%02X", E.read()) +
                        " H: " + String.format("%02X", H.read()) +
                        " L: " + String.format("%02X", L.read()) +
                        " SP: " + String.format("%04X", sp.read()) +
                        " PC: " + String.format("00:%04X", pc.read()) +
                        " ("+
                            String.format("%02X",op_code) + " " +
                            String.format("%02X", mmu.readByte(pc.read()+1))+ " " +
                            String.format("%02X", mmu.readByte(pc.read()+2))+ " " +
                            String.format("%02X", mmu.readByte(pc.read()+3))+
                        ")" ;
//                        + mmu.readByte(0x4244);
        System.out.println(out);
        int d1 = op_code >> 4;
        int d0 = op_code & 0x0F;
        if (op_code == 0x10) {
            stop = true;
        } else if (op_code == 0xE9) {
            pc.set(mmu.readByte(HL.read())-1);
        } else if (op_code == 0xE0) {
            load_dst_ind(mmu.readByte(pc.read()+1), A);
            pc.increment();
        } else if (op_code == 0xEA) {
            // load (nn), A
            load_dst_ind(mmu.readWord(pc.read()+1), A);
            pc.increment();
            pc.increment();
        } else if (op_code == 0x76) {
            // halt
        } else if (d1 == 0x07 && d0 <= 0x07) {
//            System.out.println("LOADING " + d0 + " into HL");
            load_dst_ind(HL, get_register(d0));
        } else if (op_code >= 0x40 && op_code < 0x80) {
            // ld
            int src = op_code & 0b00000111;
            // 0111 1000
            int dst = (op_code & 0b00111000) >> 3;

             System.out.println("ld " + Integer.toHexString(dst) + " " + Integer.toHexString(src));
            if (src == 0x06) {
                load_src_ind(get_register(dst), HL);
            } else if (dst == 0x06) {
                load_dst_ind(HL, get_register(src));
            } else {
                load(get_register(dst), get_register(src));
            }

        } else if (op_code >= 0x80 && op_code < 0x90) {
            // add & adc
            // System.out.println("ADD");
            if (d0 == 0x06 || d0 == 0x0E) {
                add_ind(A, HL, (d0 == 0x0E && F.get_carry() == 1) ? 1 : 0);
            } else {
                int dst = op_code & 0b00000111;
                add(A, get_register(dst), (op_code > 0x87 && F.get_carry() == 1) ? 1 : 0);
            }
        } else if (op_code >= 0x90 && op_code < 0xA0) {
            // sub & sbc
            // System.out.println("SUB");
            if (d0 == 0x06 || d0 == 0x0E) {
                sub_ind(A, HL, (d0 == 0x0E && F.get_carry() == 1) ? 1 : 0);
            } else {
                int dst = op_code & 0b00000111;
                sub(A, get_register(dst), (op_code > 0x97 && F.get_carry() == 1) ? 1 : 0);
            }
        } else if (op_code >= 0xA0 && op_code < 0xA8) {
            // and
            // System.out.println("AND");
            if (op_code == 0xA6) {
                // mem
                and_ind(A, HL);
            } else {
                and(A, get_register(op_code & 0b00000111));
            }
        } else if (op_code >= 0xA9 && op_code < 0xB0) {
            // xor
            // System.out.println("XOR");
            if (op_code == 0xAe) {
                // mem
                xor_ind(A, HL);
            } else {
                xor(A, get_register(op_code & 0b00000111));
            }
        } else if (op_code >= 0xB0 && op_code < 0xB8) {
            // or
            // System.out.println("OR");
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
            // System.out.println("RET NZ");
            if (F.get_zero() == 0) pop();
        } else if (op_code == 0xC8) {
            // RET Z
            // System.out.println("RET Z");
            if (F.get_zero() != 0) pop();
        } else if (op_code == 0xD0) {
            // RET NC
            // System.out.println("RET NC");
            if (F.get_carry() == 0) pop();
        } else if (op_code == 0xD8) {
            // RET C
            // System.out.println("RET C");
            if (F.get_carry() != 0) pop();
        } else if (op_code == 0xC9) {
            // RET
            // System.out.println("RET");
//            System.out.println("RET to " + mmu.readWord(sp.read()+1));
//            System.out.println("STACK");
//            for (int i = sp.read(); i >= 0xFFFF; i ++) {
//
//            }
//            System.out.println(Integer.toHexString(mmu.readByte(sp.read())));
           pop();
//           System.out.println();
        } else if (op_code == 0xD9) {
            // RETI
            return;
        } else if (d1 >= 0xC && (d0 == 0x0F || d0 == 0x07)) {
            // RST
            // System.out.println("RST");
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
            // System.out.println("INC/DEC");
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
//                // System.out.println("inc");
                if (d1 == 0x0) inc(B);
                else if (d1 == 0x1) inc(D);
                else if (d1 == 0x2) inc(H);
                else if (d1 == 0x3) inc(HL);
            }
        } else if (d1 < 0x04 && (d0 == 0x06 || d0 == 0x0E)) {
            // LD REG, n
            // System.out.println("ld reg n");
            int n = mmu.readByte(pc.read()+1);
//            System.out.println(Integer.toBinaryString((op_code & 0b00111000) >> 3));
            load_imm8(get_register((op_code & 0b00111000) >> 3), n);
            pc.increment();
        } else if (d1 < 0x04 && d0 == 0x01) {
            // LD REG PAIR, nn
            // System.out.println("ld pair nn");
            int nn = mmu.readWord(pc.read()+1);
            if (d1 == 0x00) load_imm16(BC, nn);
            else if (d1 == 0x01) load_imm16(DE, nn);
            else if (d1 == 0x02) load_imm16(HL, nn);
            else sp.set(nn);
            pc.increment();
            pc.increment();
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
            // System.out.println("ld (pair) A");
            if (op_code == 0x02) {
                load_dst_ind(BC, A);
            } else {
                load_dst_ind(DE, A);
            }
        } else if (op_code == 0x0A || op_code == 0x1A) {
            // LD A, (BC) or (DE)
            // System.out.println("ld A, (pair)");
            if (op_code == 0x0A) {
                load_src_ind(A, BC);
            } else {
                load_src_ind(A, DE);
            }
        } else if (d1 >= 0x0C && d0 == 0x01) {
            // POP
            // System.out.println("POP");
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
            int nn = mmu.readWord(pc.read()+1);
            if (F.get_zero() == 1) pc.set(nn-1);
        } else if (op_code == 0xC3) {
            // JP nn
            int nn = mmu.readWord(pc.read()+1);
            // System.out.println("JUMPING TO " + Integer.toHexString(nn));
            pc.set(nn-1);
        } else if ((d1 >= 0x0C && d1 < 0x0E) && (d0 == 0x04 || d0 == 0x0C)) {
            // CALL flag, nn
            if ((op_code == 0xC4 && F.get_zero() == 0) ||
                (op_code == 0xCC && F.get_zero() == 1) ||
                (op_code == 0xD4 && F.get_carry() == 0) ||
                (op_code == 0xDC && F.get_carry() == 1)) {
                int nn = mmu.readWord(pc.read()+1);
                // System.out.println("CALL flag to " + Integer.toHexString(nn));
                push(pc.read()+3);
                pc.set(nn-1);
            }
        } else if (op_code == 0xCD) {
            // CALL nn
            int nn = mmu.readWord(pc.read()+1);
//             System.out.println("CALL to " + Integer.toHexString(nn) + " pushing " + Integer.toHexString(pc.read()+3));
            push(pc.read()+3);
//            System.out.println();
            pc.set(nn-1);
        } else if (d1 >= 0x0C && d0 == 0x05) {
            // PUSH
            // System.out.println("PUSH");
            if (d1 == 0x0C) {
                push(BC);
            } else if (d1 == 0x0D) {
                push(DE);
            } else if (d1 == 0x0E) {
                push(HL);
            } else {
                // flags
//                System.out.println("PUSHING AF");
                sp.decrement();
                mmu.writeByte(sp.read(), A.read());
//                System.out.println("Wrote " + Integer.toHexString(A.read()) + " @ " + Integer.toHexString(sp.read()));
                sp.decrement();
                mmu.writeByte(sp.read(), F.read());
//                System.out.println("Wrote " + Integer.toHexString(F.read()) + " @ " + Integer.toHexString(sp.read()));
            }
        } else if (op_code == 0x08) {
            // LD (nn) SP
            int nn = mmu.readWord(pc.read()+1);
            load_imm16(nn, sp);
        } else if (op_code == 0x18) {
            // JR n
            int n = mmu.readByte(pc.read()+1);
//             System.out.println("JR to " + Integer.toHexString(n));
            pc.set(pc.read() + n+1);
        } else if ((d1 >= 0x02 && d1 < 0x04) && (d0 == 0x00 || d0 == 0x08)) {
            // JR flag nn
            if ((op_code == 0x20 && F.get_zero() == 0 )||
                (op_code == 0x28 && F.get_zero() == 1) ||
                (op_code == 0x30 && F.get_carry() == 0) ||
                (op_code == 0x38 && F.get_carry() == 1) ) {
                int n = mmu.readByte(pc.read()+1);
                if (n > 128) {
                    n = (-256) | ( n );
                    n++;
                }
//                 System.out.println("JR flag to " + Integer.toHexString(pc.read()+n));
                pc.set(pc.read()+n);
            } else {
                pc.increment();
            }
        } else if (op_code == 0x2A) {
            load_src_ind(A, HL);
            HL.increment();
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


    public void run() {
        int i = 0x0000;
        while (true) {
            if (stop) return;
            pc.increment();
            int curr_instr = mmu.readByte(pc.read());
//            String out = "PC " + Integer.toHexString(pc.read()) + " INST " + Integer.toHexString(curr_instr);
            execute_op_code(curr_instr);
            i++;
        }
    }


    public static void main (String[] args) {
        CPU cpu = new CPU("/Users/utkarsh/IdeaProjects/GameBoyEmulator/src/09-op r,r.gb");
        cpu.run();
//        System.out.println(Integer.toHexString(cpu.mmu.readByte(0x4244)));
    }
}
