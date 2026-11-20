import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Generates the README / CurseForge hero banner from docs/logo.png.
 *
 * Run from the repo root after regenerating the logo:
 *
 *     java tools/logo/MakeBanner.java
 */
public class MakeBanner {
	static final int W = 1200, H = 400;
	static final Color BG_TOP = new Color(196, 232, 255);   // same gradient as make_logo.py
	static final Color BG_BOTTOM = new Color(226, 212, 255);
	static final Color INK = new Color(36, 42, 66);
	static final Color INK_SOFT = new Color(72, 80, 110);
	static final int PIXEL = 6; // size of one "art pixel" in the title
	static final int TITLE_Y = 58;

	public static void main(String[] args) throws Exception {
		BufferedImage logo = ImageIO.read(new File("docs/logo.png"));
		BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

		g.setPaint(new GradientPaint(0, 0, BG_TOP, W, H, BG_BOTTOM));
		g.fill(new RoundRectangle2D.Float(0, 0, W, H, 64, 64));

		int logoSize = 300, logoX = 70, logoY = (H - logoSize) / 2;
		g.drawImage(logo, logoX, logoY, logoSize, logoSize, null);

		int textX = logoX + logoSize + 60;
		BufferedImage title = pixelText("Protractor", new Font("DejaVu Sans", Font.BOLD, 18), INK);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		int titleH = title.getHeight() * PIXEL;
		g.drawImage(title, textX, TITLE_Y, title.getWidth() * PIXEL, titleH, null);

		g.setColor(INK_SOFT);
		g.setFont(new Font("Ubuntu Sans", Font.PLAIN, 34));
		int lineY = TITLE_Y + titleH + 52;
		for (String line : new String[] {"See every block of a straight line,", "at any angle. Build diagonals, slopes", "and angled walls without guesswork."}) {
			g.drawString(line, textX + 4, lineY);
			lineY += 46;
		}
		g.dispose();
		ImageIO.write(out, "png", new File("docs/banner.png"));
	}

	/** Renders text small and without anti-aliasing, so scaling it up gives a blocky, Minecraft-like look. */
	static BufferedImage pixelText(String text, Font font, Color color) {
		BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		FontMetrics fm = probe.createGraphics().getFontMetrics(font);
		int w = fm.stringWidth(text) + 2, h = fm.getAscent() + fm.getDescent();
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g.setFont(font);
		g.setColor(color);
		g.drawString(text, 0, fm.getAscent());
		g.dispose();
		return img;
	}
}
