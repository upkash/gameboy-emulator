package ppu;

public class TileSet {
    private final Tile[] tiles;

    public TileSet() {
        tiles = new Tile[384];
    }

    public void setTileVal(int address, int value) {
        getTile(address).setValue(address,value);
    }

    public Tile getTileFromIndex(int idx) {
        return tiles[idx];
    }

    public Tile getTile(int address) {
        int idx = (address - 0x8000) / 16;
        return tiles[idx];
    }
}
