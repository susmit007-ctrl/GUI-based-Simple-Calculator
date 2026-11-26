import java.math.BigDecimal;
import java.math.MathContext;

/**
 * CalculatorEngine contains ALL of the calculator's logic.
 *
 * It knows nothing about Swing or buttons. The GUI simply tells the engine
 * what the user pressed (a digit, an operator, "=", ...) and then asks the
 * engine what should be shown on the display.
 *
 * Keeping the logic separate from the GUI makes the code easier to read,
 * easier to test and easier to change later.
 *
 * Numbers are stored as BigDecimal (not double) so that decimal maths is
 * exact for everyday use, e.g. 0.1 + 0.2 gives 0.3 instead of 0.30000000000000004.
 */
public class CalculatorEngine {

    /** Results are rounded to 15 significant digits. */
    private static final MathContext PRECISION = new MathContext(15);

    /** Maximum number of digits the user can type into a single number. */
    private static final int MAX_INPUT_DIGITS = 15;

    /** Friendly error messages shown on the display. */
    public static final String ERROR_DIVIDE_BY_ZERO = "Cannot divide by zero";
    public static final String ERROR_INVALID = "Invalid input";

    /** Operator symbols used for the small "expression" line (unicode escapes keep the file encoding-safe). */
    private static final String SYMBOL_PLUS = "+";
    private static final String SYMBOL_MINUS = "\u2212";    // −
    private static final String SYMBOL_MULTIPLY = "\u00D7"; // ×
    private static final String SYMBOL_DIVIDE = "\u00F7";   // ÷

    // ----- Calculator state -----

    /** The text currently shown on the main display (what the user is typing, or a result). */
    private String currentInput = "0";

    /** The first operand, saved when the user presses an operator. */
    private BigDecimal storedValue = null;

    /** The operator waiting to be applied: '+', '-', '*' or '/'. 0 means "no operator". */
    private char pendingOperator = 0;

    /** If true, the next digit typed replaces the display instead of being appended. */
    private boolean startNewInput = true;

    /** If true, an operator was just pressed and the second number has not been typed yet. */
    private boolean awaitingOperand = false;

    /** If true, the display is showing an error message. */
    private boolean errorState = false;

    /** The small line above the main display, e.g. "12 + 5 =". */
    private String expression = "";

    // ------------------------------------------------------------------
    // Getters used by the GUI
    // ------------------------------------------------------------------

    public String getDisplay() {
        return currentInput;
    }

    public String getExpression() {
        return expression;
    }

    public boolean isError() {
        return errorState;
    }

    // ------------------------------------------------------------------
    // Actions triggered by the buttons / keyboard
    // ------------------------------------------------------------------

    /** Handles the digits 0-9. */
    public void inputDigit(char digit) {
        if (errorState) {
            clearAll();
        }

        if (startNewInput) {
            currentInput = String.valueOf(digit);
            startNewInput = false;
            if (pendingOperator == 0) {
                expression = ""; // a brand new calculation begins
            }
        } else if (currentInput.equals("0")) {
            currentInput = String.valueOf(digit);          // avoid leading zeros like "007"
        } else if (currentInput.equals("-0")) {
            currentInput = "-" + digit;
        } else if (countDigits(currentInput) < MAX_INPUT_DIGITS) {
            currentInput += digit;
        }
        awaitingOperand = false;
    }

    /** Handles the decimal point. A number can only contain one. */
    public void inputDecimal() {
        if (errorState) {
            clearAll();
        }

        if (startNewInput) {
            currentInput = "0.";
            startNewInput = false;
            awaitingOperand = false;
            if (pendingOperator == 0) {
                expression = "";
            }
        } else if (!currentInput.contains(".")) {
            currentInput += ".";
        }
        // else: a second decimal point is simply ignored
    }

    /**
     * Handles + - * / (continuous calculation supported).
     * Pressing 5 + 3 + will first calculate 8 and then wait for the next number.
     */
    public void chooseOperator(char operator) {
        if (errorState) {
            return; // the user must press AC (or type a number) first
        }

        // Incorrect operator sequence such as "5 + *": just replace the operator.
        if (pendingOperator != 0 && awaitingOperand) {
            pendingOperator = operator;
            expression = format(storedValue) + " " + symbolFor(operator);
            return;
        }

        try {
            BigDecimal currentValue = parse(currentInput);
            if (pendingOperator != 0) {
                storedValue = applyOperator(storedValue, pendingOperator, currentValue);
            } else {
                storedValue = currentValue;
            }
            pendingOperator = operator;
            currentInput = format(storedValue);
            expression = currentInput + " " + symbolFor(operator);
            startNewInput = true;
            awaitingOperand = true;
        } catch (ArithmeticException | NumberFormatException e) {
            showError(messageFor(e));
        }
    }

