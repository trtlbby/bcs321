-- projectreqt7_BACSAIN_error_input.lua
-- Intentional syntax errors to demonstrate the parser's error detection
-- and panic-mode recovery.  Each error block is labelled.

-- ── Error 1: Unexpected token (lone ~ not followed by =) ───────────────────
-- Lexer emits ~ as ERROR; parser reports "invalid token"
bad ~ good

-- ── Error 2: Missing operand after = (invalid expression structure) ─────────
-- Parser expects an expression after '=' but finds ';'
x = ;

-- ── Error 3: Unmatched delimiter (missing closing parenthesis) ─────────────
-- Parser expects ')' but reaches end-of-file
y = (a + b

-- ── Error 4: if without 'then' ──────────────────────────────────────────────
-- Parser expects keyword 'then' but finds the identifier 'x'
if x > 0
  x = 1
end

-- ── Error 5: Valid statement (tests panic-mode recovery) ───────────────────
-- After recovering from previous errors the parser should accept this cleanly
z = 42
