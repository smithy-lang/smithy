$version: "1.0"

metadata shared = true

namespace ns.structures

use ns.primitives#StringList

structure Structure {
    listMember: StringList,
    stringMember: ns.primitives#String,
}
