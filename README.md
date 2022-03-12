# `jlox`
Implementation of the Lox language in Java, from the book [Crafting Interpreters](http://craftinginterpreters.com/).

This version of the Lox language is a naive tree-walker interpreter, with the following features:
* tokens and lexing
* recursive descent parsing
* prefix and infix expressions
* interpreting code using the Visitor pattern
* lexical scope
* environment chains for storing variables
* control flow and iteration
* first class functions and classes
* closures
* static variable resolution and error detection
* class with constructor, methods, and fields
* class inheritance

# Build
Install the source code using git, and run `make`.
```bash
$ git clone https://github.com/pacokwon/jlox
$ cd jlox
$ make
```

# Run
First, create a `lox` source file.
```bash
$ echo 'print "Hello Lox!";' > test.lox
```

Navigate to the project root directory and pass a filename to `lox`.
```bash
$ echo 'print "Hello Lox!";' > test.lox
$ ./lox test.lox
Hello Lox!
```
