$version: "2"

metadata suppressions = [
    {
        id: "HttpMethodSemantics"
        namespace: "smithy.benchmark.serde"
        reason: """
            These models mirror the wire shape of existing AWS operations so that
            serialization and deserialization can be measured against realistic
            payloads. Matching those operations takes priority over HTTP method
            semantics."""
    }
    {
        id: "HttpResponseCodeSemantics"
        namespace: "smithy.benchmark.serde"
        reason: """
            These models mirror the response codes of existing AWS operations, which
            do not always follow the recommended semantics."""
    }
    {
        id: "PaginatedTrait"
        namespace: "smithy.benchmark.serde"
        reason: """
            Pagination is not exercised by these benchmarks, so the operations they
            model are not marked as paginated even where the real operations are."""
    }
    {
        id: "HttpHeaderTrait"
        namespace: "smithy.benchmark.serde"
        reason: """
            These models use the same headers as the AWS operations they mirror,
            including headers that are otherwise discouraged."""
    }
    {
        id: "HttpUriConflict"
        namespace: "smithy.benchmark.serde"
        reason: """
            The same operations are modeled once per protocol so that the same
            workload can be compared across protocols, which necessarily reuses
            URIs."""
    }
    {
        id: "Service"
        namespace: "smithy.benchmark.serde"
        reason: """
            These services exist only to host benchmark operations and are never
            deployed, so they do not follow all service modeling recommendations."""
    }
    {
        id: "UnstableTrait"
        namespace: "smithy.benchmark.serde"
        reason: """
            Benchmarking protocol serialization requires applying protocol traits
            that are still marked unstable."""
    }
    {
        id: "HttpChecksumTrait"
        namespace: "smithy.benchmark.serde"
        reason: """
            Checksum behavior is benchmarked as the AWS operations define it, which
            does not always match the trait's recommendations."""
    }
]
