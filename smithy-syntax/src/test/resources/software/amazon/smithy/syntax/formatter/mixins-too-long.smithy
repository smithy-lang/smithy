$version: "2.0"

namespace smithy.example

string MyString1 with [ThisIsReallyLongStringName1, ThisIsReallyLongStringName2, ThisIsReallyLongStringName3]

string MyString2 with [
    ThisIsReallyLongStringName1
    ThisIsReallyLongStringName2
    ThisIsReallyLongStringName3
    ThisIsReallyLongStringName4
]

structure MyStruct1 with [ThisIsReallyLongStructName1, ThisIsReallyLongStructName2, ThisIsReallyLongStructName3] {}

structure MyStruct2 with [
    ThisIsReallyLongStructName1
    ThisIsReallyLongStructName2
    ThisIsReallyLongStructName3
    ThisIsReallyLongStructName4
] {}

structure MyStruct3 with [
    ThisIsReallyLongStructName1
    ThisIsReallyLongStructName2
    ThisIsReallyLongStructName3
    ThisIsReallyLongStructName4
] {
    foo: String
    bar: String
}

structure MyStruct11111111 with [ThisIsReallyLongStructName1, ThisIsReallyLongStructName2, ThisIsReallyLongStructName3]
{}

structure MyStruct111112 with [ThisIsReallyLongStructName1, ThisIsReallyLongStructName2, ThisIsReallyLongStructName3] {
    foo: Bar
}

structure MyStruct111111113 with [ThisIsReallyLongStructName1, ThisIsReallyLongStructName2, ThisIsReallyLongStructName3]
{
    foo: Bar
}

@mixin
string ThisIsReallyLongStringName1

@mixin
string ThisIsReallyLongStringName2

@mixin
string ThisIsReallyLongStringName3

@mixin
string ThisIsReallyLongStringName4

@mixin
structure ThisIsReallyLongStructName1 {}

@mixin
structure ThisIsReallyLongStructName2 {}

@mixin
structure ThisIsReallyLongStructName3 {}

@mixin
structure ThisIsReallyLongStructName4 {}
