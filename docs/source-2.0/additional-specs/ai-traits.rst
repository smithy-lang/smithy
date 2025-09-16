.. _ai-traits:

================
Smithy AI Traits
================

Smithy AI traits provide metadata for service authors to embed contextual information to guide Large Language Models (LLMs) and other AI systems in understanding and interacting with services.


.. smithy-trait:: smithy.ai#prompts
.. _smithy.ai#prompts-trait:

----------------------------
``smithy.ai#prompts`` trait
----------------------------

Summary
    Defines prompt templates that provide contextual guidance to LLMs for understanding when and how to use operations or services. This trait can be used to generate prompts for a Model Context Protocol (MCP) server.
Trait selector
    ``:is(service, operation)``
Value type
    ``map`` where keys are prompt names and values are prompt template definitions.

The ``smithy.ai#prompts`` trait allows service authors to provide structured guidance to AI systems, including contextual descriptions, parameter templates with placeholder syntax, usage conditions, and workflow guidance for complex processes.

Each prompt template definition consists of the following components:

.. list-table::
    :header-rows: 1
    :widths: 15 15 70

    * - Property
      - Type
      - Description
    * - description
      - ``string``
      - **Required** A concise description of the prompt's purpose and functionality.
    * - template
      - ``string``
      - **Required** The prompt template text.
    * - arguments
      - ``string``
      - Optional reference to a structure shape that defines the parameters used in the template placeholders. Valid value MUST be a structure shapeId.
    * - preferWhen
      - ``string``
      - Optional condition to provide preference for tool selection.May be used for routing to other tools or prompts.


Template Placeholder Syntax
============================

Prompt templates use ``{{parameterName}}`` syntax to reference parameters from the associated ``arguments`` structure. This can be useful to reference and interpolate members of arguments when accepting user input for prompts in Model Context Protocol (MCP) servers.

.. code-block:: smithy

    @prompts({
        detailed_weather_report: {
            description: "Generate a comprehensive weather report"
            template: "Create a detailed weather report for {{location}} showing {{temperature}}°F with {{conditions}}. Include humidity at {{humidity}}% and provide recommendations for outdoor activities."
            arguments: DetailedWeatherInput
        }
    })

    structure DetailedWeatherInput {
        location: String
        temperature: Float
        conditions: String
        humidity: Float
    }


Service-level vs operation-level prompts
========================================

**Service-level prompts** SHOULD be used to define workflows that compose multiple operations, provide guidance about the service and offer prompts that make complex interactions with the service simple:

.. code-block:: smithy

    @prompts({
        service_overview: {
            description: "Overview of weather service capabilities"
            template: "This weather service provides current conditions and forecasts. Use GetCurrentWeather for immediate conditions, and leverage creative prompts like emoji_weather for enhanced user experience."
            preferWhen: "User needs to understand available weather service features"
        },
        vacation_weather_planner: {
            description: "Plan weather for vacation destinations"
            template: "Help plan a vacation by comparing weather between {{homeLocation}} and {{destinationLocation}}.
            Call GetCurrentWeather for both locations, then provide a recommendation on the best time to travel, what to pack, and activities to consider based on weather differences."
            arguments: VacationPlannerInput
        }
    })
    service WeatherService {
        operations: [GetCurrentWeather]
    }

**Operation-level prompts** SHOULD provide specific guidance for individual operations and their usage:

.. code-block:: smithy

    @prompts({
        emergency_weather_check: {
            description: "Quick weather check for emergency situations"
            template: "Immediately check weather conditions at {{location}} focusing on safety concerns. Prioritize severe weather alerts, visibility, and hazardous conditions."
            arguments: GetCurrentWeatherInput
            preferWhen: "Emergency situations requiring immediate weather assessment"
        }
    })
    operation GetCurrentWeather {
        input: GetCurrentWeatherInput
        output: GetCurrentWeatherOutput
    }

    structure GetCurrentWeatherInput {
        /// Location for getting weather
        @required
        location: String
    }


Use cases for prompts
=======================

Service authors can define sophisticated workflows by defining prompts to orchestrate multiple API calls and format results.

