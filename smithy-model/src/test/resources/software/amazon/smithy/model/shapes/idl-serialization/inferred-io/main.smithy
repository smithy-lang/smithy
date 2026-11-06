$version: "2"

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
