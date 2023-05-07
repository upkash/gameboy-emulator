package memory;

public class MMU  {

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
        rom =  new int[0x7EB0];
        eram = new int[0xFFF];
        vram = new int[0xFFF];
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
        return 0;

    }

    public void writeWord(int address, int value) {
        return;
    }

    public int readByte(int address) {
        switch (address & 0xf000) {
            case 0x0000:
                return 0;
            case 0x1000:
                return 0;
            case 0x2000:
                return 0;
            case 0x3000:
                return 0;
            case 0x4000:
                return 0;
            case 0x5000:
                return 0;
            case 0x6000:
                return 0;
            case 0x7000:
                return 0;
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

    public static void main (String[] args) {
        MMU mmu = new MMU();
        mmu.writeByte(0xD111, 0x11);
        System.out.println(mmu.readByte(0xD111));
    }
}
