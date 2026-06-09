$version: "2.0"

namespace ns.foo

// Structure references a target that does not exist, producing an ERROR
// event during assembly. Used by toProjectedModelReturnsBrokenBaseModelWithoutThrowing.
structure BadRef {
    ref: DoesNotExist
}
