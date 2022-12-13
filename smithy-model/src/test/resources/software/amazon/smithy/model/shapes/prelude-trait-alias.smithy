$version: "2"

namespace ns.foo

@deprecated
@sensitive
@smithy.api#sensitive
string String

@trait
structure sensitive {}
