$version: "2.0"

namespace smithy.example

use smithy.protocols#rpcv2Json

@rpcv2Json(http: ["h2", "http/1.1"], eventStreamHttp: ["h2"])
service ValidService1 {
    version: "2023-02-10"
}

@rpcv2Json(http: ["h2"], eventStreamHttp: ["h2"])
service ValidService2 {
    version: "2023-02-10"
}

@rpcv2Json(http: [], eventStreamHttp: [])
service ValidService3 {
    version: "2023-02-10"
}

@rpcv2Json(http: ["http/1.1"], eventStreamHttp: [])
service ValidService4 {
    version: "2023-02-10"
}

@rpcv2Json(eventStreamHttp: ["http/1.1"])
service InvalidService1 {
    version: "2023-02-10"
}

@rpcv2Json(http: ["h2"], eventStreamHttp: ["http/1.1"])
service InvalidService2 {
    version: "2023-02-10"
}

@rpcv2Json(http: ["h2"], eventStreamHttp: ["h2", "http/1.1", "h2c"])
service InvalidService3 {
    version: "2023-02-10"
}
