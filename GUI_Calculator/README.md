# Java GUI Calculator

A clean, dark-themed desktop calculator built with **Core Java** and **Java Swing**.
No build tools, no frameworks, no external libraries – just `javac` and `java`.

It is a small, beginner-friendly project that shows how to separate GUI code from
calculation logic, handle button and keyboard events, and deal with invalid input gracefully.

## Features

- Modern dark UI with a large display and a small expression line (e.g. `12 + 5 =`)
- Rounded, color-coded buttons with hover and pressed effects
- Basic operations: addition, subtraction, multiplication, division
- Percentage (`%`), decimal point (`.`) and positive/negative toggle (`±`)
- `AC` (clear all) and `DEL` (delete last digit)
- Continuous calculations (`5 + 3 × 2 =` is evaluated step by step)
- Exact decimal maths using `BigDecimal` (`0.1 + 0.2 = 0.3`)
- Full keyboard support
- Friendly error messages instead of crashes:
  - division by zero
  - multiple decimal points (ignored)
  - incorrect operator sequences (the operator is replaced, e.g. `5 + ×` becomes `5 ×`)
  - empty input (treated as `0`)
- Resizable window with a minimum size; text and buttons scale with the window

## Technologies Used

- Java (JDK 17 or newer recommended)
- Java Swing (GUI components)
- Java AWT (layouts, colors, graphics, events)

## Project Structure

```
Java-GUI-Calculator/
│
├── src/
│   ├── Calculator.java         # Main window (GUI) and program entry point
│   ├── CalculatorEngine.java   # All calculation logic (no GUI code)
│   └── RoundedButton.java      # Custom rounded JButton
│
├── .gitignore
├── LICENSE
└── README.md
```

## Prerequisites

You need the **Java Development Kit (JDK) 17 or newer** (it must include `javac`).

Check whether it is already installed:

```bash
java -version
javac -version
```

If not, download a JDK from [Adoptium](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/).

## Installation

Clone the repository and move into the project folder:

```bash
git clone https://github.com/<your-username>/Java-GUI-Calculator.git
cd Java-GUI-Calculator
```

## How to Compile

From the project root folder:

```bash
javac -d out -sourcepath src src/Calculator.java
```

This compiles `Calculator.java` together with the other classes it uses and places the
`.class` files in a new `out` folder.

> On macOS/Linux you can also use `javac -d out src/*.java`.

## How to Run

```bash
java -cp out Calculator
```

## Usage

Click the buttons with the mouse, or use the keyboard.

| Action               | Mouse button | Keyboard                      |
|----------------------|--------------|-------------------------------|
| Digits               | `0`–`9`      | `0`–`9` (main keys or numpad) |
| Decimal point        | `.`          | `.` or `,`                    |
| Add / Subtract       | `+` / `−`    | `+` / `-`                     |
| Multiply / Divide    | `×` / `÷`    | `*` or `x` / `/`              |
| Percentage           | `%`          | `%`                           |
| Positive/negative    | `±`          | `n`                           |
| Calculate            | `=`          | `Enter` or `=`                |
| Delete last digit    | `DEL`        | `Backspace`                   |
| Clear all            | `AC`         | `Esc`, `Delete` or `c`        |

**Notes**

- `200 + 10 %` gives `220` (the percentage is taken from the first number after `+` or `−`).
- `200 × 10 %` gives `20` (the percentage simply divides by 100 for `×` and `÷`).
- Pressing `=` right after an operator (e.g. `5 + =`) uses the displayed number twice (`10`).
- After an error message, press `AC`, `DEL` or start typing a new number.

## Screenshots

> Screenshots will be added soon.

| Normal                                | Error message                         |
|---------------------------------------|---------------------------------------|
| _screenshots/calculator.png (coming)_ | _screenshots/error.png (coming)_      |

## Future Improvements

- Scientific functions (square root, powers, trigonometry)
- Calculation history panel
- Memory buttons (`M+`, `M-`, `MR`, `MC`)
- Light/dark theme switch
- Copy and paste support for the display
- Repeat the last operation when `=` is pressed again
- Unit tests for `CalculatorEngine`

## License

This project is licensed under the **MIT License** – see the [LICENSE](LICENSE) file for details.
