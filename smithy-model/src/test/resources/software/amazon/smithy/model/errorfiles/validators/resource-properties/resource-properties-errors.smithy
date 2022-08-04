$version: "2.0"
namespace smithy.example

resource Forecast {
    properties: {
        chanceOfRain: Float
        booleanProperty: Boolean
    }
    read: GetForecast
}

@readonly
operation GetForecast {
   input: GetForecastInput
   output: GetForecastOutput
}

structure GetForecastOutput {
    chanceOfRain: Float
    @property(name: "chanceOfRain")
    chancesOfRain: Float
}

structure GetForecastInput {
    @property(name: "chanceOfRain")
    chancesOfRain: Float

    @property(name: "chanceOfRain")
    howLikelyToRain: Float

    memberIsNotProperty: String

    /// Mismatch type error
    booleanProperty: String
}

