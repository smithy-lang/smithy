$version: "2.0"

namespace ns.foo

@tags(["toAdd"])
structure TestA {}

@tags(["toRemove"])
structure TestB {}

@tags(["to", "switch"])
structure TestC {}
