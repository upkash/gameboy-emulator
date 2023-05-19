package ppu;

public class TileMapContainer {
    private final TileMap tileMap0;
    private final TileMap tileMap1;
    private final TileSet tileSet;

    public TileMapContainer() {
        tileSet = new TileSet();
        tileMap0 = new TileMap(tileSet);
        tileMap1 = new TileMap(tileSet);
    }

    public TileMapContainer(TileSet tileSet) {
        this.tileSet = tileSet;
        tileMap0 = new TileMap(tileSet);
        tileMap1 = new TileMap(tileSet);
    }

    public void updateTileMap(int address, int value) {
        if (address >= 0x9800 && address <= 0x9BFF) {
            int i = (address - 0x9800) / 32;
            int j = (address - 0x9800) % 32;
            tileMap0.updateTileMap(i, j, value);
        } else if (address >= 0x9C00 && address <= 0x9FFF) {
            int i = (address - 0x9C00) / 32;
            int j = (address - 0x9C00) % 32;
            tileMap1.updateTileMap(i, j, value);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }
}
