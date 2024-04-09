$version: "2.0"
namespace smithy.example

// See the DiffCommandTest#projectModeUsesConfigOfOldModel integration test.
//
// This is to cause a diff event when compared against simple-config-sources, ensuring that the config file of this
// project is used and not the config file of the "new" model. If the new model's config was also loaded, the
// integration test would create a shape conflict error after loading.
integer MyString
