$version: "2.0"

namespace smithy.example

@deprecated
string MyString

string OtherString

structure Foo {
    a: MyString
    b: OtherString
}
