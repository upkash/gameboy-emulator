package emulator.ppu;

import emulator.memory.MMU;

public class PPUHeadless extends PPU {
    private final int[][] pixels;
    public PPUHeadless(MMU mmu) {
        super(mmu);
        pixels = new int[144][160];
    }

    public void tick(int cycles) {
        cycleCounter += cycles;
        switch (mode){
            case HBLANK: // HBlank
                if (cycleCounter >= CLOCKS_PER_HBLANK) {
                    writeScanline(scanLine);
                    scanLine++;
                    mmu.setLY(scanLine);

                    cycleCounter %= CLOCKS_PER_HBLANK;

                    if (scanLine == 144) {
                        mode = PPUMode.VBLANK;

                        mmu.setStatBit(0);
                        mmu.unSetStatBit(1);
                        mmu.setIFBit(0);

                    } else {
                        mmu.unSetStatBit(0);
                        mmu.setStatBit(1);
                        mode = PPUMode.ACCESS_OAM;
                    }
                }
                break;
            case VBLANK: //VBlank
                if (cycleCounter >= CLOCKS_PER_SCANLINE) {
                    scanLine++;
                    mmu.setLY(scanLine);

                    cycleCounter %= CLOCKS_PER_SCANLINE;

                    if (scanLine == 154) {
//                        sc.renderFrame();
                        printFrame();
                        scanLine = 0;
                        mmu.setLY(scanLine);
                        mode = PPUMode.ACCESS_OAM;
                        mmu.setStatBit(1);
                        mmu.unSetStatBit(0);
                    }
                }
                break;
            case ACCESS_OAM: // OAM
                if (cycleCounter >= CLOCKS_PER_SCANLINE_OAM) {
                    cycleCounter %= CLOCKS_PER_SCANLINE_OAM;
                    // Set LCD Controller stat bit
                    mmu.setStatBit(0);
                    mmu.setStatBit(1);

                    mode = PPUMode.ACCESS_VRAM;
                }
                break;
            case ACCESS_VRAM: /// VRAM
                if (cycleCounter >= CLOCKS_PER_SCANLINE_VRAM) {
                    cycleCounter %= CLOCKS_PER_SCANLINE_VRAM;
                    mode = PPUMode.HBLANK;

                    boolean hBlankInterrupt = mmu.getStatBit(3) == 1;
                    if (hBlankInterrupt) {
                        mmu.setIFBit(1);
                    }

                    boolean lyCoincidenceInterrupt = mmu.getStatBit(6) == 1;
                    boolean lyCoincidence = mmu.getLYComp() == scanLine;
                    if (lyCoincidenceInterrupt && lyCoincidence) {
                        mmu.setIFBit(1);
                    }
                    if (lyCoincidence) mmu.setStatBit(2);
                    else mmu.unSetStatBit(2);

                    mmu.unSetStatBit(1);
                    mmu.unSetStatBit(0);

                }
                break;
        }

    }

    @Override
    protected void drawBgLine(int line) {
        boolean useTileSetZero = mmu.bgWindowTileData();
        boolean useTileMapZero = !mmu.bgTileMapDisplay();

        int tileSetAddr = useTileSetZero ? TILE_SET_ZERO_ADDR : TILE_SET_ONE_ADDR;
        int tileMapAddr = useTileMapZero ? TILE_MAP_ZERO_ADDR : TILE_MAP_ONE_ADDR;

        for (int screenX = 0; screenX < 160; screenX++) {
            /* Work out the position of the pixel in the framebuffer */
//            System.out.println("scroll");
            int scrolledX = screenX + mmu.getScrollY();
            int scrolledY = line + mmu.getScrollX();

            /* Work out the index of the pixel in the full background map */
            int bgMapX = scrolledX % 256;
            int bgMapY = scrolledY % 256;

            /* Work out which tile of the bg_map this pixel is in, and the index of that tile
             * in the array of all tiles */
            int tileX = bgMapX / 8;
            int tileY = bgMapY / 8;

            /* Work out which specific (x,y) inside that tile we're going to render */
            int tilePixelX = bgMapX % 8;
            int tilePixelY = bgMapY % 8;

            /* Work out the address of the tile ID from the tile map */
            int tileIndex = tileY * 32 + tileX;
            int tileIdAddress = tileMapAddr + tileIndex;
//            System.out.println("TILE ID ADDR " + Integer.toHexString(tileIdAddress));
            /* Grab the ID of the tile we'll get data from in the tile map */
            int tileId = mmu.readByte(tileIdAddress);

//            System.out.println("TILE ID " + Integer.toHexString(tileId));


            /* Calculate the offset from the start of the tile data emulator.memory.memory where
             * the data for our tile lives */
            int tileDataMemOffset = useTileSetZero
                    ? tileId * 16
                    : (tileId + 128) * 16;


            /* Calculate the extra offset to the data for the line of pixels we
             * are rendering from.
             * 2 (bytes per line of pixels) * y (lines) */
            int tileDataLineOffset = tilePixelY * 2;

            int tileLineDataStartAddr = tileSetAddr + tileDataMemOffset + tileDataLineOffset;
//            System.out.println("TILE IS AT " + Integer.toHexString(tileLineDataStartAddr));

            /* FIXME: We fetch the full line of pixels for each pixel in the tile
             * we render. This could be altered to work in a way that avoids re-fetching
             * for a more performant renderer */
            int pixels1 = mmu.readByte(tileLineDataStartAddr);
            int pixels2 = mmu.readByte(tileLineDataStartAddr + 1);

            int color = getColorFromPixel(pixels1, pixels2, tilePixelX);
            pixels[line][screenX] = color;
//            sc.set(screenX, line, palette[color].getRGB());

        }
    }

    @Override
    protected void drawWindowLine(int line) {

    }

    private void printFrame() {
        System.out.println("NEW FRAME");
        String out = "";
        for (int y = 0; y < 144; y ++) {
            for (int x = 0; x < 160; x++) out = out.concat(pixels[y][x] + " ");
            out = out.concat("\n");
        }

        System.out.println(out);
    }
}
