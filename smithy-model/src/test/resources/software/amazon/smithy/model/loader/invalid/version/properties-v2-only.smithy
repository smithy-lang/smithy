//Resource properties can only be used with Smithy version 2 or later.

namespace example.weather

resource City {
    identifiers: { cityId: CityId },
    properties: {
    }
}

@pattern("^[A-Za-z0-9 ]+$")
string CityId
