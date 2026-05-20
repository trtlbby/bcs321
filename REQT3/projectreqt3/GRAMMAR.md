# Parser Grammar Specification

This document describes the implemented grammar of the parser in `src/Parser.java`.
It is a simplified EBNF-style description of what the current project accepts.
It is not a full Lua grammar.

## Scope

The grammar below documents the parser as implemented.
It includes only constructs that are actually parsed by the current code.

Supported high-level constructs:
- empty statements
- local declarations
- local function declarations
- function declarations
- assignments
- function-call statements
- if / elseif / else blocks
- numeric for loops
- while loops
- repeat-until loops
- return statements
- expressions with precedence
- table constructors using value lists

Recognized lexically but not parsed as statements:
- `break`
- `goto`
- `for ... in ...`

## Entry Point

```ebnf
program ::= block EOF

block ::= { statement }
```

`block` stops when the parser encounters one of the block-ending keywords used by the implementation:
`end`, `else`, `elseif`, or `until`.

## Statements

```ebnf
statement ::= ";"
            | localDecl
            | ifStmt
            | forStmt
            | whileStmt
            | repeatStmt
            | funcDecl
            | returnStmt
            | exprStatement
```

```ebnf
localDecl ::= "local" "function" IDENTIFIER "(" paramList ")" block "end"
            | "local" IDENTIFIER [ "=" expression ]
```

```ebnf
ifStmt ::= "if" expression "then" block
           { "elseif" expression "then" block }
           [ "else" block ]
           "end"
```

```ebnf
forStmt ::= "for" IDENTIFIER "=" expression "," expression
            [ "," expression ]
            "do" block "end"
```

```ebnf
whileStmt ::= "while" expression "do" block "end"
```

```ebnf
repeatStmt ::= "repeat" block "until" expression
```

```ebnf
funcDecl ::= "function" IDENTIFIER "(" paramList ")" block "end"
```

```ebnf
returnStmt ::= "return" [ expression ]
```

```ebnf
exprStatement ::= assignStmt | callStmt

assignStmt ::= prefixExpr "=" expression

callStmt ::= callPrefix
```

## Prefix Expressions, Calls, and Lists

```ebnf
prefixExpr ::= IDENTIFIER { suffix }

suffix ::= "." IDENTIFIER
         | "[" expression "]"
         | ":" IDENTIFIER "(" argList ")"
         | "(" argList ")"
```

The implementation treats a prefix expression as a callable form only when at least one call-style suffix appears:
either `("...`)` or `:name(...)`.

```ebnf
callPrefix ::= IDENTIFIER { nonCallSuffix } callSuffix { suffix }

nonCallSuffix ::= "." IDENTIFIER
                | "[" expression "]"

callSuffix ::= ":" IDENTIFIER "(" argList ")"
             | "(" argList ")"
```

```ebnf
argList ::= [ expression { "," expression } ]
```

```ebnf
paramList ::= [ IDENTIFIER { "," IDENTIFIER } ]
```

## Expressions

The parser uses layered precedence rules.
The grammar below mirrors the implementation order from lowest precedence to highest precedence.

```ebnf
expression ::= orExpr
```

```ebnf
orExpr ::= andExpr { "or" andExpr }
```

```ebnf
andExpr ::= compExpr { "and" compExpr }
```

```ebnf
compExpr ::= concatExpr { compOp concatExpr }
```

```ebnf
concatExpr ::= addExpr { ".." addExpr }
```

```ebnf
addExpr ::= mulExpr { addOp mulExpr }
```

```ebnf
mulExpr ::= unaryExpr { mulOp unaryExpr }
```

```ebnf
unaryExpr ::= unaryOp unaryExpr | primary
```

```ebnf
primary ::= INTEGER_LITERAL
          | FLOAT_LITERAL
          | HEX_LITERAL
          | STRING_LITERAL
          | BOOLEAN_LITERAL
          | NIL
          | prefixExpr
          | "(" expression ")"
          | tableConstructor
```

```ebnf
tableConstructor ::= "{" [ expression { separator expression } [ separator ] ] "}"
```

```ebnf
separator ::= "," | ";"

compOp ::= "<" | ">" | "<=" | ">=" | "==" | "~="

addOp ::= "+" | "-"

mulOp ::= "*" | "/" | "%" | "^"

unaryOp ::= "-" | "not"
```

## Implementation Notes

1. This is the grammar of the current parser implementation, not the full Lua language.
2. `exprStatement` is written as `assignStmt | callStmt` for clarity. In code, the parser first reads a `prefixExpr` and then decides whether it is an assignment or a function-call statement.
3. `block` termination is parser-controlled by sentinel keywords rather than by a separate grammar file.
4. Table constructors currently support expression lists such as `{1, 2, 3}` but not key-value fields such as `{x = 1}`.
5. The expression rules reflect the parser's precedence-climbing structure.

## Traceability to Parser Methods

| Grammar Rule | Parser Method |
| --- | --- |
| `program` | `parse()` |
| `block` | `block()` |
| `statement` | `statement()` |
| `exprStatement` | `exprStatement()` |
| `prefixExpr` | `prefixExpr()` |
| `argList` | `argList()` |
| `localDecl` | `localDecl()` |
| `ifStmt` | `ifStmt()` |
| `forStmt` | `forStmt()` |
| `whileStmt` | `whileStmt()` |
| `repeatStmt` | `repeatStmt()` |
| `funcDecl` | `funcDecl()` |
| `paramList` | `paramList()` |
| `returnStmt` | `returnStmt()` |
| `expression` | `expression()` |
| `orExpr` | `orExpr()` |
| `andExpr` | `andExpr()` |
| `compExpr` | `compExpr()` |
| `concatExpr` | `concatExpr()` |
| `addExpr` | `addExpr()` |
| `mulExpr` | `mulExpr()` |
| `unaryExpr` | `unaryExpr()` |
| `primary` | `primary()` |
| `tableConstructor` | `tableConstructor()` |