package emulator.main;

import emulator.cpu.CPU;
import emulator.memory.MMU;
import emulator.ppu.*;

public class GameBoy implements Runnable {
    public CPU cpu;
    public PPU ppu;
    public GameBoy(String romPath) {
        MMU mmu = new MMU(romPath);
        ppu = new PPUHead(mmu);
        cpu = new CPU(mmu);
        mmu.writeByte(0xFF40, 0x91);
        mmu.writeByte(0xFF00, 0xFF);
    }

    public void run() {
        while (!cpu.stop) {
            int cycles = cpu.tick();
            ppu.tick(cycles);
        }
    }

    public static void main(String[] args) {
        GameBoy gb = new GameBoy("/Users/utkarsh/IdeaProjects/GameBoyEmulator/Tetris.gb");
        gb.run();
    }
}
