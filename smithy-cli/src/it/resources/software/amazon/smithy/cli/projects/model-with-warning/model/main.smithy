$version: "2.0"
namespace smithy.example

@unstable
@trait
structure exampleUnstable {}

@exampleUnstable
string MyString
