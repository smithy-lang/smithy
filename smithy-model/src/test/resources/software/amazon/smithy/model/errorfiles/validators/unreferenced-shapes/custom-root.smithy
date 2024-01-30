$version: "2.0"

metadata validators = [
    {
        name: "UnreferencedShape"
        // Elevate the severity so that NOTE events don't slip through.
        severity: "WARNING"
        configuration: {
            rootShapeSelector: "[trait|smithy.example#root]"
        }
    }
]

namespace smithy.example

// Considered referenced because it's a trait.
@trait
structure root {}

// Considered referenced because of the root trait.
@root
structure MyAbc {
    MyString: MyString
}

// Considered referenced because it's referenced by MyAbc$MyString
string MyString

// Considered referenced because of the root trait.
@root
string MyDef

// Unreferenced.
string Baz

// Unreferenced.
string Bam
