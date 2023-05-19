package ppu;

public class TileMap {
    private TileSet tileSet;
    private int[][] tileMap;
    public TileMap(TileSet tileSet) {
        this.tileSet = tileSet;
        tileMap = new int[32][32];
    }

    public void updateTileMap(int i, int j,  int value) {
//        System.out.println(i + " " + j);
        tileMap[i][j] = value;
    }

//    public int[][] getLine(int i, int scrollX, int scrollY) {
//        int[][][] line = new int[20][8][8];
//        i %= 32;
//        int address = 0x9800 + ((i + scrollY) & 0xFF) / 8 * 32 + scrollX / 8;
//        for (int i = 0; i < 20; i++) {
//            int tileIdx = mmu.readByte(address+i);
//            line[i] = mmu.getTileFromIndex(tileIdx).getPixels();
//        }
//
//        int[][] mergedLine = new int [8][160];
//        for (int i = 0; i < line.length; i++) {
//            int yStart = i * 8;
//            for (int row = 0; row < line[i].length; row++) {
//                System.arraycopy(line[i][row],0, mergedLine[row], yStart, 8);
//            }
//        }
//        if (currScanLine < 144) currScanLine++;
//        else currScanLine = 0;
//    }
}
