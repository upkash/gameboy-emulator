package ppu;

public class Tile {
    private final int[][] pixels;
    private final int[][] data;

    public Tile() {
        pixels = new int[8][8];
        data = new int[8][2];
    }

    public Tile(int[] data) {
        assert data.length == 16;
        pixels = new int[8][8];
        this.data = new int[8][2];
        for (int i = 0; i < data.length - 1; i+=2) {
            this.data[i/2][0] = data[i];
            this.data[i/2][1] = data[i+1];
            pixels[i] = createRow(data[i], data[i+1]);
        }
    }

    public int[][] getPixels() {
        return pixels;
    }

    public void setValue(int address, int value) {
        int i = (address - 0x8000) - ((address - 0x8000) / 16) * 16;
        data[i/2][i%2] = value;
        pixels[i/2] = createRow(data[i/2][0], data[i/2][1]);
    }

    private int[] createRow(int b1, int b2) {
        int[] row = new int[8];
        String b1S = Integer.toBinaryString(b1);
        String b2S = Integer.toBinaryString(b2);

        for (int i = 0; i < b1S.length(); i++) {
            String curr_string = String.valueOf(b2S.charAt(i)) + b1S.charAt(i);
            int curr = Integer.parseInt(curr_string, 2);
            row[i] = curr;
        }
        return row;
    }
}
