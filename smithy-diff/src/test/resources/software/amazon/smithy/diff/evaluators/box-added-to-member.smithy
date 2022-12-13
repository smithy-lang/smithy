$version: "1.0"

namespace smithy.example

structure Example {
    // @box - added in the test and after upgrading to a v2 semantic model.
    baz: MyBoolean

    // @box - added in the test and after upgrading to a v2 semantic model.
    @required
    bam: MyBoolean
}

@box
boolean MyBoolean
