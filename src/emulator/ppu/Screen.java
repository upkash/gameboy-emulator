package emulator.ppu;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Screen extends JPanel {
    private final BufferedImage buffer;
    private final BufferedImage img;
    private final JFrame frame;

    public Screen() {
        frame = new JFrame("gb");
        buffer = new BufferedImage(160, 144, BufferedImage.TYPE_INT_RGB);
        img = new BufferedImage(160*4, 144*4, BufferedImage.TYPE_INT_RGB);
        ImageIcon icon = new ImageIcon( img );
        frame.setSize((160 * 4) + 6, (144 * 4) + 34);
        frame.add(new JLabel(icon));
        frame.pack();
        frame.setVisible(true);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    }

    public void set(int x, int y, int rgb) {
        buffer.setRGB(x, y, rgb);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.drawImage(img, null, null);
    }

    public void renderFrame() {
        Graphics2D g2 = img.createGraphics();
        g2.drawImage(buffer, 0, 0, 160*4, 144*4, null);
        frame.repaint();
    }
}
