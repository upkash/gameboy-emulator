package memory;

import gpu.Tile;
import gpu.TileMapContainer;
import gpu.TileSet;
import java.io.*;


/**
 *  Memory Management Unit for the Gameboy Classic;
 *  Maps addresses to byte arrays;
 *  Read and write values to memory;
 *  Stores tile information for graphics;
 */
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
    };

    private final int[] rom;
    private final int[] vram;
    private final int[] eram;
    private final int[] wram;
    private final int[] oam;
    private final int[] io;
    private final int[] hram;
    private final int[] wrams;

    private final TileSet tileSet;

    private final TileMapContainer tileMaps;


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
        tileSet = new TileSet();
        tileMaps = new TileMapContainer(tileSet);
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
//                tileSet.setTileVal(address, value);
            case 0x9000:
//                System.out.print.ln(Integer.toHexString(address & 0x1FFF));
                vram[address & 0x1FFF] = value;
//                if (address >= 0x9800 && address <= 0x9FFF) {
//                    tileMaps.updateTileMap(address, value);
//                }
//                System.out.println("writing to " + Integer.toHexString(address));
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
//        System.out.println(Integer.toHexString(address));
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

    public Tile getTile(int address) {
        return tileSet.getTile(address);
    }

    public Tile getTileFromIndex(int idx) {
        return tileSet.getTileFromIndex(idx);
    }
}
