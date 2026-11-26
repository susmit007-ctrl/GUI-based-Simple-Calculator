import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Java GUI Calculator - the main window (the "view").
 *
 * This class builds the dark-themed interface and forwards every button click
 * and key press to CalculatorEngine, which does the real work.
 *
 * Run it with:  java -cp out Calculator
 */
public class Calculator extends JFrame {

    // ----- Colour scheme (Catppuccin-inspired dark theme) -----
    private static final Color BACKGROUND_COLOR = new Color(0x1E1E2E);
    private static final Color DISPLAY_COLOR = new Color(0x313244);
    private static final Color NUMBER_COLOR = new Color(0x45475A);
    private static final Color FUNCTION_COLOR = new Color(0x585B70);
    private static final Color OPERATOR_COLOR = new Color(0x89B4FA);
    private static final Color EQUALS_COLOR = new Color(0xA6E3A1);
    private static final Color CLEAR_COLOR = new Color(0xF38BA8);
    private static final Color TEXT_COLOR = new Color(0xCDD6F4);
    private static final Color SUBTLE_TEXT_COLOR = new Color(0x9399B2);

    // ----- Symbols (unicode escapes keep the source file encoding-safe) -----
    private static final String MULTIPLY = "\u00D7";   // ×
    private static final String DIVIDE = "\u00F7";     // ÷
    private static final String MINUS = "\u2212";      // −
    private static final String PLUS_MINUS = "\u00B1"; // ±

    /**
     * Button layout, row by row: { text shown on the button, command sent to handleCommand() }.
     * Changing this table changes the keypad.
     */
    private static final String[][] BUTTONS = {
        {"AC", "AC"},       {"DEL", "DEL"}, {"%", "%"},   {DIVIDE, "/"},
        {"7", "7"},         {"8", "8"},     {"9", "9"},   {MULTIPLY, "*"},
        {"4", "4"},         {"5", "5"},     {"6", "6"},   {MINUS, "-"},
        {"1", "1"},         {"2", "2"},     {"3", "3"},   {"+", "+"},
        {PLUS_MINUS, "NEG"}, {"0", "0"},    {".", "."},   {"=", "="}
    };

    private final CalculatorEngine engine = new CalculatorEngine();
    private final Font baseFont = pickFont();

    private final JLabel expressionLabel = new JLabel(" ", SwingConstants.RIGHT);
    private final JLabel displayLabel = new JLabel("0", SwingConstants.RIGHT);

    // ------------------------------------------------------------------
    // Window construction
    // ------------------------------------------------------------------

    public Calculator() {
        super("Java GUI Calculator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(320, 480));
        setSize(380, 600);
        setLocationRelativeTo(null); // center on screen

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(BACKGROUND_COLOR);
        content.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        content.add(createDisplayPanel(), BorderLayout.NORTH);
        content.add(createButtonPanel(), BorderLayout.CENTER);
        setContentPane(content);

        setupKeyboardSupport();
        refreshDisplay();
    }

    /** Builds the rounded display area (small expression line + big number). */
    private JPanel createDisplayPanel() {
        // A panel that paints a rounded background.
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(DISPLAY_COLOR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        panel.setPreferredSize(new Dimension(0, 140));

        expressionLabel.setFont(baseFont.deriveFont(Font.PLAIN, 18f));
        expressionLabel.setForeground(SUBTLE_TEXT_COLOR);

        displayLabel.setFont(baseFont.deriveFont(Font.PLAIN, 48f));
        displayLabel.setForeground(TEXT_COLOR);

        // Re-fit the text whenever the label changes size (window resized).
        displayLabel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                fitDisplayFont();
            }
        });

