$version: "2"

metadata suppressions = [
    {
        namespace: "foo.baz"
        id: "TraitTarget"
    }
]

namespace foo.baz

@idempotent // < this is invalid
string MyString
