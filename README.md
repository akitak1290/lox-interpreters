# A tree-walk interpreter for the Lox language, built with Java using Bob Nystrom's book "Crafting Interpreters."
Lox is a simple interpreted, dynamically typed, general-purpose programming language that supports OOP to a limited extend. <br>

It leverages the JVM to support:
- Expressions, statements, and control flow structures.
- First-class functions.
- Classes and single inheritance.
- Garbage collection.
- Static and runtime error handling.

[designDocs](designDocs) is used to keep track of changes as the project progresses to its final form.

## Table of Contents
- [Setup](#setup)
- [Testing](#testing)
- [Bacis](#basics)
- [Titbits](#titbits)

## Getting Started
### Setup
Compiling and testing the interpreter is done through makefile scripts. You can clone the repo and run `make` to setup JDK and compile the project.

After, you can either try the interpreter in REPL mode.
```
./jlox
```
Or pass an argument to the script
```
./jlox add.lox
```
### Testing
Tests are setup through `make` scripts that runs the interpreter with `.test.lox` files under `/test` against `.result.lox` files. 

If you want to mess around with the project and want to make sure it stil works correctly, run `make test` or `make test FLAG=verbose` for more details.

### Basics
Currently, Lox supports 7 data types:
- nil
- boolean
- floats
- string
- array
- functions / native functions (`clock()`)
- classes

Variable and function are declared with the keyword `var` and `fun` respectively. <br>
```
var counter = 0;
fun makeCounter() {}
```
(do note that variables are not hoisted!)

Functions:
- Are first-class, so they are treated like other values.
- Take in parameters and return a value.
- Can create side-effects (like print to console).
- Have a reference to its outer scope to create closure.
```
fun makeCounter() {
  var i = 0;
  fun count() {
    i = i + 1;
    print i;
  }
  return count;
}
```

Lox supports OOP features, so it also has classes with properties and methods, inheritance, constructors, `this` and `super`.

```
class Doughnut {
  cook() {
    print "Fry until golden brown.";
  }
}

class BostonCream < Doughnut {
  cook() {
    super.cook();
    print "Pipe full of custard and coat with chocolate.";
  }
}

BostonCream().cook();
```
Lox class is a collection of functions, no properties. Properties of a class is declared inside the constructor:
```
init() {
  this.beverage = "cola";
}
```
New array is called by calling the native function `Array(2)` and pass in the size of the array.
```
var arr = Array(5);

var i = 0;

while (i < arr.length) {
        arr.set(i, i);
        i = i + 1;
}
```
### Native functions
- `clock()`: use to get the current time in seconds.
- `clear()`: use to clear the console.
- `Array()`: use to create a new array.

### Titbits
The complete syntax of Lox can be found [here](https://craftinginterpreters.com/appendix-i.html).

The currect project supports the following basic escape sequences
- `\n`
- `\t`
- `\r`
- `\b`
- `\\`

Strings are characters surrounded by double quotes, single quotes are not allowed, as well
as using double quote character as a string literal.

Numbers are represented internally using float.

Comments starts with `//`.

### 
