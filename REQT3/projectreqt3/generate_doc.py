"""
Generate the formal grammar document for the Lua subset language
defined in projectreqt3.  Produces a .docx file.
"""

from docx import Document
from docx.shared import Pt, RGBColor, Inches, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
import copy

# ─────────────────────────────────────────────
# Helpers
# ─────────────────────────────────────────────

def set_heading(doc, text, level=1):
    h = doc.add_heading(text, level=level)
    h.alignment = WD_ALIGN_PARAGRAPH.LEFT
    run = h.runs[0] if h.runs else h.add_run(text)
    run.font.color.rgb = RGBColor(0x1F, 0x49, 0x7D)
    return h

def add_paragraph(doc, text, bold=False, italic=False, size=11):
    p = doc.add_paragraph()
    run = p.add_run(text)
    run.bold = bold
    run.italic = italic
    run.font.size = Pt(size)
    return p

def add_code_block(doc, lines):
    """Add a monospaced grey-shaded code block."""
    for line in lines:
        p = doc.add_paragraph()
        p.paragraph_format.left_indent = Inches(0.3)
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(0)
        run = p.add_run(line)
        run.font.name = "Courier New"
        run.font.size = Pt(9.5)
        # light grey background via highlight is not available; use shading on the paragraph
        pPr = p._p.get_or_add_pPr()
        shd = OxmlElement('w:shd')
        shd.set(qn('w:val'), 'clear')
        shd.set(qn('w:color'), 'auto')
        shd.set(qn('w:fill'), 'F2F2F2')
        pPr.append(shd)
    doc.add_paragraph()  # spacer

def add_railroad_box(doc, title, ascii_diagram):
    """Add a titled railroad/syntax diagram as a framed code block."""
    p = doc.add_paragraph()
    run = p.add_run(f"Diagram: {title}")
    run.bold = True
    run.font.size = Pt(10)
    run.font.color.rgb = RGBColor(0x1F, 0x49, 0x7D)

    for line in ascii_diagram:
        p = doc.add_paragraph()
        p.paragraph_format.left_indent = Inches(0.3)
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(0)
        run = p.add_run(line)
        run.font.name = "Courier New"
        run.font.size = Pt(9)
        pPr = p._p.get_or_add_pPr()
        shd = OxmlElement('w:shd')
        shd.set(qn('w:val'), 'clear')
        shd.set(qn('w:color'), 'auto')
        shd.set(qn('w:fill'), 'EAF4FB')
        pPr.append(shd)
    doc.add_paragraph()

# ─────────────────────────────────────────────
# Document content data
# ─────────────────────────────────────────────

