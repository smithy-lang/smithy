namespace com.foo

union A {
  a: B
}

union B {
  str: String
}

union C
{ a: A }

union D
{ a: A
}

union E     {  a: D   }

@deprecated
union F { a: E }

@deprecated
@documentation("foo")
union G { abc: F}

union H {
  foo: E
}

union I {
  foo: E,
  baz: H
}

union J {
  @deprecated
  foo: E,
  baz: H
}

union K {
  @deprecated @since("2.0")
  foo: E,
  @deprecated
  baz: H
}

union L {
  @deprecated @since("2.0") foo: E,
  @deprecated baz: H
}

@documentation("abc")
union M {
  @deprecated
  @since("2.0")
  foo:E,
  @deprecated
  baz:H
}