    /**
     * Handles "=".
     * If no second number was typed (e.g. "5 + ="), the displayed number is used twice.
     */
    public void calculate() {
        if (errorState || pendingOperator == 0) {
            return; // nothing to calculate
        }

        try {
            BigDecimal rightValue = parse(currentInput);
            BigDecimal result = applyOperator(storedValue, pendingOperator, rightValue);

            expression = format(storedValue) + " " + symbolFor(pendingOperator)
                    + " " + format(rightValue) + " =";
            currentInput = format(result);

            storedValue = null;
            pendingOperator = 0;
            startNewInput = true;
            awaitingOperand = false;
        } catch (ArithmeticException | NumberFormatException e) {
            showError(messageFor(e));
        }
    }

    /**
     * Handles "%".
     *  - After + or -  : percentage of the first number (200 + 10% -> 200 + 20)
     *  - Otherwise     : divide by 100 (50% -> 0.5, 200 x 10% -> 200 x 0.1)
     */
    public void percent() {
        if (errorState) {
            return;
        }

        try {
            BigDecimal value = parse(currentInput);
            BigDecimal result;

            if (pendingOperator != 0 && storedValue != null
                    && (pendingOperator == '+' || pendingOperator == '-')) {
                result = storedValue.multiply(value, PRECISION).divide(BigDecimal.valueOf(100), PRECISION);
            } else {
                result = value.divide(BigDecimal.valueOf(100), PRECISION);
            }

            if (pendingOperator == 0) {
                expression = format(value) + " %";
            }
            currentInput = format(result);
            startNewInput = true;
            awaitingOperand = false;
        } catch (ArithmeticException | NumberFormatException e) {
            showError(messageFor(e));
        }
    }

    /** Handles the +/- button: flips the sign of the displayed number. */
    public void toggleSign() {
        if (errorState) {
            return;
        }

        try {
            if (parse(currentInput).signum() == 0) {
                return; // "-0" makes no sense
            }
        } catch (NumberFormatException e) {
            showError(ERROR_INVALID);
            return;
        }

        if (currentInput.startsWith("-")) {
            currentInput = currentInput.substring(1);
        } else {
            currentInput = "-" + currentInput;
        }
        awaitingOperand = false;
    }

    /** Handles DEL: removes the last typed character. */
    public void delete() {
        if (errorState) {
            clearAll();
            return;
        }
        if (startNewInput) {
            return; // results and operators cannot be edited digit by digit
        }

        currentInput = currentInput.substring(0, currentInput.length() - 1);
        if (currentInput.isEmpty() || currentInput.equals("-")) {
            currentInput = "0";
        }
    }

    /** Handles AC: resets everything. */
    public void clearAll() {
        currentInput = "0";
        storedValue = null;
        pendingOperator = 0;
        startNewInput = true;
        awaitingOperand = false;
        errorState = false;
        expression = "";
    }

    // ------------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------------

    /** Performs one calculation. Throws ArithmeticException for division by zero. */
    private BigDecimal applyOperator(BigDecimal left, char operator, BigDecimal right) {
        switch (operator) {
            case '+':
                return left.add(right, PRECISION);
            case '-':
                return left.subtract(right, PRECISION);
            case '*':
                return left.multiply(right, PRECISION);
            case '/':
                if (right.signum() == 0) {
                    throw new ArithmeticException(ERROR_DIVIDE_BY_ZERO);
                }
                return left.divide(right, PRECISION);
            default:
                throw new ArithmeticException(ERROR_INVALID);
        }
    }

    /** Converts display text to a number ("5." and "-0.5" are both valid). */
    private BigDecimal parse(String text) {
        return new BigDecimal(text);
    }

    /**
     * Converts a number to display text: no trailing zeros (2.50 -> 2.5),
     * and scientific notation only for extremely large or small values.
     */
    private String format(BigDecimal value) {
        BigDecimal rounded = value.round(PRECISION);
        if (rounded.signum() == 0) {
            return "0";
        }
        rounded = rounded.stripTrailingZeros();

        BigDecimal abs = rounded.abs();
        boolean tooBig = abs.compareTo(new BigDecimal("1E15")) >= 0;
        boolean tooSmall = abs.compareTo(new BigDecimal("1E-10")) < 0;
        return (tooBig || tooSmall) ? rounded.toString() : rounded.toPlainString();
    }

    private String symbolFor(char operator) {
        switch (operator) {
            case '+': return SYMBOL_PLUS;
            case '-': return SYMBOL_MINUS;
            case '*': return SYMBOL_MULTIPLY;
            default:  return SYMBOL_DIVIDE;
        }
    }

    /** Chooses the text for an error: division by zero gets its own message. */
    private String messageFor(RuntimeException e) {
        return ERROR_DIVIDE_BY_ZERO.equals(e.getMessage()) ? ERROR_DIVIDE_BY_ZERO : ERROR_INVALID;
    }

    /** Puts the calculator into the error state and shows a message. */
    private void showError(String message) {
        errorState = true;
        currentInput = message;
        expression = "";
        storedValue = null;
        pendingOperator = 0;
        startNewInput = true;
        awaitingOperand = false;
    }

    /** Counts only the digit characters (ignores "-" and "."). */
    private int countDigits(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (Character.isDigit(c)) {
                count++;
            }
        }
        return count;
    }
}
