$version: "2"

namespace smithy.example

@trait
@deprecated(message: "Should be filtered as it is deprecated before date", since: "2024-10-09")
structure FilteredBeforeHyphens {}

@trait
@deprecated(message: "Should be filtered as it is deprecated before date", since: "20241009")
structure FilteredBeforeNoHyphens {}

@trait
@deprecated(message: "Should be filtered as it is deprecated before date", since: "2024-1009")
structure FilteredBeforeMixedHyphens {}

@trait
@deprecated(message: "Should NOT be filtered as it is deprecated at the same date as the filter", since: "2024-10-10")
structure NotFilteredEquals {}

@trait
@deprecated(message: "Should NOT be filtered as it is deprecated after the filter date", since: "2024-10-11")
structure NotFilteredAfter {}

@FilteredBeforeHyphens
@FilteredBeforeNoHyphens
@FilteredBeforeMixedHyphens
@NotFilteredEquals
@NotFilteredAfter
structure MyStruct {}
