# Lua Lexer and Parser

## Build

```bash
javac -d bin src/*.java
```

## General CLI

```bash
java -cp bin App tokens input.lua
java -cp bin App symbols input.lua
java -cp bin App parse input.lua
```

- `tokens` prints the lexical analysis table.
- `symbols` prints the symbol table with detected types and scopes.
- `parse` prints the parse tree, the line-by-line statement report, and the final syntax result.

## Parser Submission Runner

```bash
java -cp bin projectreqt7_BACSAIN projectreqt7_BACSAIN_input.lua
```

This runner prints the token stream, parse tree, line-by-line report, and syntax analysis result in one command for the parser submission flow.

## Automated Parser Checks

```bash
java -cp bin ParserTestHarness
```

The regression harness runs five parser checks automatically:
- valid local assignment
- valid `if / then / end` block
- grouped arithmetic precedence
- missing `end` block
- unexpected token after `if`

## Notes

- The parser consumes the `List<Token>` produced by the lexer. It does not re-lex input or use a lazy token stream.
- Syntax errors report the line number, the expected construct, the found lexeme, and the found token type.
- Semicolons are optional in Lua. In this project, `;` is accepted as an empty statement and as a table-constructor separator, but it is not required to terminate statements.

## Documentation

See GRAMMAR.md for the implemented parser grammar in simplified EBNF.