package ppu;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;

public class Screen extends JPanel {
    private final BufferedImage img;
    private JFrame frame;
    private final Color[] palette = {
            new Color(224, 248, 208),
            new Color(136, 192, 112),
            new Color(52,104,86),
            new Color(8, 24, 32)
    };

    public Screen() {
        frame = new JFrame("gb");
        img = new BufferedImage((160 * 4), (144 * 4), BufferedImage.TYPE_INT_RGB);
        ImageIcon icon = new ImageIcon( img );
        frame.setSize((160 * 4) + 6, (144 * 4) + 34);
        frame.add(new JLabel(icon));
        frame.pack();
        frame.setVisible(true);
        frame.setResizable(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
//        frame.addKeyListener(Joypad.getInstance());
    }

    public void set(int x, int y, int rgb) {
        img.setRGB(x, y, rgb);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.drawImage(img, null, null);
    }

    public void renderFrame(int[][] screen) {
        for (int x = 0; x < 144; x++) {
            for (int y = 0; y < 160; y++) {
                System.out.println(x+ " " + y);
                img.setRGB(y, x, palette[screen[x][y]].getRGB());
            }
        }

//        Graphics2D g2 = canvas.createGraphics();
//        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
//        g2.drawImage(img, 0, 0, 160*2, 144*2, null);

        frame.repaint();
    }

    public static void main(String[] args) {
        int[][] s = new int[160][144];
        for (int i = 0; i < 160; i++) {
            for (int j = 0; j < 144; j++) {
                s[i][j] = 2;
            }
        }
        Screen sc = new Screen();
        sc.renderFrame(s);
    }

}
