class MyClass:
    attr = 42

    def method(self):
        pass

    @classmethod
    def class_method(cls):
        pass

    @staticmethod
    def static_method():
        pass

class CustomException(TypeError):
    pass

def a_function():
    pass

async def statements_having_effects_or_meant_to_be_ignored(param):
    ...
    pass
    await param
    param()
    x = lambda: param
    (lambda: 2)()
    yield 42
    return 1

def literals():
    True  # Noncompliant {{Remove or refactor this statement; it has no side effects.}}
#   ^^^^
    False  # Noncompliant
    None  # Noncompliant
    1  # Noncompliant
    1.1  # Noncompliant
    [1, 2]  # Noncompliant
    {1, 2}  # Noncompliant
    {1: 1, 2: 2}  # Noncompliant
    "str"  # OK (reportOnStrings false by default)

def unassigned_expressions(param):
    x = 3
    param  # Noncompliant
    x  # Noncompliant
    lambda: 2  # Noncompliant
    func(42); param  # Noncompliant

def conditional_expressions():
    1 if True else 2  # Noncompliant
    1 if True else func()  # Noncompliant
    if True:
        1  # Noncompliant
    else:
        func()  # Ok

def binary_and_unary_expressions():
  """By default, no operator is ignored"""
  param < 1  # Noncompliant
  param + param  # Noncompliant
  + param  # Noncompliant
  - param  # Noncompliant
  float16(65472)+float16(32) # Noncompliant
  float16(2**-13)/float16(2) # Noncompliant
  1 / 0 # Noncompliant

def expression_list():
  basic.LENGTH, basic.DATA, basic.COMMA, basic.NUMBER # Noncompliant 4
  something, a_call(), other # Noncompliant 2

def non_called_functions_and_classes():
    round  # Noncompliant
    a_function  # Noncompliant

    MyClass  # Noncompliant
    NotImplemented  # Noncompliant

    # No issue will be raised on Exception classes. This use case is covered by S3984
    BaseException  # Ok
    Exception  # Ok
    ValueError  # Ok
    CustomException  # Ok

def accessing_members(param):
    """
    Sometime members are accessed on purpose.
    Example: to force lazy loading. (https://github.com/tensorflow/tensorflow/blob/5c00e793c61860bbf26778cd4704313e867645be/tensorflow/api_template_v1.__init__.py#L68)
    This is why we will only raise issues when on methods which are not called and class attributes.

    Note: Accessing a member without doing any function call is still confusing so we might have a
    rule dedicated to that later.
    """
    MyClass.attr  # Noncompliant
    MyClass.class_method  # Noncompliant
    MyClass.static_method  # Noncompliant
    MyClass.__eq__  # Noncompliant

    if MyClass.class_method:
        pass

def corner_cases():
    try:
        MyClass.attr # No issues when within a try/except
    except AttributeError as e:
        pass

    param and param()  # No issue when there are calls within a boolean expression (could be used as an if)
    not (param() or param)
    param and param + 1  # Noncompliant
    not (a and b) # Noncompliant
    call(42) == "hello" # Noncompliant
    [a() for a in param]  #  No issue for comprehensions: might be used as a loop (although it's a code smell)
    a.addCallBack(42).something # Noncompliant
