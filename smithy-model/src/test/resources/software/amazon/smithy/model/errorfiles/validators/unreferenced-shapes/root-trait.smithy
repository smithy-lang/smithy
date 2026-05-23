$version: "2.0"

metadata validators = [
    {
        name: "UnreferencedShape"
    }
]

namespace smithy.example

// A custom trait marked with @root. Shapes that have this trait
// applied (and their transitive targets) are never unreferenced.
// The @trait trait itself is a root trait so this shape will be
// unreferenced.
@root
@trait
structure myRoot {}

// Referenced because @myRoot is marked with @root.
@myRoot
structure MyAbc {
    myString: MyString
}

// Referenced because it's transitively connected to MyAbc.
string MyString

// Referenced because @myRoot is marked with @root.
@myRoot
string MyDef

// Unreferenced because it has no connection to a root shape.
string Baz

// Unreferenced because it has no connection to a root shape.
string Bam
