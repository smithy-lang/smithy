$version: "2.0"

namespace smithy.example

use aws.api#clientOptional

structure Foo {
    @required
    @clientOptional
    baz: String

    @default
    @clientOptional
    bar: String

    // this is fine, but unnecessary
    @clientOptional
    bam: String
}
