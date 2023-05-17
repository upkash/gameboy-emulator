package gpu;

import memory.MMU;

import javax.swing.*;
import java.awt.*;

public class GPU implements Runnable {
    private final MMU mmu;
    private final Screen sc;
    private int[][] pixels;
    private int currScanLine;
    private int scrollX;
    private int scrollY;


    public GPU(MMU mmu, Screen sc) {
        this.mmu = mmu;
        this.sc = sc;
        currScanLine = 0;
        pixels = new int[144][160];
    }

    private void scanLine(int y) {
        int[] mergedLine = new int [160];
        int address = 0x9800 + ((y + scrollY) & 0xFF) / 8 * 32 + scrollX / 8;
        for (int i = 0; i < 20; i++) {
            int tileIdx = mmu.readByte(address+i);
            int [][] curr = mmu.getTileFromIndex(tileIdx).getPixels();
            System.arraycopy(curr[0],0, mergedLine, i * 8, 8);
        }
        sc.setLine(mergedLine, currScanLine);
    }

    private void tick() {
        mmu.writeByte(0xFF44, currScanLine);
        if (currScanLine < 144) {
            scanLine(currScanLine);
        } else if (currScanLine == 144) {
            // Vblank start
            sc.repaint();
        } else if (currScanLine < 154) {
            // other vblank
        }

        currScanLine++;
        if (currScanLine >= 154) {
            currScanLine = 0;
        }
    }

    @Override
    public void run() {
        JFrame frame = new JFrame("main.Gameboy Screen");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(sc);
        frame.pack();
        frame.setVisible(true);
        while (true) {
            tick();
        }
    }
}
