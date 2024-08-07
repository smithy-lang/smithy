:hide-toc:

======
Smithy
======

**Smithy** is a language for defining services and SDKs.

.. code-block:: smithy
    :caption: Example Smithy service

    $version: "2"
    namespace example.weather

    service Weather {
        version: "2006-03-01"
        resources: [City]
        operations: [GetCurrentTime]
    }

    resource City {
        identifiers: { cityId: CityId }
        read: GetCity
        list: ListCities
        resources: [Forecast]
    }

.. rubric:: Next steps

* Find out more about Smithy models and this example in the :doc:`quickstart`.
* Find the source code on `Github <https://github.com/smithy-lang/smithy>`_.


Features
========

.. hlist::
    :columns: 2

    * .. rubric:: 🤖  Protocol-agnostic

      Smithy is designed to work with any programming language, describe
      services running in any environment, and work with any kind of
      transport or serialization format.
    * .. rubric:: 🧬  Designed to evolve

      Smithy is extended through traits. Smithy's extensible meta-model
      can evolve and capture more information about services without
      breaking changes.
    * .. rubric:: ⚖ Codify and enforce API governance

      Customizable API standards help to automatically ensure that APIs
      meet customer expectations. Validation rules can be shared and
      applied to all APIs in an organization.
    * .. rubric:: 🖇️  Resource based

      Smithy models are defined by resources and operations. Defining
      services with resources helps lead to better APIs and provides
      rich information for model transformations.


FAQ
===

Why did you develop Smithy?
---------------------------

Smithy is based on an interface definition language that has been widely used
within Amazon and AWS for over a decade. We decided to release Smithy publicly
to let other developers use Smithy for their own services and benefit from our
years of experience with building tens of thousands of services. By releasing
the Smithy specification and tooling, we also hope to make it easier for
developers to maintain open source AWS SDKs.

Does Smithy only work with AWS?
-------------------------------

No. Smithy can be used with any kind of service. All AWS-specific metadata in
Smithy is implemented as decoupled packages.

Why not just use an existing IDL?
---------------------------------

At AWS, we rely heavily on metadata, code generation, service frameworks,
client libraries, and automated policy enforcement. Existing IDLs available
outside of AWS were not extensible enough to meet our needs and not
compatible with our existing services.

How is Smithy different than other IDLs and frameworks?
-------------------------------------------------------

* Smithy is built for code generation and tools. Smithy models were designed
  for the purpose of generating code for multiple programming languages. For
  example, Smithy models are completely normalized, which gives all generated
  types an explicit name and makes models easy to traverse, validate, and diff.
* Smithy can be extended and constrained. Traits are used to extend a model
  and add capabilities that are not part of the core specification. For
  example, AWS defines AWS-specific traits that are used to generate other
  metadata documents that are derived from Smithy models. While extensibility
  is a key design requirement, Smithy provides a built-in validation system
  that ensures models adhere to a configurable set of rules and policies.
* Smithy is protocol agnostic. Smithy decouples the transport layer of web services
  from the data structures and capabilities of the service so that they can
  evolve independently.
* Smithy helps large organizations collaborate on APIs. Smithy allows
  different aspects of a model to be owned by different teams. We use this
  feature at AWS to allow the documentation teams to own API documentation,
  while service owners control the shape and operations of a service.
* Smithy models can be altered for different audiences. Smithy's projection
  system allows the contents of a model to be filtered and changed based on
  who a particular build of a model is intended for. This allows for use cases
  like maintaining an internal and external version of a model and providing
  beta models to customers as part of a private beta.

What does protocol-agnostic mean?
---------------------------------

Protocol-agnostic means that the model is an abstraction that specifies the
rules and semantics of how a client and server communicate. The transport and
serialization format of a service is left as an implementation detail,
allowing them to evolve over time. For example, a service owner may wish to
evolve their serialization format (e.g., move from text to binary), their
connection type (e.g., move from HTTP/1 to HTTP/2), or launch entirely new
capabilities.

What is the main difference between Smithy and OpenAPI?
-------------------------------------------------------

The primary difference between Smithy and OpenAPI is that Smithy is
protocol-agnostic, allowing Smithy to describe a broader range of services,
metadata, and capabilities. Smithy can be used alongside OpenAPI by
:ref:`converting Smithy models to OpenAPI <smithy-to-openapi>`.

What can Smithy do today?
-------------------------

See https://github.com/smithy-lang/awesome-smithy.


Read more
=========

.. toctree::
    :maxdepth: 1

    quickstart
    spec/index
    trait-index
    guides/index
    tutorials/index
    Additional specs <additional-specs/index>
    aws/index
    ts-ssdk/index

.. toctree::
    :caption: Project
    :maxdepth: 1

    Source code <https://github.com/smithy-lang/smithy>
    Awesome Smithy <https://github.com/smithy-lang/awesome-smithy>
    Smithy Examples <https://github.com/smithy-lang/smithy-examples>
    1.0 Documentation <https://smithy.io/1.0/>
