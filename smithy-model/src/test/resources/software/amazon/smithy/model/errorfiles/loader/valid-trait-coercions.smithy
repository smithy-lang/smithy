namespace com.foo

@trait(selector: "*")
list a {
  member: String,
}

@trait(selector: "*")
set b {
  member: String,
}

@trait
structure c {}

@trait(selector: "*")
map d {
  key: String,
  value: String,
}

@trait(selector: "*")
structure e {}

@a @b @c @d @e
structure Test {}
