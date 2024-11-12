$version: "2"

namespace smithy.example

structure MyStruct {
    @deprecated(message: "Should be filtered as it is deprecated before version", since: "1.0.0")
    filteredBefore: String

    @deprecated(message: "Should NOT be filtered as it is deprecated at the same version as the filter", since: "1.1.0")
    notFilteredEquals: String

    @deprecated(message: "Should NOT be filtered as it is deprecated after the filter version", since: "1.1.1")
    notFilteredAfter: String
}
