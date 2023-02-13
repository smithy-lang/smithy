$version: "2.0"

namespace smithy.example

use smithy.protocols#rpcv2

@rpcv2(http: ["h2", "http/1.1"], eventStreamHttp: ["h2"], format: ["cbor"])
service ValidService1 {
    version: "2023-02-10"
}

@rpcv2(http: ["h2"], eventStreamHttp: ["h2"], format: ["cbor"])
service ValidService2 {
    version: "2023-02-10"
}

@rpcv2(http: [], eventStreamHttp: [], format: ["cbor"])
service ValidService3 {
    version: "2023-02-10"
}

@rpcv2(http: ["http/1.1"], eventStreamHttp: [], format: ["cbor"])
service ValidService4 {
    version: "2023-02-10"
}

@rpcv2(eventStreamHttp: ["http/1.1"], format: ["cbor"])
service InvalidService1 {
    version: "2023-02-10"
}

@rpcv2(http: ["h2"], eventStreamHttp: ["http/1.1"], format: ["cbor"])
service InvalidService2 {
    version: "2023-02-10"
}

@rpcv2(http: ["h2"], eventStreamHttp: ["h2", "http/1.1", "h2c"], format: ["cbor"])
service InvalidService3 {
    version: "2023-02-10"
}
