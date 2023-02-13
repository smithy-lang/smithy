$version: "2.0"

namespace smithy.example

use smithy.protocols#rpcv2

@rpcv2(format: ["cbor"])
service ValidService1 {
    version: "2023-02-10"
}

@rpcv2(format: [])
service InvalidService1 {
    version: "2023-02-10"
}

@rpcv2(format: ["invalid-wire-format"])
service InvalidService2 {
    version: "2023-02-10"
}

@rpcv2(format: ["cbor", "invalid-wire-format"])
service InvalidService3 {
    version: "2023-02-10"
}

@rpcv2(format: ["invalid-wire-format1", "invalid-wire-format2"])
service InvalidService4 {
    version: "2023-02-10"
}
