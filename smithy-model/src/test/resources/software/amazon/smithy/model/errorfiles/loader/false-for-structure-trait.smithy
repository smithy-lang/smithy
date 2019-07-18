namespace com.foo

@trait
structure foo {}

structure Struct {}

// This is invalid
@foo(false)
string MyString1

// This is valid
@foo(true)
string MyString2
