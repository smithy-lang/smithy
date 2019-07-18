namespace com.foo

@trait
string foo

// A null trait value cannot be coerced to a string.
@foo
string MyString
