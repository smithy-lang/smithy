$version: "2.0"

namespace smithy.example

@deprecated(message: "Should be filtered as it is deprecated before date", since: "2024-10-09")
structure FilteredDateBefore {
    field: String
}

@deprecated(message: "Should NOT be filtered as it is deprecated at the same date as the filter", since: "2024-10-10")
structure NotFilteredDateEquals {
    field: String
}

@deprecated(message: "Should NOT be filtered as it is deprecated after the filter date", since: "2024-10-11")
structure NotFilteredDateAfter {
    field: String
}

@deprecated(message: "Should be filtered as it is deprecated before version", since: "1.0.0")
structure FilteredVersionBefore {
    field: String
}

@deprecated(message: "Should NOT be filtered as it is deprecated at the same version as the filter", since: "1.1.0")
structure NotFilteredVersionEquals {
    field: String
}

@deprecated(message: "Should NOT be filtered as it is deprecated after the filter version", since: "1.1.1")
structure NotFilteredVersionAfter {
    field: String
}
