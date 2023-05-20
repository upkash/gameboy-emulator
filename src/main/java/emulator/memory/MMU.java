package emulator.memory;

import java.io.*;


/**
 *  Memory Management Unit for the emulator.main.Gameboy Classic;
 *  Maps addresses to byte arrays;
 *  Read and write values to emulator.memory.memory;
 *  Stores tile information for graphics;
 */
public class MMU  {
    private static final int ROM_BANK_SIZE = 0x4000;
    private static final int RAM_BANK_SIZE = 0x2000;
    int[] boot = new int[0x100];
    private static final int[] boot_loader = {
            // prod emulator.memory.memory
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
    };

    private final int[] rom;
    private final int[] vram;
    private final int[] eram;
    private final int[] wram;
    private final int[] oam;
    private final int[] io;
    private final int[] hram;
    private final int[] wrams;


    public MMU(String romPath) {
        wram = new int[0x2000];
        eram = new int[0x2000];
        vram = new int[0x2000];
        oam = new int[0xA0];
        io = new int[0x80];
        hram = new int[0x80];
        wrams = new int[0x1E00];
        System.arraycopy(boot_loader, 0, boot, 0, boot_loader.length);
        try {
            rom = loadRom(romPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int[] loadRom(String fileName) throws IOException {
        byte[] cartridgeMemory = new byte[0x8000];

        try {
            FileInputStream inputStream = new FileInputStream(fileName);
            while (inputStream.read(cartridgeMemory) != -1);
            inputStream.close();
        } catch (IOException e) {
            throw new IOException(e);
        }

        int[] rom = new int[cartridgeMemory.length];
        for (int i = 0; i < cartridgeMemory.length; i++) {
            rom[i] = cartridgeMemory[i] & 0xFF;
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
                vram[address & 0x1FFF] = value;
                break;
            case 0xA000:
            case 0xB000:
                eram[address & 0x1FFF] = value;
                break;
            case 0xC000:    // internal RAM
            case 0xD000:
                wram[address & 0x1FFF] = value;
                if (address < 0xDDFF) {
                    wrams[address & 0x1FFF] = value;
                }
                break;
            case 0xE000:
                throw new IndexOutOfBoundsException();
            case 0xF000:
                if (address >= 0xFE00 && address <= 0xFE9F) oam[address & 0x00FF] = value;
                else if (address < 0xFF4C) io[address - 0xFF00] = value;
                else if (address <= 0xFFFF) hram[address - 0xFF80] = value;
                else {
                    throw new IndexOutOfBoundsException();
                }
        }

    }

    public int readWord(int address) {
        return readByte(address) | readByte(address+1) << 8;

    }

    public void writeWord(int address, int value) {
        writeByte(address, value & 0x00ff);
        writeByte(address + 1, value >> 8);
    }

    public int readByte(int address) {
        switch (address & 0xf000) {
            case 0x0000:
            case 0x0100:
            case 0x2000:
            case 0x3000:
            case 0x4000:
            case 0x5000:
            case 0x6000:
            case 0x7000:
                return rom[address];
            case 0x8000:
            case 0x9000:
                return vram[address & 0x1FFF];
            case 0xA000:
            case 0xB000:
                return eram[address & 0x1FFF];
            case 0xC000:    // internal RAM
            case 0xD000:
                return wram[address & 0x1FFF];
            case 0xE000:
            case 0xF000:
                if (address <= 0xFDFF) return wrams[address & 0x1FFF];
                else if (address <= 0xFE9F) return oam[address & 0x00FF];
                else if (address < 0xFF4C) return io[address - 0xFF00];
                else if (address <= 0xFFFF) return hram[address - 0xFF80];
                else throw new IndexOutOfBoundsException();

        }
        return 0;
    }

    public int getIF() {
        return readByte(0xFF0F);
    }

    public int getIE() {
        return readByte(0xFFFF);
    }

    public void unSetIFBit(int bit) {
        int iF = getIF();
        iF = iF & ~(1 << bit);
        writeByte(0xFF0F, iF);
    }

    public void unSetIEBit(int bit) {
        int iE = getIE();
        iE = iE & ~(1 << bit);
        writeByte(0xFFFF, iE);
    }

    public void setIFBit(int bit) {
        int iF = getIF();
        iF = iF | (1 << bit);
        writeByte(0xFF0F, iF);
    }

    public void setIEBit(int bit) {
        int iE = getIE();
        iE = iE | (1 << bit);
        writeByte(0xFFFF, iE);
    }

    public int getStat() {
        return readByte(0xFF41);
    }

    public int getStatBit(int bit) {
        return 0;
    }

    public void setStatBit(int bit) {
        int stat = getStat();
        stat = stat | (1 << bit);
        writeByte(0xFF41, stat);
    }

    public void unSetStatBit(int bit) {
        int stat = getStat();
        stat = stat & ~(1 << bit);
        writeByte(0xFF41, stat);
    }

    public int getLYComp() {
        return readByte(0xFF45);
    }

    public void setLYComp(int value) {
        writeByte(0xFF45, value);
    }

    public int getLY() {
        return readByte(0xFF44);
    }

    public void setLY(int value) {
        writeByte(0xFF44, value);
    }

    public boolean displayEnabled() {
        return (readByte(0xFF40) >> 7) == 1;
    }
    public boolean windowTileMap() {
        return ((readByte(0xFF40) >> 6) & 0x01) == 1;
    }
    public boolean windowEnabled() {
        return ((readByte(0xFF40) >> 5) & 0x01) == 1;
    }
    public boolean bgWindowTileData() {
        return ((readByte(0xFF40) >> 4) & 0x01) == 1;
    }
    public boolean bgTileMapDisplay() {
        return ((readByte(0xFF40) >> 3) & 0x01) == 1;
    }
    public boolean spriteSize() {
        return ((readByte(0xFF40) >> 2) & 0x01) == 1;
    }
    public boolean spritesEnabled() {
        return ((readByte(0xFF40) >> 1) & 0x01) == 1;
    }
    public boolean bgEnabled() {
//        System.out.println(readByte(0xFF40 & 0x01));
//        return readByte(0xFF40 & 0x01) == 1;
        return true;
    }

    public int getScrollX() {
        return readByte(0xFF42);
    }

    public int getScrollY() {
        return readByte(0xFF43);
    }
}