EBNF_RULES = [
    ("program",
     ["program  ::=  block EOF"]),

    ("block",
     ["block    ::=  { statement }"]),

    ("statement",
     ['statement ::=  \';\' ',
      '           |  local_decl',
      '           |  if_stmt',
      '           |  for_stmt',
      '           |  while_stmt',
      '           |  repeat_stmt',
      '           |  func_decl',
      '           |  return_stmt',
      '           |  expr_stmt']),

    ("local_decl",
     ["local_decl ::=  'local' ( 'function' IDENTIFIER '(' param_list ')' block 'end'",
      "             |  IDENTIFIER [ '=' expression ] )"]),

    ("if_stmt",
     ["if_stmt  ::=  'if' expression 'then' block",
      "              { 'elseif' expression 'then' block }",
      "              [ 'else' block ]",
      "              'end'"]),

    ("for_stmt",
     ["for_stmt ::=  'for' IDENTIFIER '=' expression ',' expression",
      "              [ ',' expression ] 'do' block 'end'"]),

    ("while_stmt",
     ["while_stmt ::=  'while' expression 'do' block 'end'"]),

    ("repeat_stmt",
     ["repeat_stmt ::=  'repeat' block 'until' expression"]),

    ("func_decl",
     ["func_decl ::=  'function' IDENTIFIER '(' param_list ')' block 'end'"]),

    ("return_stmt",
     ["return_stmt ::=  'return' [ expression ]"]),

    ("expr_stmt",
     ["expr_stmt ::=  prefix_expr [ '=' expression ]"]),

    ("prefix_expr",
     ["prefix_expr ::=  IDENTIFIER { suffix }",
      "",
      "suffix  ::=  '.' IDENTIFIER",
      "         |  '[' expression ']'",
      "         |  ':' IDENTIFIER '(' arg_list ')'",
      "         |  '(' arg_list ')'"]),

    ("param_list",
     ["param_list ::=  [ IDENTIFIER { ',' IDENTIFIER } ]"]),

    ("arg_list",
     ["arg_list   ::=  [ expression { ',' expression } ]"]),

    ("expression hierarchy",
     ["expression  ::=  or_expr",
      "",
      "or_expr     ::=  and_expr { 'or'  and_expr }",
      "and_expr    ::=  comp_expr { 'and' comp_expr }",
      "comp_expr   ::=  concat_expr { comp_op concat_expr }",
      "comp_op     ::=  '<' | '>' | '<=' | '>=' | '==' | '~='",
      "concat_expr ::=  add_expr { '..' add_expr }",
      "add_expr    ::=  mul_expr { ( '+' | '-' ) mul_expr }",
      "mul_expr    ::=  unary_expr { ( '*' | '/' | '%' | '^' ) unary_expr }",
      "unary_expr  ::=  'not' unary_expr | '-' unary_expr | primary"]),

    ("primary",
     ["primary ::=  INTEGER_LITERAL",
      "         |  FLOAT_LITERAL",
      "         |  HEX_LITERAL",
      "         |  STRING_LITERAL",
      "         |  BOOLEAN_LITERAL",
      "         |  'nil'",
      "         |  IDENTIFIER { suffix }",
      "         |  '(' expression ')'",
      "         |  table_constructor"]),

    ("table_constructor",
     ["table_constructor ::=  '{' [ expression { ( ',' | ';' ) expression } ] '}'"]),

    ("Lexical rules",
     ["IDENTIFIER      ::=  ( letter | '_' ) { letter | digit | '_' }",
      "INTEGER_LITERAL ::=  digit { digit }",
      "FLOAT_LITERAL   ::=  ( digit { digit } '.' digit { digit } )",
      "                 |  '.' digit { digit }",
      "HEX_LITERAL     ::=  '0' ( 'x' | 'X' ) hex_digit { hex_digit }",
      "STRING_LITERAL  ::=  '\"' { char | escape } '\"'",
      "                 |  \"'\" { char | escape } \"'\"",
      "escape          ::=  '\\' char",
      "comment         ::=  '--' { char }  (* to end of line *)",
      "",
      "letter    ::=  'a' .. 'z' | 'A' .. 'Z'",
      "digit     ::=  '0' .. '9'",
      "hex_digit ::=  digit | 'a' .. 'f' | 'A' .. 'F'"]),
]

