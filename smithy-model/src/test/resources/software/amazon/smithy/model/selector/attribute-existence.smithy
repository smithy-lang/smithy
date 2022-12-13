$version: "2"

namespace smithy.example

@deprecated
string DeprecatedString

string MyString1

@tags
string MyString2

@enum([
    {value: "Foo1"},
    {value: "Foo2"}
    ])
string MyString3

@enum([
    {value: "Foo1", name: "FOO1",},
    {value: "Foo2", name: "FOO2"}
    ])
string MyString4

@enum([
    {value: "Foo1", tags: ["a"]}
    ])
string MyString5
