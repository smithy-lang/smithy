$version: "2.0"

namespace aws.protocoltests.restjson.nested

// Note that this conflicts with the shared-types GreetingStruct
// and needs to be renamed if used as part of a service closure.
structure GreetingStruct {
    salutation: String,
}
