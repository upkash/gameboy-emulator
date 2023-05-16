package tests;
import cpu.CPU;
import memory.MMU;
import org.junit.Test;
import static org.junit.Assert.*;

public class CPUTest {

    public boolean blarggTest(String rom) {
        String baseDir = "/Users/utkarsh/IdeaProjects/GameBoyEmulator/src/cpu_instrs/individual/";
        MMU mmu = new MMU(baseDir + rom);
        CPU cpu = new CPU(mmu);
        Thread t = new Thread(cpu);
        t.start();
        while (!cpu.testOutput.contains("Passed") && !cpu.testOutput.contains("Failed"));
        cpu.stop = true;
        try {
            t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(cpu.testOutput);
        return cpu.testOutput.contains("Passed");
    }

    @Test
    public void special() {
        String testName = "01-special.gb";
        assertTrue(blarggTest(testName));
    }

    @Test
    public void interrupts() {
        String testName = "02-interrupts.gb";
        assertTrue(blarggTest(testName));
    }

    @Test
    public void op_sp_hl() {
        String testName = "03-op sp,hl.gb";
        assertTrue(blarggTest(testName));
    }

    @Test
    public void op_r_imm() {
        String testName = "04-op r,imm.gb";
        assertTrue(blarggTest(testName));
    }

    @Test
    public void op_rp() {
        String testName = "05-op rp.gb";
        assertTrue(blarggTest(testName));
    }

    @Test
    public void ld_r_r() {
        String testName = "06-ld r,r.gb";
        assertTrue(blarggTest(testName));
    }

    @Test
    public void jumps() {
        String testName = "07-jr,jp,call,ret,rst.gb";
        assertTrue(blarggTest(testName));
    }

    @Test
    public void misc() {
        String testName = "08-misc instrs.gb";
        assertTrue(blarggTest(testName));
    }

    @Test
    public void op_r_r() {
        String testName = "09-op r,r.gb";
        assertTrue(blarggTest(testName));
    }

    @Test
    public void bit_ops() {
        String testName = "10-bit ops.gb";
        assertTrue(blarggTest(testName));
    }

    @Test
    public void op_a_hl() {
        String testName = "11-op a,(hl).gb";
        assertTrue(blarggTest(testName));
    }
}
