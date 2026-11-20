import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/** 原創幾何圖示，開發時產生後打包；網站執行時不需繪圖工具或外部圖片。 */
public class GenerateSeedImages {
    static final Color INK = new Color(22, 40, 60),
            GREEN = new Color(23, 107, 85),
            MINT = new Color(213, 237, 225);

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "src/main/resources/seed/images" : args[0]);
        Files.createDirectories(root);
        for (String name :
                new String[] {"java", "mvc", "database", "security", "tools", "markdown"}) {
            BufferedImage image = new BufferedImage(960, 480, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(241, 248, 245));
            g.fillRect(0, 0, 960, 480);
            g.setColor(MINT);
            g.fillOval(310, 70, 340, 340);
            g.setStroke(new BasicStroke(14, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(INK);
            switch (name) {
                case "java" -> {
                    g.drawRoundRect(365, 200, 205, 135, 25, 25);
                    g.drawArc(540, 212, 90, 100, 270, 180);
                    g.drawLine(350, 360, 595, 360);
                    g.setColor(GREEN);
                    g.draw(new CubicCurve2D.Double(420, 165, 375, 125, 465, 125, 425, 85));
                    g.draw(new CubicCurve2D.Double(505, 165, 460, 125, 550, 125, 510, 85));
                }
                case "mvc" -> {
                    for (int x : new int[] {180, 400, 620}) {
                        g.setColor(Color.WHITE);
                        g.fillRoundRect(x, 165, 160, 150, 20, 20);
                        g.setColor(INK);
                        g.drawRoundRect(x, 165, 160, 150, 20, 20);
                    }
                    g.setColor(GREEN);
                    g.drawLine(340, 240, 400, 240);
                    g.drawLine(560, 240, 620, 240);
                    for (int x : new int[] {225, 445, 665}) {
                        g.fillOval(x, 205, 70, 70);
                    }
                }
                case "database" -> {
                    g.setColor(GREEN);
                    g.fillRect(360, 160, 240, 170);
                    g.fillOval(360, 285, 240, 90);
                    g.setColor(INK);
                    g.drawLine(360, 160, 360, 330);
                    g.drawLine(600, 160, 600, 330);
                    g.drawArc(360, 215, 240, 90, 180, 180);
                    g.drawArc(360, 285, 240, 90, 180, 180);
                    g.setColor(MINT);
                    g.fillOval(360, 115, 240, 90);
                    g.setColor(INK);
                    g.drawOval(360, 115, 240, 90);
                }
                case "security" -> {
                    Path2D shield = new Path2D.Double();
                    shield.moveTo(480, 90);
                    shield.lineTo(610, 140);
                    shield.lineTo(595, 280);
                    shield.quadTo(580, 345, 480, 395);
                    shield.quadTo(380, 345, 365, 280);
                    shield.lineTo(350, 140);
                    shield.closePath();
                    g.setColor(GREEN);
                    g.fill(shield);
                    g.setColor(Color.WHITE);
                    g.drawLine(420, 240, 465, 285);
                    g.drawLine(465, 285, 545, 190);
                }
                case "tools" -> {
                    g.setColor(INK);
                    g.fillRoundRect(285, 125, 390, 240, 25, 25);
                    g.setColor(MINT);
                    g.fillOval(310, 145, 15, 15);
                    g.fillOval(340, 145, 15, 15);
                    g.fillOval(370, 145, 15, 15);
                    g.setColor(Color.WHITE);
                    g.drawLine(335, 210, 380, 245);
                    g.drawLine(380, 245, 335, 280);
                    g.setColor(new Color(111, 211, 166));
                    g.drawLine(420, 280, 540, 280);
                }
                case "markdown" -> {
                    g.setColor(Color.WHITE);
                    g.fillRoundRect(350, 85, 260, 320, 20, 20);
                    g.setColor(INK);
                    g.drawRoundRect(350, 85, 260, 320, 20, 20);
                    g.setColor(GREEN);
                    g.drawLine(395, 140, 555, 140);
                    g.drawLine(395, 190, 520, 190);
                    g.setColor(MINT);
                    g.fillRoundRect(390, 230, 180, 125, 12, 12);
                    g.setColor(GREEN);
                    g.drawLine(415, 305, 450, 265);
                    g.drawLine(450, 265, 490, 310);
                    g.drawLine(490, 310, 530, 280);
                }
            }
            g.dispose();
            ImageIO.write(image, "png", root.resolve(name + ".png").toFile());
        }
    }
}
