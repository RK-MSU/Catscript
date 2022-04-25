# Catscript Documentation

Catscript is a simple scripting language that is statically typed and compiled to JVM bytecode.

## Features

Here are some features of the Catscript language.

### Comments

---

Commnets are used to better help document your code. To write a command in Catscript, the use of two forward slash charaters (`//`) is used.

    // this is a comment
    var a = 1

### Variable Statement

---

To declare a varible in Catscript, the keyword `var` is used.

    var x = 1

### Types

---

Catscript supports explicit typing. To declare a type the use of a colon character (`:`) followed by the desired type.

    var x : int = 1

> Note: If a type is not explicitly declared, then the type is inferred.

Here is a list of the supported types:

- int
- string
- bool
- list
- null
- object

### Print Statement

---

Catscript has a built in statement to send output to the console. The built-in statement has a reserved keyword `print`

    print(10) // will output: 10

### Math Operation

---

Catscript supports basic math operations.

| Operator | Name |
| :--: | :-- |
| `+` | Addition |
| `-` | Subtraction |
| `*` | Multiplication |
| `/` | Division |

#### Example

    print(2 + 1) // Will print out: 3
    print(2 - 1) // Will print out: 1
    print(2 * 1) // Will print out: 2
    print(2 / 1) // Will print out: 2

### Comparison

---

Catscript supports the following comparison operations.

| Operator | Name |
| :--: | :-- |
| `>` | Greater than |
| `>=` | Greater than or equal to |
| `<` | Less than |
| `<=` | Less than or equal to |

#### Example

    1 > 0   // true
    1 >= 0  // true
    1 < 0   // false
    1 <= 0  // false

### Equality

---

Catscript can check for the equality of two values that are of the same type.
These operands include the "equal equal" (`==`) and "bang equal" (`!=`).

    1 == 0 // false
    1 != 0 // true

### Unary Expression

---

There are two ways to negate a value depending on the type.
Variables use the minus symbol (`-`) and booleans use the `not` keyword.

#### Variables

Using the minus symbol character (`-`) variables will be negated.

    var x = 5
    var y = 10
    print(x + y)  // Will print out: 15
    print(x + -y) // Will print out: -5

#### Booleans

Using the keyword `not`, booleans will be negated.

    var x = true
    print(x)      // Will print out: true
    print(not x)  // Will print out: false

### For Statement

---

Catscript supports the operation of `for`-loops. Using the keyword `in`, a for loop will iterate of each of the items in a list.

    var list = [1, 2, 3]
    for (x in list) {
        print(x)
    }

### If-Else Statement

---

If statements are similar to those seen in most other common programming langauges. Using the keyword `if` followed by and expression and body statements. Catscript also support `if-else` and `else` operations.

    if ( expression ) {
        // statements
    } else if ( expression ) {
        // statements
    } else {
        // statements
    }

### Functions

---

Functions in Catscript are defined by using the keyword `function` before the idenitier name.

    function foo() {
        print("Hello World")
    }
    foo()
