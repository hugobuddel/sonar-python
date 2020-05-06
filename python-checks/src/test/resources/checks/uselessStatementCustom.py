def report_on_string():
  """triple quotes strings are still ignored"""
  "this is not" # Noncompliant
  'neither this' # Noncompliant

def binary_operators():
  42 + 24 # + is ignored
  a << b # << is ignored
  + 42 # OK: to avoid FPs, ignored operators are ignored for both unary and binary expressions
  50 - 8 # Noncompliant
