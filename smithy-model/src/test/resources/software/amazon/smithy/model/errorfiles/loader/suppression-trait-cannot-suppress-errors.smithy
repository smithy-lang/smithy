$version: "2"

namespace foo.baz

@suppress(["TraitTarget"])
@idempotent // < this is invalid
string MyString
