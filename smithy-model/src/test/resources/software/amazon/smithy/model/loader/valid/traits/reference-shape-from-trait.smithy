$version: "2.0"

namespace smithy.example

@trait
@idRef
string link

structure StructA {
    id: String
}

structure StructB {
    @link(StructA$id)
    structAId: String
}

structure StructC {
    @link(smithy.example#StructA$id)
    structAId: String
}

structure StructD {
    @link(smithy.example#StructA)
    structAId: String
}
