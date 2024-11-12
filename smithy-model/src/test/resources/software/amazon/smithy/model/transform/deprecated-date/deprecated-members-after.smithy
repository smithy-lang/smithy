$version: "2"

namespace smithy.example

structure MyStruct {
    @deprecated(message: "Should NOT be filtered as it is deprecated at the same date as the filter", since: "2024-10-10")
    notFilteredEquals: String

    @deprecated(message: "Should NOT be filtered as it is deprecated after the filter date", since: "2024-10-11")
    notFilteredAfter: String
}
