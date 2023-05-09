package memory;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;

public class MMU  {
    private static final int ROM_BANK_SIZE = 0x4000;
    private static final int RAM_BANK_SIZE = 0x2000;
    int[] boot = new int[0x100];
    private static final int[] boot_loader = {
            // prod memory
            0x31, 0xFE, 0xFF, // LD SP,$FFFE

            // enable LCD
            0x3E, 0x91, // LD A,$91
            0xE0, 0x40, // LDH [Mem::LCDC], A

            // set flags
            0x3E, 0x01, // LD A,$01
            0xCB, 0x7F, // BIT 7,A (sets Z,n,H)
            0x37,       // SCF (sets C)

            // set registers
            0x3E, 0x01, // LD A,$01
            0x06, 0x00, // LD B,$00
            0x0E, 0x13, // LD C,$13
            0x16, 0x00, // LD D,$00
            0x1E, 0xD8, // LD E,$D8
            0x26, 0x01, // LD H,$01
            0x2E, 0x4D, // LD L,$4D

            // skip to the end of the bootloader
            0xC3, 0xFD, 0x00, // JP 0x00FD
    };;
    private boolean inbios;
    private int[] bios;
    private int[] rom;
    private int[] vram;
    private int[] eram;
    private int[] wram;
    private int[] wrams;
    private int[] oam;
    private int[] empty;
    private int[] io;
    private int[] ppu;
    private int[] zram;
    private int[] intenable;

    public MMU() {
        wram = new int[0x1FFF];
        wrams = new int[0x1FFF];
        rom =  new int[0x8000];
        eram = new int[0xFFF];
        vram = new int[0xFFF];
        System.arraycopy(boot_loader, 0, boot, 0, boot_loader.length);
        int[] test_rom = {0xC3, 0x01, 0x04, 0x0C, 0x40};
        System.arraycopy(test_rom, 0, rom, 0x0100, test_rom.length);
    }

    public MMU(String rom_path) {
        wram = new int[0x1FFF];
        wrams = new int[0x1FFF];
        rom =  new int[0x7F00];
        eram = new int[0xFFF];
        vram = new int[0xFFF];
        System.arraycopy(boot_loader, 0, boot, 0, boot_loader.length);
        try {
            rom = load_rom(rom_path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int[] load_rom(String fileName) throws IOException {
        byte[] cartridgeMemory = new byte[0x8000];

        try {
            FileInputStream inputStream = new FileInputStream(fileName);

            int total = 0, nRead = 0;

            while ((nRead = inputStream.read(cartridgeMemory)) != -1) {
                total += nRead;
            }
            inputStream.close();

            System.out.println("Read " + total + " bytes");
        } catch (IOException e) {
            throw new IOException(e);
        }
        int[] rom = new int[cartridgeMemory.length];
        for (int i = 0; i < cartridgeMemory.length; i++) {
            rom[i] = cartridgeMemory[i] & 0xFF;
            if (rom[i] != 0)
                System.out.println("addr: " + Integer.toHexString(i) + " val: " + Integer.toHexString(rom[i]));
        }
        return rom;
    }

    public void writeByte(int address, int value) {
        switch (address & 0xf000) {
            case 0x0000:
            case 0x1000:
            case 0x2000:
            case 0x3000:
            case 0x4000:
            case 0x5000:
            case 0x6000:
            case 0x7000:
            case 0x8000:
            case 0x9000:
            case 0xA000:
            case 0xB000:
            case 0xC000:    // internal RAM
            case 0xD000:
                wram[address & 0x1FFF] = value;
            case 0xE000:
            case 0xF000:
                break;
        }

    }

    public int readWord(int address) {
        return readByte(address) | readByte(address+1) << 8;

    }

    public void writeWord(int address, int value) {
        return;
    }

    public int readByte(int address) {
//        System.out.println("READING " + Integer.toHexString(address));
        switch (address & 0xf000) {
            case 0x0000:
            case 0x0100:
            case 0x2000:
            case 0x3000:
            case 0x4000:
            case 0x5000:
            case 0x6000:
            case 0x7000:
//                String out = "READING " + Integer.toHexString(address);
//                System.out.println(out);
                return rom[address];
            case 0x8000:
                return 0;
            case 0x9000:
                return 0;
            case 0xA000:
                return 0;
            case 0xB000:
                return 0;
            case 0xC000:    // internal RAM
            case 0xD000:
                return wram[address & 0x1FFF];
            case 0xE000:
                return 0;
            case 0xF000:
                return 0;

        }
        return 0;
    }

    public static void main (String[] args) throws IOException {
        MMU mmu = new MMU();
        System.out.println(Integer.toHexString(mmu.readByte(0x100)));
        System.out.println(Integer.toHexString(mmu.readWord(0x101)));
    }
}
