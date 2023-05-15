import cpu.CPU;
import gpu.GPU;
import gpu.Screen;
import gpu.TileSet;
import memory.MMU;

public class Gameboy {
    private static Thread cpuThread;
    private Thread gpuThread;
    public Gameboy() {
        Screen sc = new Screen();
        MMU mmu = new MMU("/Users/utkarsh/IdeaProjects/GameBoyEmulator/src/09-op r,r.gb");
        CPU cpu = new CPU(mmu);
        GPU gpu = new GPU(mmu, sc);
        cpuThread = new Thread(cpu);
        gpuThread = new Thread(gpu);
    }

    public void run() {
        cpuThread.start();
//        gpuThread.start();
    }

    public static void main(String[] args) {
        Gameboy gb = new Gameboy();
        gb.run();
    }
}
