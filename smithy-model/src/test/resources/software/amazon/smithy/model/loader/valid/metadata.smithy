metadata foo = "abc"
metadata "foo.1" = "def"
metadata "foo.4"="def"
metadata "foo.5"   =    "def"
metadata "foo.6"   =    "def"
metadata "foo.7" = true
metadata "foo.8" = false
metadata "foo.9" = null
metadata "foo.10" = [true, "true", false, "false", null, "null"]

metadata "foo.11" = [
true   ,    "true"
,
false
  ,
     "false", null, "null"
]

metadata "foo.12" = {
}

metadata "foo.13" = {}
metadata "foo.14" = {abc: 123}
metadata "foo.15" = {abc: "def"}
metadata "foo.16" = {abc: "def", foo: "baz"}
metadata "foo.17" = {"abc": "def", "foo": "baz"}

metadata "foo.18" = {
"abc"
:
"def"
,
"foo"
:
"baz"
}

metadata "foo.19" = {
  "abc.123": "def",
  def: {
    foo: [1, 2, -10],
    bar: []
  }
}

metadata exp1 = 10e0
metadata exp2 = 10e+0
metadata exp3 = 10e+2
metadata exp4 = -10e-2
metadata float1 = -2.0
metadata float2 = 10.0

// Trailing commas!

metadata "foo.20" = {
  a: "b",
}

metadata trailing_commas1 = {
  a: "b",
}
metadata trailing_commas2 = ["a", "b",]

// Unquoted strings resolve to shape IDs.
metadata shape_id = smithy.api#String