# Railroad / syntax diagrams (ASCII art)
DIAGRAMS = [
    ("program",
     [
      "                                                           ",
      "  ►──[ block ]──[ EOF ]──►                                ",
     ]),

    ("block",
     [
      "                                                           ",
      "  ►──┬──────────────────────┬──►                          ",
      "     │                      │                             ",
      "     └──[ statement ]──◄────┘                             ",
     ]),

    ("statement",
     [
      "                                                           ",
      "  ►──┬──[ ';' ]──────────────────┬──►                     ",
      "     ├──[ local_decl ]────────────┤                       ",
      "     ├──[ if_stmt ]───────────────┤                       ",
      "     ├──[ for_stmt ]──────────────┤                       ",
      "     ├──[ while_stmt ]────────────┤                       ",
      "     ├──[ repeat_stmt ]───────────┤                       ",
      "     ├──[ func_decl ]─────────────┤                       ",
      "     ├──[ return_stmt ]───────────┤                       ",
      "     └──[ expr_stmt ]─────────────┘                       ",
     ]),

    ("local_decl",
     [
      "                                                           ",
      "  ►──'local'──┬──'function'──[IDENT]──'('──[param_list]──')'──[block]──'end'──►  ",
      "              │                                                                    ",
      "              └──[IDENT]──┬─────────────────┬──►                                  ",
      "                          └──'='──[expr]────┘                                     ",
     ]),

    ("if_stmt",
     [
      "                                                                           ",
      "  ►──'if'──[expr]──'then'──[block]──┬──────────────────────────────┬──►  ",
      "                                    │                               │     ",
      "                                    ├──'elseif'──[expr]──'then'─────┤     ",
      "                                    │    (repeating)                │     ",
      "                                    ├──'else'──[block]──────────────┤     ",
      "                                    └──'end'────────────────────────┘     ",
     ]),

    ("for_stmt",
     [
      "                                                                    ",
      "  ►──'for'──[IDENT]──'='──[expr]──','──[expr]──┬──────────────┬──'do'──[block]──'end'──►  ",
      "                                               └──','──[expr]─┘                            ",
     ]),

    ("while_stmt",
     [
      "                                             ",
      "  ►──'while'──[expr]──'do'──[block]──'end'──►  ",
     ]),

    ("repeat_stmt",
     [
      "                                              ",
      "  ►──'repeat'──[block]──'until'──[expr]──►    ",
     ]),

    ("func_decl",
     [
      "                                                                          ",
      "  ►──'function'──[IDENT]──'('──[param_list]──')'──[block]──'end'──►      ",
     ]),

    ("return_stmt",
     [
      "                                    ",
      "  ►──'return'──┬────────────┬──►    ",
      "               └──[expr]────┘       ",
     ]),

    ("expr_stmt",
     [
      "                                          ",
      "  ►──[prefix_expr]──┬─────────────────┬──►  ",
      "                    └──'='──[expr]─────┘     ",
     ]),

    ("prefix_expr / suffix",
     [
      "                                                                 ",
      "  ►──[IDENT]──┬──────────────────────────────────────────┬──►  ",
      "              │                                           │      ",
      "              ├──'.'──[IDENT]──────────────────────────◄─┤      ",
      "              ├──'['──[expr]──']'──────────────────────◄─┤      ",
      "              ├──':'──[IDENT]──'('──[arg_list]──')'────◄─┤      ",
      "              └──'('──[arg_list]──')'────────────────────┘      ",
     ]),

    ("expression (precedence, top → bottom)",
     [
      "                                                                  ",
      "  expression                                                      ",
      "    └─► or_expr                                                   ",
      "           └─► and_expr { 'or' and_expr }                        ",
      "                 └─► comp_expr { 'and' comp_expr }               ",
      "                       └─► concat_expr { comp_op concat_expr }   ",
      "                             └─► add_expr { '..' add_expr }      ",
      "                                   └─► mul_expr { +/- mul_expr } ",
      "                                         └─► unary_expr          ",
      "                                               └─► primary        ",
     ]),

    ("primary",
     [
      "                                                          ",
      "  ►──┬──[INTEGER_LITERAL]──────────────────────────┬──►  ",
      "     ├──[FLOAT_LITERAL]───────────────────────────┤       ",
      "     ├──[HEX_LITERAL]────────────────────────────┤        ",
      "     ├──[STRING_LITERAL]──────────────────────────┤       ",
      "     ├──[BOOLEAN_LITERAL]─────────────────────────┤       ",
      "     ├──'nil'────────────────────────────────────┤        ",
      "     ├──[IDENT]──{ suffix }───────────────────────┤       ",
      "     ├──'('──[expr]──')'──────────────────────────┤       ",
      "     └──[table_constructor]──────────────────────┘        ",
     ]),

    ("table_constructor",
     [
      "                                                                          ",
      "  ►──'{'──┬──────────────────────────────────────────────────────┬──'}'──►  ",
      "          └──[expr]──┬────────────────────────────────────┬──────┘          ",
      "                     └──( ',' | ';' )──[expr]──◄──────────┘                 ",
     ]),

    ("IDENTIFIER (lexical)",
     [
      "                                                           ",
      "  ►──( letter | '_' )──┬───────────────────────────┬──►  ",
      "                       │                            │      ",
      "                       └──( letter | digit | '_' )─┘      ",
     ]),

    ("Number literals (lexical)",
     [
      "  INTEGER  ►──digit──{ digit }──►                                      ",
      "                                                                        ",
      "  FLOAT    ►──┬──digit──{ digit }──'.'──digit──{ digit }──┬──►         ",
      "              └──'.'──digit──{ digit }─────────────────────┘           ",
      "                                                                        ",
      "  HEX      ►──'0'──( 'x' | 'X' )──hex_digit──{ hex_digit }──►         ",
     ]),

    ("STRING_LITERAL (lexical)",
     [
      "                                                                    ",
      "  ►──'\"'──┬──( any char except '\"' and '\\' )──┬──'\"'──►          ",
      "          └──'\\'──( any char )──◄──────────────┘                   ",
      "                                                                    ",
      "  (Same rule applies for single-quoted strings using  '  )         ",
     ]),
]

# ─────────────────────────────────────────────
# Build the document
# ─────────────────────────────────────────────