**Output formatting**

Service teams can control how LLMs present API results:

.. code-block:: smithy

    @prompts({
        weather_dashboard: {
            description: "Create a weather dashboard with multiple data points"
            template: "Get weather for {{location}} and create a dashboard view. Format as: 🌡️ Temperature: [temp]°F | 💧 Humidity: [humidity]% | 🌤️ Conditions: [conditions]. Add appropriate weather emoji and brief activity recommendations."
            arguments: GetCurrentWeatherInput
        }
    })

**Multi-operation workflows**

Prompts can guide LLMs to perform complex workflows using existing operations:

.. code-block:: smithy

    @prompts({
        vacation_weather_planner: {
            description: "Plan weather for vacation destinations"
            template: "Help plan a vacation by comparing weather between {{homeLocation}} and {{destinationLocation}}. Call GetCurrentWeather for both locations, then provide a recommendation on the best time to travel, what to pack, and activities to consider based on weather differences."
            arguments: VacationPlannerInput
        }
    })

    structure VacationPlannerInput {
        @required
        homeLocation: String
        @required
        destinationLocation: String
    }


A complete example
=====================

The following example demonstrates creative prompt templates that enhance the user experience with a weather service:

.. code-block:: smithy

    $version: "2"

    namespace example.weather

    use smithy.ai#prompts

    @prompts({
        emoji_weather: {
            description: "Get weather with emoji visualization"
            template: "Get current weather for {{location}} and display it with appropriate weather emojis (☀️ sunny, ⛅ partly cloudy, ☁️ cloudy, 🌧️ rainy, ⛈️ stormy, ❄️ snowy)."
            arguments: GetCurrentWeatherInput
            preferWhen: "User wants a fun, visual weather display"
        }
        travel_weather_advisor: {
            description: "Provides complete travel guidance according to the weather"
            template: "Get weather for {{location}} and provide travel advice. Include clothing recommendations based on temperature and conditions, suggest appropriate activities, and highlight any weather concerns."
            arguments: GetCurrentWeatherInput
            preferWhen: "User is planning travel or outdoor activities"
        }
        weather_comparison: {
            description: "Compare weather between multiple locations"
            template: "Compare current weather between {{location1}} and {{location2}}. Call GetCurrentWeather for each location, then present results in a table format showing temperature, conditions, and humidity. Highlight which location has better weather conditions."
            arguments: WeatherComparisonInput
            preferWhen: "User wants to compare weather across different cities"
        }
    })
    service WeatherService {
        version: "2024-01-01"
        operations: [GetCurrentWeather]
    }

    operation GetCurrentWeather {
        input: GetCurrentWeatherInput
        output: GetCurrentWeatherOutput
    }

    structure GetCurrentWeatherInput {
        /// Location to get weather for (city, coordinates, or address)
        @required
        location: String
    }

    structure GetCurrentWeatherOutput {
        temperature: Float
        humidity: Float
        conditions: String
    }

    structure WeatherComparisonInput {
        /// First location to compare
        @required
        location1: String

        /// Second location to compare
        @required
        location2: String
    }


Integration with Model Context Protocol
=======================================

The ``smithy.ai#prompts`` trait is designed to work with Model Context Protocol (MCP) servers. MCP servers can use the metadata defined in the trait to generate _prompts:https://modelcontextprotocol.io/specification/2025-06-18/server/prompts  as defined in the Model Context Protocol (MCP) specification.

See also
    - `MCP Server Example`_ for a complete implementation of an MCP server using Smithy
    - `MCP Traits Example`_ for additional examples of AI traits in practice
    - `MCP Server Model`_ for the Smithy model definition used in the MCP server example

.. _MCP Server Example: https://github.com/smithy-lang/smithy-java/tree/main/examples/mcp-server
.. _MCP Traits Example: https://github.com/smithy-lang/smithy-java/tree/main/examples/mcp-traits-example
.. _MCP Server Model: https://github.com/smithy-lang/smithy-java/blob/main/examples/mcp-server/src/main/resources/software/amazon/smithy/java/example/server/mcp/main.smithy
