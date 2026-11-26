import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;

/**
 * A JButton that paints itself as a rounded rectangle.
 *
 * It changes shade when the mouse hovers over it or when it is pressed,
 * and its text grows or shrinks together with the button size.
 */
public class RoundedButton extends JButton {

    private final Color baseColor;
    private final Font baseFont;

    public RoundedButton(String text, Color background, Color foreground, Font font) {
        super(text);
        this.baseColor = background;
        this.baseFont = font;

        setForeground(foreground);
        setFont(font);
        setContentAreaFilled(false);  // we paint the background ourselves
        setBorderPainted(false);
        setFocusPainted(false);
        setFocusable(false);          // keeps keyboard input working for the whole window
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Pick the fill colour depending on the button state.
        Color fill = baseColor;
        if (getModel().isPressed()) {
            fill = blend(baseColor, Color.BLACK, 0.20f);
        } else if (getModel().isRollover()) {
            fill = blend(baseColor, Color.WHITE, 0.15f);
        }

        // Rounded background.
        int arc = (int) (Math.min(width, height) * 0.40);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, width, height, arc, arc);

        // Centered text; the font scales with the button size.
        float fontSize = Math.max(14f, Math.min(width, height) * 0.36f);
        g2.setFont(baseFont.deriveFont(fontSize));
        FontMetrics metrics = g2.getFontMetrics();
        int textX = (width - metrics.stringWidth(getText())) / 2;
        int textY = (height - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.setColor(getForeground());
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }

    /** Mixes two colours. amount = 0 gives 'from', amount = 1 gives 'to'. */
    private static Color blend(Color from, Color to, float amount) {
        int r = (int) (from.getRed() + (to.getRed() - from.getRed()) * amount);
        int g = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * amount);
        int b = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * amount);
        return new Color(r, g, b);
    }
}
