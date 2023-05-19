package main;
import cpu.CPU;
import ppu.PPU;
import ppu.Screen;
import memory.MMU;

public class GameBoy implements Runnable {
    public CPU cpu;
    public PPU ppu;
    public GameBoy(String romPath) {
        MMU mmu = new MMU(romPath);
        ppu = new PPU(mmu);
        cpu = new CPU(mmu);
        mmu.writeByte(0xFF40, 0x91);
//        System.out.println(mmu.readByte(0xFF40));
    }

    public void run() {
        while (!cpu.stop) {
            int cycles = cpu.tick();
            ppu.tick(cycles);
        }
    }

    public static void main(String[] args) {
        GameBoy gb = new GameBoy("/Users/utkarsh/IdeaProjects/GameBoyEmulator/src/cpu_instrs/individual/06-ld r,r.gb");
        gb.run();
    }
}