def build_document():
    doc = Document()

    # ── Page margins ──
    for section in doc.sections:
        section.top_margin    = Cm(2.5)
        section.bottom_margin = Cm(2.5)
        section.left_margin   = Cm(3.0)
        section.right_margin  = Cm(2.5)

    # ── Title page ──
    title_p = doc.add_paragraph()
    title_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = title_p.add_run("Formal Language Structure of Lua\n(Subset Implementation)")
    r.bold = True
    r.font.size = Pt(20)
    r.font.color.rgb = RGBColor(0x1F, 0x49, 0x7D)

    doc.add_paragraph()

    sub_p = doc.add_paragraph()
    sub_p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r2 = sub_p.add_run(
        "BCS321 — REQT3\n"
        "Context-Free Grammar (EBNF) and Syntax Diagrams (Railroad)\n"
    )
    r2.font.size = Pt(12)
    r2.italic = True

    doc.add_page_break()

    # ══════════════════════════════════════════
    # SECTION 1 — Introduction
    # ══════════════════════════════════════════
    set_heading(doc, "1.  Introduction", level=1)
    add_paragraph(doc,
        "This document presents the formal grammatical structure of the Lua subset "
        "implemented in the projectreqt3 codebase.  The implementation covers a "
        "Scanner (lexical analyser), a Keyword classifier, a recursive-descent Parser, "
        "and a Symbol Table.  The language constructs recognised by the parser define "
        "the subset described here.\n"
        "\n"
        "Two complementary notations are used:\n"
        "  • Context-Free Grammar (CFG) expressed in Extended Backus-Naur Form (EBNF)\n"
        "  • Syntax (Railroad) Diagrams — a visual equivalent of the EBNF rules\n"
        "\n"
        "EBNF conventions used throughout this document:\n"
        "  ::=    production rule\n"
        "  { }    zero or more repetitions\n"
        "  [ ]    optional (zero or one occurrence)\n"
        "  ( )    grouping\n"
        "   |     alternation (choice)\n"
        "  ' '    terminal symbol (literal keyword or punctuation)\n"
        "  UPPER  non-quoted UPPERCASE names are terminal token types\n"
        "  lower  lowercase names are non-terminal symbols"
    )

    doc.add_page_break()

    # ══════════════════════════════════════════
    # SECTION 2 — Token Types
    # ══════════════════════════════════════════
    set_heading(doc, "2.  Token Types  (TokenType.java)", level=1)
    add_paragraph(doc,
        "The lexer recognises the following token categories (defined in TokenType.java):"
    )

    token_data = [
        ("KEYWORD",          "Language keywords (see list below)"),
        ("BOOLEAN_LITERAL",  "true  |  false"),
        ("NIL",              "nil"),
        ("IDENTIFIER",       "User-defined names: letter or '_', followed by letters, digits, '_'"),
        ("INTEGER_LITERAL",  "Decimal integer: digit { digit }"),
        ("FLOAT_LITERAL",    "Floating-point number with a decimal point"),
        ("HEX_LITERAL",      "Hexadecimal: 0x or 0X followed by hex digits"),
        ("STRING_LITERAL",   "Double-quoted or single-quoted string with escape support"),
        ("OPERATOR",         "=  ==  ~=  <  <=  >  >=  +  -  *  /  %  ^  .."),
        ("LOGICAL_OP",       "and  |  or  |  not"),
        ("DELIMITER",        "(  )  {  }  [  ]  ,  :  ;  ."),
        ("EOF",              "End-of-file sentinel"),
        ("ERROR",            "Unrecognised / malformed input"),
    ]

    tbl = doc.add_table(rows=1, cols=2)
    tbl.style = 'Table Grid'
    tbl.alignment = WD_TABLE_ALIGNMENT.LEFT
    hdr = tbl.rows[0].cells
    for cell, text in zip(hdr, ["Token Type", "Description / Examples"]):
        cell.text = text
        cell.paragraphs[0].runs[0].bold = True

    for tt, desc in token_data:
        row = tbl.add_row().cells
        row[0].text = tt
        row[1].text = desc

    doc.add_paragraph()

    add_paragraph(doc, "Keyword set (KeywordList.java):", bold=True)
    add_code_block(doc, [
        "and   break  do     else    elseif  end   false  for",
        "function  goto  if  in  local  nil  not  or",
        "repeat  return  then  true  until  while",
    ])

    doc.add_page_break()

    # ══════════════════════════════════════════
    # SECTION 3 — EBNF Grammar
    # ══════════════════════════════════════════
    set_heading(doc, "3.  Context-Free Grammar  (EBNF)", level=1)
    add_paragraph(doc,
        "The following EBNF rules are derived directly from the recursive-descent "
        "parser implemented in Parser.java and the scanner in Scanner.java."
    )
    doc.add_paragraph()

    set_heading(doc, "3.1  Program Structure", level=2)
    add_code_block(doc, [
        "program  ::=  block EOF",
        "",
        "block    ::=  { statement }",
    ])

    set_heading(doc, "3.2  Statements", level=2)
    add_code_block(doc, [
        "statement ::=  ';'",
        "           |  local_decl",
        "           |  if_stmt",
        "           |  for_stmt",
        "           |  while_stmt",
        "           |  repeat_stmt",
        "           |  func_decl",
        "           |  return_stmt",
        "           |  expr_stmt",
    ])

    set_heading(doc, "3.3  Local Declaration", level=2)
    add_code_block(doc, [
        "local_decl ::=  'local' ( 'function' IDENTIFIER '(' param_list ')' block 'end'",
        "             |           IDENTIFIER [ '=' expression ] )",
    ])

    set_heading(doc, "3.4  If Statement", level=2)
    add_code_block(doc, [
        "if_stmt ::=  'if' expression 'then' block",
        "             { 'elseif' expression 'then' block }",
        "             [ 'else' block ]",
        "             'end'",
    ])

    set_heading(doc, "3.5  For Statement", level=2)
    add_code_block(doc, [
        "for_stmt ::=  'for' IDENTIFIER '=' expression ',' expression",
        "              [ ',' expression ] 'do' block 'end'",
    ])

    set_heading(doc, "3.6  While Statement", level=2)
    add_code_block(doc, [
        "while_stmt ::=  'while' expression 'do' block 'end'",
    ])

    set_heading(doc, "3.7  Repeat Statement", level=2)
    add_code_block(doc, [
        "repeat_stmt ::=  'repeat' block 'until' expression",
    ])

    set_heading(doc, "3.8  Function Declaration", level=2)
    add_code_block(doc, [
        "func_decl  ::=  'function' IDENTIFIER '(' param_list ')' block 'end'",
        "",
        "param_list ::=  [ IDENTIFIER { ',' IDENTIFIER } ]",
    ])

    set_heading(doc, "3.9  Return Statement", level=2)
    add_code_block(doc, [
        "return_stmt ::=  'return' [ expression ]",
    ])

    set_heading(doc, "3.10  Expression Statement  (assignment or call)", level=2)
    add_code_block(doc, [
        "expr_stmt   ::=  prefix_expr [ '=' expression ]",
        "",
        "prefix_expr ::=  IDENTIFIER { suffix }",
        "",
        "suffix      ::=  '.' IDENTIFIER",
        "            |  '[' expression ']'",
        "            |  ':' IDENTIFIER '(' arg_list ')'",
        "            |  '(' arg_list ')'",
        "",
        "arg_list    ::=  [ expression { ',' expression } ]",
    ])

    set_heading(doc, "3.11  Expressions  (precedence, lowest → highest)", level=2)
    add_code_block(doc, [
        "expression  ::=  or_expr",
        "",
        "or_expr     ::=  and_expr { 'or'  and_expr }",
        "",
        "and_expr    ::=  comp_expr { 'and' comp_expr }",
        "",
        "comp_expr   ::=  concat_expr { comp_op concat_expr }",
        "comp_op     ::=  '<' | '>' | '<=' | '>=' | '==' | '~='",
        "",
        "concat_expr ::=  add_expr { '..' add_expr }",
        "",
        "add_expr    ::=  mul_expr { ( '+' | '-' ) mul_expr }",
        "",
        "mul_expr    ::=  unary_expr { ( '*' | '/' | '%' | '^' ) unary_expr }",
        "",
        "unary_expr  ::=  'not' unary_expr",
        "            |  '-' unary_expr",
        "            |  primary",
    ])

    set_heading(doc, "3.12  Primary Expressions", level=2)
    add_code_block(doc, [
        "primary ::=  INTEGER_LITERAL",
        "         |  FLOAT_LITERAL",
        "         |  HEX_LITERAL",
        "         |  STRING_LITERAL",
        "         |  BOOLEAN_LITERAL",
        "         |  'nil'",
        "         |  IDENTIFIER { suffix }",
        "         |  '(' expression ')'",
        "         |  table_constructor",
        "",
        "table_constructor ::=  '{' [ expression { ( ',' | ';' ) expression } ] '}'",
    ])

    set_heading(doc, "3.13  Lexical Rules", level=2)
    add_code_block(doc, [
        "IDENTIFIER      ::=  ( letter | '_' ) { letter | digit | '_' }",
        "INTEGER_LITERAL ::=  digit { digit }",
        "FLOAT_LITERAL   ::=  ( digit { digit } '.' digit { digit } )",
        "                 |  '.' digit { digit }",
        "HEX_LITERAL     ::=  '0' ( 'x' | 'X' ) hex_digit { hex_digit }",
        "STRING_LITERAL  ::=  '\"' { char | escape } '\"'",
        "                 |  \"'\" { char | escape } \"'\"",
        "escape          ::=  '\\' char",
        "comment         ::=  '--' { char }   -- to end of line",
        "",
        "letter    ::=  'a'..'z' | 'A'..'Z'",
        "digit     ::=  '0'..'9'",
        "hex_digit ::=  digit | 'a'..'f' | 'A'..'F'",
    ])

    doc.add_page_break()

    # ══════════════════════════════════════════
    # SECTION 4 — Syntax / Railroad Diagrams
    # ══════════════════════════════════════════
    set_heading(doc, "4.  Syntax Diagrams  (Railroad Diagrams)", level=1)
    add_paragraph(doc,
        "Each diagram below corresponds directly to one of the EBNF productions.  "
        "Reading conventions:\n"
        "  ►   entry / exit of the diagram\n"
        "  ─   path (sequence)\n"
        "  ┬┴  fork / merge (choice or loop)\n"
        "  [ ] non-terminal symbol\n"
        "  ' ' terminal (keyword or punctuation)\n"
        "  ◄   loop-back arrow"
    )
    doc.add_paragraph()

    set_heading(doc, "4.1  program", level=2)
    add_railroad_box(doc, "program", [
        "                                              ",
        "  ►──[ block ]──[ EOF ]──►                   ",
    ])

    set_heading(doc, "4.2  block", level=2)
    add_railroad_box(doc, "block", [
        "                                              ",
        "  ►──┬──────────────────────────────┬──►     ",
        "     │                              │         ",
        "     └──[ statement ]──────────◄───┘          ",
    ])

    set_heading(doc, "4.3  statement", level=2)
    add_railroad_box(doc, "statement", [
        "                                                    ",
        "  ►──┬──[ ';' ]──────────────────────────────┬──►  ",
        "     ├──[ local_decl ]───────────────────────┤      ",
        "     ├──[ if_stmt ]─────────────────────────┤       ",
        "     ├──[ for_stmt ]────────────────────────┤       ",
        "     ├──[ while_stmt ]──────────────────────┤       ",
        "     ├──[ repeat_stmt ]─────────────────────┤       ",
        "     ├──[ func_decl ]───────────────────────┤       ",
        "     ├──[ return_stmt ]──────────────────────┤      ",
        "     └──[ expr_stmt ]───────────────────────┘       ",
    ])

    set_heading(doc, "4.4  local_decl", level=2)
    add_railroad_box(doc, "local_decl", [
        "                                                                                     ",
        "  ►──'local'──┬──'function'──[IDENT]──'('──[param_list]──')'──[block]──'end'──┬──► ",
        "              │                                                                │     ",
        "              └──[IDENT]──┬─────────────────────┬──────────────────────────────┘    ",
        "                          └──'='──[expression]──┘                                   ",
    ])

    set_heading(doc, "4.5  if_stmt", level=2)
    add_railroad_box(doc, "if_stmt", [
        "                                                                                ",
        "  ►──'if'──[expr]──'then'──[block]──┬──────────────────────────────────┬──►   ",
        "                                    ├──'elseif'──[expr]──'then'──[block]┤      ",
        "                                    │          (zero or more)           │      ",
        "                                    ├──'else'──[block]──────────────────┤      ",
        "                                    └──────────────────────────────────►┘      ",
        "                                    (all paths converge at 'end')              ",
    ])
    add_code_block(doc, ["  Note: all paths through 4.5 must be followed by 'end'."])

    set_heading(doc, "4.6  for_stmt", level=2)
    add_railroad_box(doc, "for_stmt", [
        "                                                                                     ",
        "  ►──'for'──[IDENT]──'='──[expr]──','──[expr]──┬──────────────┬──'do'──[block]──'end'──►",
        "                                               └──','──[expr]─┘                         ",
    ])

    set_heading(doc, "4.7  while_stmt", level=2)
    add_railroad_box(doc, "while_stmt", [
        "                                                  ",
        "  ►──'while'──[expr]──'do'──[block]──'end'──►    ",
    ])

    set_heading(doc, "4.8  repeat_stmt", level=2)
    add_railroad_box(doc, "repeat_stmt", [
        "                                                   ",
        "  ►──'repeat'──[block]──'until'──[expr]──►         ",
    ])

    set_heading(doc, "4.9  func_decl", level=2)
    add_railroad_box(doc, "func_decl", [
        "                                                                               ",
        "  ►──'function'──[IDENT]──'('──[param_list]──')'──[block]──'end'──►          ",
    ])

    set_heading(doc, "4.10  param_list", level=2)
    add_railroad_box(doc, "param_list", [
        "                                                         ",
        "  ►──┬──────────────────────────────────────────┬──►    ",
        "     └──[IDENT]──┬──────────────────────────┬───┘       ",
        "                 └──','──[IDENT]──◄──────────┘           ",
    ])

    set_heading(doc, "4.11  return_stmt", level=2)
    add_railroad_box(doc, "return_stmt", [
        "                                          ",
        "  ►──'return'──┬──────────────────┬──►   ",
        "               └──[expression]────┘       ",
    ])

    set_heading(doc, "4.12  expr_stmt  (assignment or call)", level=2)
    add_railroad_box(doc, "expr_stmt", [
        "                                                 ",
        "  ►──[prefix_expr]──┬────────────────────┬──►   ",
        "                    └──'='──[expression]──┘      ",
    ])

    set_heading(doc, "4.13  prefix_expr and suffix", level=2)
    add_railroad_box(doc, "prefix_expr", [
        "                                                                   ",
        "  ►──[IDENT]──┬─────────────────────────────────────────────┬──►  ",
        "              │  (repeat zero or more suffixes)              │      ",
        "              ├──'.'──[IDENT]──────────────────────────────◄─┤     ",
        "              ├──'['──[expr]──']'──────────────────────────◄─┤     ",
        "              ├──':'──[IDENT]──'('──[arg_list]──')'────────◄─┤     ",
        "              └──'('──[arg_list]──')'──────────────────────◄─┘     ",
    ])

    set_heading(doc, "4.14  arg_list", level=2)
    add_railroad_box(doc, "arg_list", [
        "                                                         ",
        "  ►──┬──────────────────────────────────────────┬──►    ",
        "     └──[expr]──┬──────────────────────────┬────┘       ",
        "                └──','──[expr]──◄───────────┘            ",
    ])

    set_heading(doc, "4.15  expression  (precedence tower)", level=2)
    add_railroad_box(doc, "expression (precedence tower)", [
        "                                                                    ",
        "  expression                                                        ",
        "      │                                                             ",
        "      ▼  or_expr                                                    ",
        "      │    └──[and_expr]──{ 'or' ──[and_expr] }                    ",
        "      ▼  and_expr                                                   ",
        "      │    └──[comp_expr]──{ 'and' ──[comp_expr] }                 ",
        "      ▼  comp_expr                                                  ",
        "      │    └──[concat_expr]──{ comp_op ──[concat_expr] }           ",
        "      │         comp_op: < > <= >= == ~=                            ",
        "      ▼  concat_expr                                                ",
        "      │    └──[add_expr]──{ '..' ──[add_expr] }                    ",
        "      ▼  add_expr                                                   ",
        "      │    └──[mul_expr]──{ ('+' | '-') ──[mul_expr] }             ",
        "      ▼  mul_expr                                                   ",
        "      │    └──[unary_expr]──{ ('*'|'/'|'%'|'^') ──[unary_expr] }  ",
        "      ▼  unary_expr                                                 ",
        "           ├──'not'──[unary_expr]                                   ",
        "           ├──'-'───[unary_expr]                                    ",
        "           └──[primary]                                             ",
    ])

    set_heading(doc, "4.16  primary", level=2)
    add_railroad_box(doc, "primary", [
        "                                                            ",
        "  ►──┬──[INTEGER_LITERAL]───────────────────────────┬──►  ",
        "     ├──[FLOAT_LITERAL]────────────────────────────┤       ",
        "     ├──[HEX_LITERAL]─────────────────────────────┤        ",
        "     ├──[STRING_LITERAL]───────────────────────────┤       ",
        "     ├──[BOOLEAN_LITERAL]──────────────────────────┤       ",
        "     ├──'nil'────────────────────────────────────┤         ",
        "     ├──[IDENT]──{ suffix }────────────────────────┤       ",
        "     ├──'('──[expression]──')'────────────────────┤        ",
        "     └──[table_constructor]────────────────────────┘       ",
    ])

    set_heading(doc, "4.17  table_constructor", level=2)
    add_railroad_box(doc, "table_constructor", [
        "                                                                              ",
        "  ►──'{'──┬────────────────────────────────────────────────────┬──'}'──►    ",
        "          └──[expr]──┬──────────────────────────────────┬──────┘             ",
        "                     └──( ',' | ';' )──[expr]──◄─────────┘                   ",
    ])

    set_heading(doc, "4.18  IDENTIFIER  (lexical)", level=2)
    add_railroad_box(doc, "IDENTIFIER", [
        "                                                          ",
        "  ►──( letter | '_' )──┬────────────────────────────┬──►",
        "                       │                             │    ",
        "                       └──( letter | digit | '_' )──┘    ",
    ])

    set_heading(doc, "4.19  Number Literals  (lexical)", level=2)
    add_railroad_box(doc, "INTEGER_LITERAL", [
        "  ►──digit──{ digit }──►",
    ])
    add_railroad_box(doc, "FLOAT_LITERAL", [
        "  ►──┬──digit──{ digit }──'.'──digit──{ digit }──┬──►",
        "     └──'.'──digit──{ digit }───────────────────┘    ",
    ])
    add_railroad_box(doc, "HEX_LITERAL", [
        "  ►──'0'──( 'x' | 'X' )──hex_digit──{ hex_digit }──►",
    ])

    set_heading(doc, "4.20  STRING_LITERAL  (lexical)", level=2)
    add_railroad_box(doc, "STRING_LITERAL (double-quoted)", [
        "  ►──'\"'──┬──( any char except '\"' and '\\' )──┬──'\"'──►",
        "          └──'\\'──( any char )──◄──────────────┘         ",
        "  (Single-quoted strings follow the identical rule with  '  )",
    ])

    doc.add_page_break()

    # ══════════════════════════════════════════
    # SECTION 5 — Operator Precedence Table
    # ══════════════════════════════════════════
    set_heading(doc, "5.  Operator Precedence Summary", level=1)
    add_paragraph(doc,
        "Derived from the layered expression grammar (lowest to highest precedence):"
    )

    prec_data = [
        ("1 (lowest)", "or",                "Logical OR",              "left"),
        ("2",          "and",               "Logical AND",             "left"),
        ("3",          "< > <= >= == ~=",   "Comparison / equality",   "left"),
        ("4",          "..",                "String concatenation",    "right"),
        ("5",          "+ -",               "Addition / subtraction",  "left"),
        ("6",          "* / % ^",           "Multiplication etc.",     "left"),
        ("7 (highest)","not  -  (unary)",   "Unary operators",         "right"),
    ]

    tbl2 = doc.add_table(rows=1, cols=4)
    tbl2.style = 'Table Grid'
    tbl2.alignment = WD_TABLE_ALIGNMENT.LEFT
    hdr2 = tbl2.rows[0].cells
    for cell, text in zip(hdr2, ["Level", "Operator(s)", "Category", "Associativity"]):
        cell.text = text
        cell.paragraphs[0].runs[0].bold = True

    for row_data in prec_data:
        row = tbl2.add_row().cells
        for cell, val in zip(row, row_data):
            cell.text = val

    doc.add_paragraph()
    doc.add_page_break()

    # ══════════════════════════════════════════
    # SECTION 6 — Symbol Table Structure
    # ══════════════════════════════════════════
    set_heading(doc, "6.  Symbol Table  (SymbolTable.java)", level=1)
    add_paragraph(doc,
        "The symbol table records every identifier encountered during lexical/syntax analysis.  "
        "Each entry holds the following fields:"
    )

    sym_data = [
        ("name",       "Identifier lexeme (string)"),
        ("type",       "Inferred data type: integer | float | hex | string | boolean | nil | identifier | unknown | function"),
        ("scopeLevel", "Integer depth: 0 = global; increments on 'then', 'do', 'function' blocks"),
        ("category",   "VARIABLE | FUNCTION | PARAMETER"),
    ]

    tbl3 = doc.add_table(rows=1, cols=2)
    tbl3.style = 'Table Grid'
    hdr3 = tbl3.rows[0].cells
    for cell, text in zip(hdr3, ["Field", "Description"]):
        cell.text = text
        cell.paragraphs[0].runs[0].bold = True

    for name, desc in sym_data:
        row = tbl3.add_row().cells
        row[0].text = name
        row[1].text = desc

    doc.add_paragraph()
    add_paragraph(doc,
        "Scope management:\n"
        "  enterScope()  — called when 'then', 'do', or 'function' keyword is processed\n"
        "  exitScope()   — called when 'end' keyword is processed\n"
        "  lookup()      — searches from innermost to outermost scope"
    )

    # ── Save ──
    out = r"c:\Users\user\devs\Academics\BCS321\REQT3\projectreqt3\Formal_Grammar_Document.docx"
    doc.save(out)
    print(f"Saved: {out}")

if __name__ == "__main__":
    build_document()
