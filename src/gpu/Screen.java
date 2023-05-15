package gpu;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Screen extends JPanel {
    private final BufferedImage img;
    private final Color[] pallette = {new Color(8, 24, 32), new Color(52,104,86), new Color(136, 192, 112), new Color(224, 248, 208)};
    public Screen() {
        img = new BufferedImage(160, 144, BufferedImage.TYPE_INT_RGB);
        ImageIcon icon = new ImageIcon(img);
        add(new JLabel(icon));
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(img, 0, 0, this);
    }

    public void setLine(int[] values, int currScanLine) {
        for (int i = 0; i < values.length; i++) {
            img.setRGB(currScanLine, i, pallette[values[i]].getRGB());
        }
    }

    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.add(new Screen());
        f.setLocationByPlatform(true);
        f.pack();
        f.setVisible( true );

    }
}
