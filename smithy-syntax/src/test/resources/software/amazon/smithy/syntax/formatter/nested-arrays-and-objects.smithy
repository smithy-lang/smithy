$version: "2.0"

metadata foo = {bar: ["baz", "foo", "bar", "baz"], baz: {foo: ["bar"]}}

metadata bar = [{foo: [{bar: ["baz", "foo"], baz: ["foo", "bar"]}, {foo: "bar"}]}]

namespace smithy.example
