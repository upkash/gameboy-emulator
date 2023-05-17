package main;
import cpu.CPU;
import gpu.Screen;
import memory.MMU;

public class GameBoy implements Runnable {
    public CPU cpu;
    public GameBoy(String romPath) {
        Screen sc = new Screen();
        MMU mmu = new MMU(romPath);
        cpu = new CPU(mmu);
    }

    public void run() {
        while (!cpu.stop) {
            int cycles = cpu.tick();
        }
    }

    public static void main(String[] args) {
        GameBoy gb = new GameBoy("/Users/utkarsh/IdeaProjects/GameBoyEmulator/src/cpu_instrs/individual/02-interrupts.gb");
        gb.run();
    }
}
