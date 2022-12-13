$version: "2.0"

namespace smithy.example

/// Base enum
@mixin
@private
enum BaseEnum {
    FOO

    /// Documentation
    BAR
}

/// Mixed enum
@sensitive
enum MixedEnum with [BaseEnum] {
    /// Docs
    BAR
    BAZ
}

/// Base int enum
@mixin
@private
intEnum BaseIntEnum {
    @enumValue(1)
    FOO

    /// Documentation
    @enumValue(2)
    BAR
}

/// Mixed int enum
@sensitive
intEnum MixedIntEnum with [BaseIntEnum] {
    /// Docs
    BAR

    @enumValue(3)
    BAZ
}
