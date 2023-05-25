package emulator.ppu;
import emulator.memory.MMU;

public class PPUHead extends PPU {
    private final Screen sc;
    public PPUHead(MMU mmu) {
        super(mmu);
        sc = new Screen();
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
                        renderFrame();
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
            int scrolledX = screenX + mmu.getScrollX();
            int scrolledY = line +  mmu.getScrollY();

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
//            pixels[line][screenX] = color;
            sc.set(screenX, line, palette[color].getRGB());

        }
    }

    @Override
    protected void drawWindowLine(int line) {
        boolean useTileSetZero = mmu.bgWindowTileData();
        boolean useTileMapZero = !mmu.bgTileMapDisplay();

        int tileSetAddr = useTileSetZero ? TILE_SET_ZERO_ADDR : TILE_SET_ONE_ADDR;
        int tileMapAddr = useTileMapZero ? TILE_MAP_ZERO_ADDR : TILE_MAP_ONE_ADDR;

        int screenY = scanLine;
        int scrolledY = screenY - mmu.getWindowY();

        if (scrolledY >= 144) { return; }
        // if (!is_on_screenY(scrolledY)) { return; }

        for (int screenX = 0; screenX < 160; screenX++) {
            /* Work out the position of the pixel in the framebuffer */
            int scrolledX = screenX + mmu.getWindowX() - 7;

            /* Work out which tile of the bg_map this pixel is in, and the index of that tile
             * in the array of all tiles */
            int tileX = scrolledX / 8;
            int tileY = scrolledY / 8;

            /* Work out which specific (x,y) inside that tile we're going to render */
            int tilePixelX = scrolledX % 8;
            int tilePixelY = scrolledY % 8;

            /* Work out the address of the tile ID from the tile map */
            int tileIndex = tileY * 32 + tileX;
            int tileIdAddress = tileMapAddr + tileIndex;

            /* Grab the ID of the tile we'll get data from in the tile map */
            int tileId = mmu.readByte(tileIdAddress);

            /* Calculate the offset from the start of the tile data memory where
             * the data for our tile lives */
            int tileDataMemOffset = useTileSetZero
                    ? tileId * 16
                    : (tileId + 128) * 16;

            /* Calculate the extra offset to the data for the line of pixels we
             * are rendering from.
             * 2 (bytes per line of pixels) * y (lines) */
            int tileDataLineOffset = tilePixelY * 2;

            int tileLineDataStartAddress = tileSetAddr + tileDataMemOffset + tileDataLineOffset;

            /* FIXME: We fetch the full line of pixels for each pixel in the tile
             * we render. This could be altered to work in a way that avoids re-fetching
             * for a more performant renderer */
            int pixels1 = mmu.readByte(tileLineDataStartAddress);
            int pixels2 = mmu.readByte(tileLineDataStartAddress + 1);

            int color = getColorFromPixel(pixels1, pixels2, tilePixelX);

            sc.set(screenX, screenY, palette[color].getRGB());
        }
    }

    protected void renderFrame() {
        sc.renderFrame();
    }
}
