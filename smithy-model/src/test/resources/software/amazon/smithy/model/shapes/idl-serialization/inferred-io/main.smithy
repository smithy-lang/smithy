$version: "2.0"

namespace com.example

service InlineService {
    operations: [
        MixedWithCustomA
        MixedWithCustomB
        MixedWithDefault
        SharedCustomA
        SharedCustomB
        UsesDefaults
    ]
}
