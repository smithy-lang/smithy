//[ERROR] example.weather#City: Parse error at line 10, column 2 near `\n`: Resource properties can only be used with Smithy version 2 or later. Attempted to use resource properties with version `1.0`. | Model
$version: "1.0"

namespace example.weather

resource City {
    identifiers: { cityId: CityId },
    properties: {
    }
}

@pattern("^[A-Za-z0-9 ]+$")
string CityId
