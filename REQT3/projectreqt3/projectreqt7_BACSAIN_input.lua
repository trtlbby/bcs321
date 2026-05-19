-- projectreqt7_BACSAIN_input.lua
-- Valid Lua source covering all parser checklist items.

-- ── Checklist 1: Statement Validation ──────────────────────────────────────
-- Simple assignment (valid statement)
x = 10

-- ── Checklist 2: Expression Nesting ────────────────────────────────────────
-- Nested arithmetic with correct operator precedence
a = (b + c) * d

-- Deeper nesting
result = (x + 1) * (y - 2) / z

-- ── Checklist 3: Balanced Delimiters ───────────────────────────────────────
-- Function declaration with matching ( ) and end
function greet(name)
  print("Hello, " .. name)
end

-- Table constructor with matching { }
t = {1, 2, 3}

-- Indexed access with matching [ ]
v = t[1]

-- ── Checklist 4: Control flow with balanced blocks ─────────────────────────
-- if-elseif-else-end
if x > 5 then
  x = x - 1
elseif x == 0 then
  x = 1
else
  x = x + 1
end

-- while loop
while x > 0 do
  x = x - 1
end

-- for loop
for i = 1, 10 do
  x = x + i
end

-- repeat-until
repeat
  x = x + 1
until x >= 5

-- ── Checklist 5: Tree Generation ───────────────────────────────────────────
-- Logical operators: not, and, or
local flag = not false and true or nil

-- String concatenation
local msg = "Score: " .. x .. "!"

-- Method call
t:push(42)

-- Hex literal and float
local colour = 0xFF
local pi = 3.14

-- Nested function call inside expression
local total = x + greet(name)
