$version: "2.0"

namespace smithy.example

use aws.protocols#restJson1

@restJson1(http: ["h2", "http/1.1"], eventStreamHttp: ["h2"])
service ValidService1 {
    version: "2020-04-02"
}

@restJson1(http: ["h2"], eventStreamHttp: ["h2"])
service ValidService2 {
    version: "2020-04-02"
}

@restJson1(http: [], eventStreamHttp: [])
service ValidService3 {
    version: "2020-04-02"
}

@restJson1(http: ["http/1.1"], eventStreamHttp: [])
service ValidService4 {
    version: "2020-04-02"
}

@restJson1(eventStreamHttp: ["http/1.1"])
service InvalidService1 {
    version: "2020-04-02"
}

@restJson1(http: ["h2"], eventStreamHttp: ["http/1.1"])
service InvalidService2 {
    version: "2020-04-02"
}

@restJson1(http: ["h2"], eventStreamHttp: ["h2", "http/1.1", "h2c"])
service InvalidService3 {
    version: "2020-04-02"
}
