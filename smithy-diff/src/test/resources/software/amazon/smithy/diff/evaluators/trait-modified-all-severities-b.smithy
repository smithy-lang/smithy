$version: "2.0"

namespace smithy.example

@aTrait(
    a: "a",    // add
    // b: "a", // remove
    c: "c",    // update
    d: "d",    // const
    e: "a",    // add
    // f: "a", // remove
    g: "h",    // update
    h: "h",    // const
    i: "a",    // add
    // j: "a", // remove
    k: "k",    // update
    l: "l",    // const
)
string Foo

@trait
@tags(["diff.contents"])
structure aTrait {
    @tags(["diff.error.add"])
    a: String,

    @tags(["diff.error.remove"])
    b: String,

    @tags(["diff.error.update"])
    c: String,

    @tags(["diff.error.const"])
    d: String,

    @tags(["diff.danger.add"])
    e: String,

    @tags(["diff.danger.remove"])
    f: String,

    @tags(["diff.danger.update"])
    g: String,

    @tags(["diff.danger.const"])
    h: String,

    @tags(["diff.warning.add"])
    i: String,

    @tags(["diff.warning.remove"])
    j: String,

    @tags(["diff.warning.update"])
    k: String,

    @tags(["diff.warning.const"])
    l: String,
}