        panel.add(expressionLabel, BorderLayout.NORTH);
        panel.add(displayLabel, BorderLayout.CENTER);
        return panel;
    }

    /** Builds the 5 x 4 grid of buttons from the BUTTONS table. */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 4, 10, 10));
        panel.setOpaque(false);

        for (String[] definition : BUTTONS) {
            String text = definition[0];
            String command = definition[1];

            RoundedButton button = new RoundedButton(
                    text, backgroundFor(command), foregroundFor(command), baseFont.deriveFont(Font.BOLD));
            button.addActionListener(event -> handleCommand(command));
            panel.add(button);
        }
        return panel;
    }

    // ------------------------------------------------------------------
    // Handling input (buttons AND keyboard end up here)
    // ------------------------------------------------------------------

    /**
     * Sends a command to the engine and refreshes the screen.
     * Commands: "0"-"9", ".", "+", "-", "*", "/", "%", "=", "AC", "DEL", "NEG".
     */
    private void handleCommand(String command) {
        switch (command) {
            case "AC":
                engine.clearAll();
                break;
            case "DEL":
                engine.delete();
                break;
            case "%":
                engine.percent();
                break;
            case "NEG":
                engine.toggleSign();
                break;
            case "=":
                engine.calculate();
                break;
            case ".":
                engine.inputDecimal();
                break;
            case "+":
            case "-":
            case "*":
            case "/":
                engine.chooseOperator(command.charAt(0));
                break;
            default: // digits
                engine.inputDigit(command.charAt(0));
                break;
        }
        refreshDisplay();
    }

    /** Registers keyboard shortcuts for the whole window. */
    private void setupKeyboardSupport() {
        // Digits (main keyboard and numpad both produce these characters)
        for (char digit = '0'; digit <= '9'; digit++) {
            bindKey(KeyStroke.getKeyStroke(digit), String.valueOf(digit));
        }

        // Operators and symbols
        bindKey(KeyStroke.getKeyStroke('.'), ".");
        bindKey(KeyStroke.getKeyStroke(','), ".");
        bindKey(KeyStroke.getKeyStroke('+'), "+");
        bindKey(KeyStroke.getKeyStroke('-'), "-");
        bindKey(KeyStroke.getKeyStroke('*'), "*");
        bindKey(KeyStroke.getKeyStroke('x'), "*");
        bindKey(KeyStroke.getKeyStroke('X'), "*");
        bindKey(KeyStroke.getKeyStroke('/'), "/");
        bindKey(KeyStroke.getKeyStroke('%'), "%");
        bindKey(KeyStroke.getKeyStroke('='), "=");
        bindKey(KeyStroke.getKeyStroke('n'), "NEG");
        bindKey(KeyStroke.getKeyStroke('N'), "NEG");
        bindKey(KeyStroke.getKeyStroke('c'), "AC");
        bindKey(KeyStroke.getKeyStroke('C'), "AC");

        // Special keys
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "=");
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "DEL");
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "AC");
        bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "AC");
    }

    /** Connects one key to one command using Swing key bindings. */
    private void bindKey(KeyStroke keyStroke, String command) {
        String actionName = "calculator-" + keyStroke;
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionName);
        getRootPane().getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCommand(command);
            }
        });
    }

    // ------------------------------------------------------------------
    // Display helpers
    // ------------------------------------------------------------------

    /** Copies the engine's current state onto the labels. */
    private void refreshDisplay() {
        displayLabel.setText(engine.getDisplay());
        displayLabel.setForeground(engine.isError() ? CLEAR_COLOR : TEXT_COLOR);

        // A single space keeps the label's height stable when there is no expression.
        String expression = engine.getExpression();
        expressionLabel.setText(expression.isEmpty() ? " " : expression);

        fitDisplayFont();
    }

    /** Shrinks the display font until the text fits, so long numbers never get cut off. */
    private void fitDisplayFont() {
        int availableWidth = displayLabel.getWidth();
        if (availableWidth <= 0) {
            return; // not laid out yet; the resize listener will call us again
        }

        String text = displayLabel.getText();
        for (float size = 48f; size >= 16f; size -= 2f) {
            Font candidate = baseFont.deriveFont(Font.PLAIN, size);
            if (displayLabel.getFontMetrics(candidate).stringWidth(text) <= availableWidth) {
                displayLabel.setFont(candidate);
                return;
            }
        }
        displayLabel.setFont(baseFont.deriveFont(Font.PLAIN, 16f));
    }

    /** Returns the button colour for a command. */
    private Color backgroundFor(String command) {
        switch (command) {
            case "AC":
            case "DEL":
                return CLEAR_COLOR;
            case "=":
                return EQUALS_COLOR;
            case "+":
            case "-":
            case "*":
            case "/":
            case "%":
                return OPERATOR_COLOR;
            case "NEG":
                return FUNCTION_COLOR;
            default:
                return NUMBER_COLOR;
        }
    }

    /** Light buttons get dark text for contrast; dark buttons get light text. */
    private Color foregroundFor(String command) {
        Color background = backgroundFor(command);
        boolean isDarkButton = background == NUMBER_COLOR || background == FUNCTION_COLOR;
        return isDarkButton ? TEXT_COLOR : BACKGROUND_COLOR;
    }

    /** Picks a nice font that exists on this computer, falling back to Java's default sans-serif. */
    private static Font pickFont() {
        String[] preferred = {"Segoe UI", "SF Pro Display", "Helvetica Neue", "Roboto", "Ubuntu", "Noto Sans"};
        java.util.List<String> installed = Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String name : preferred) {
            if (installed.contains(name)) {
                return new Font(name, Font.PLAIN, 16);
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 16);
    }

    // ------------------------------------------------------------------
    // Program entry point
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        // Swing must be created on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> new Calculator().setVisible(true));
    }
}
