$version: "2.0"

namespace ns.foo

@cors(
    origin: "https://example.com"
    origins: {
        prod: "https://prod.example.com"
    }
)
service InvalidService {
    version: "2024-01-01"
}

@cors(
    origin: "https://example.com"
)
service ValidOriginOnly {
    version: "2024-01-01"
}

@cors(
    origins: {
        prod: "https://prod.example.com"
        beta: "https://beta.example.com"
    }
)
service ValidOriginsOnly {
    version: "2024-01-01"
}

@cors
service ValidDefaults {
    version: "2024-01-01"
}

@cors(
    origin: "*"
    origins: {
        prod: "https://prod.example.com"
    }
)
service ValidExplicitDefaultWithOrigins {
    version: "2024-01-01"
}
