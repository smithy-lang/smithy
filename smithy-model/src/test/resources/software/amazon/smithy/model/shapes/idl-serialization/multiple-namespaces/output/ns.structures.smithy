$version: "1.1"

metadata shared = true

namespace ns.structures

use ns.primitives#StringList

structure Structure {
    listMember: StringList
    stringMember: ns.primitives#String
}
