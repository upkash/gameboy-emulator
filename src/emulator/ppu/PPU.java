package emulator.ppu;

import emulator.memory.MMU;


public abstract class PPU  {
    protected static final int CLOCKS_PER_HBLANK = 204; /* Mode 0 */
    protected static final int CLOCKS_PER_SCANLINE_OAM = 80; /* Mode 2 */
    protected static final int CLOCKS_PER_SCANLINE_VRAM = 172; /* Mode 3 */
    protected static final int CLOCKS_PER_SCANLINE =
                (CLOCKS_PER_SCANLINE_OAM + CLOCKS_PER_SCANLINE_VRAM + CLOCKS_PER_HBLANK);

    private static final int CLOCKS_PER_VBLANK = 4560; /* Mode 1 */
    private static final int SCANLINES_PER_FRAME = 144;
    private static final int CLOCKS_PER_FRAME = (CLOCKS_PER_SCANLINE * SCANLINES_PER_FRAME) + CLOCKS_PER_VBLANK;

    protected static final int TILE_SET_ZERO_ADDR = 0x8000;
    protected static final int TILE_SET_ONE_ADDR = 0x8800;
    protected static final int TILE_MAP_ZERO_ADDR = 0x9800;
    protected static final int TILE_MAP_ONE_ADDR = 0x9C00;


    protected final MMU mmu;
    protected int scanLine;
    protected PPUMode mode = PPUMode.ACCESS_OAM;
    protected int cycleCounter;

    public PPU(MMU mmu) {
        this.mmu = mmu;
    }

    public abstract void tick(int cycles);

    protected abstract void drawBgLine(int line);

    protected abstract void drawWindowLine(int line);

    protected void writeScanline(int line) {
        if (!mmu.displayEnabled()) { return; }
        if (mmu.bgEnabled()) {
            drawBgLine(line);
        }

        if (mmu.windowEnabled()) {
            drawWindowLine(line);
        }
    }


    protected int getColorFromPixel(int byte1, int byte2, int pixelIndex) {
//        System.out.println("WRITING COLOR " + ((byte2 >> 7-pixelIndex << 1 | byte1 >> 7-pixelIndex) & 0xFF));
        return (byte2 >> 7-pixelIndex << 1 | byte1 >> 7-pixelIndex) & 0x03;
    }

}
