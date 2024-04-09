============
smithy-build
============

Building a Smithy model requires constructing a configuration file,
``smithy-build.json``. This file is used to describe how a model is created
and what projections of the model to create.


.. _smithy-build-json:

Using ``smithy-build.json``
===========================

The ``smithy-build.json`` file is used to describe how a model is created and
what projections of the model to create.

The configuration file accepts the following properties:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - version
      - ``string``
      - **Required.** Defines the version of smithy-build. Set to `1.0`.
    * - outputDirectory
      - ``string``
      - The location where projections are written. Each
        projection will create a subdirectory named after the projection, and
        the artifacts from the projection, including a ``model.json`` file,
        will be placed in the directory.
    * - imports
      - ``string[]``
      - Provides a list of relative imports to combine into a single model.
        When a directory is encountered, all files and all files within all
        subdirectories are imported. Note that build systems MAY choose to rely
        on other mechanisms for importing models and forming a composite model.
        These imports are used in every projection. Note: imports are relative
        to the configuration file.
    * - projections
      - ``map<string, object>``
      - A map of projection names to projection configurations.
    * - plugins
      - ``map<string, object>``
      - Defines the plugins to apply to the model when building every
        projection. Plugins are a mapping of plugin names to an arbitrary
        plugin configuration object.
    * - ignoreMissingPlugins
      - ``bool``
      - If a plugin can't be found, Smithy will by default fail the build. This
        setting can be set to ``true`` to allow the build to progress even if
        a plugin can't be found on the classpath.

The following is an example ``smithy-build.json`` configuration:

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "outputDirectory": "build/output",
            "imports": ["foo.json", "some/directory"],
            "projections": {
                "my-abstract-projection": {
                    "abstract": true
                },
                "projection-name": {
                    "imports": ["projection-specific-imports/"],
                    "transforms": [
                        {
                            "name": "excludeShapesByTag",
                            "args": {
                                "tags": ["internal", "beta", "..."]
                            }
                        },
                        {
                            "name": "excludeShapesByTrait",
                            "args": {
                                "traits": ["internal"]
                            }
                        }
                    ],
                    "plugins": {
                        "plugin-name": {
                            "plugin-config": "value"
                        },
                        "...": {}
                    }
                }
            },
            "plugins": {
                "plugin-name": {
                    "plugin-config": "value"
                },
                "...": {}
            }
        }


.. _projections:

Projections
===========

A projection of a model is a filtered and modified version of a Smithy model
that is intended for specific audiences or customers. Projections are
useful to companies that maintain internal and external versions of an API
or include parameters and operations that are available to only a subset of
their customers.

Projections are defined in the smithy-build.json file in the ``projections``
property. Projection names MUST match the following pattern: ``^[A-Za-z0-9\-_.]+$``.

A projection accepts the following configuration:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - abstract
      - ``boolean``
      - Defines the projection as a placeholder that other projections apply.
        Smithy will not build artifacts for abstract projections. Abstract
        projections must not define ``imports`` or ``plugins``.
    * - imports
      - ``string[]``
      - Provides a list of relative imports to include when building this
        specific projection. When a directory is encountered, all files and
        all files within all subdirectories are imported. Note: imports are
        relative to the configuration file.
    * - transforms
      - ``list<Transforms>``
      - Defines the transformations to apply to the projection.
        Transformations are used to remove shapes, remove traits, modify trait
        contents, and any other kind of transformation necessary for the
        projection. Transforms are applied in the order defined.
    * - plugins
      - ``map<string, object>``
      - Defines the plugins to apply to the model when building this
        projection. Plugins are a mapping of plugin names to an arbitrary
        plugin configuration object. smithy-build will attempt to resolve
        plugin names using `Java SPI`_ to locate an instance of ``software.amazon.smithy.build.SmithyBuildPlugin``
        that returns a matching name when calling ``getName``. smithy-build will
        emit a warning when a plugin cannot be resolved.


.. _projection-artifacts:

Projection artifacts
--------------------

smithy-build will write artifacts for each projection inside of
`outputDirectory`.

* The model that is projected is placed inside of ``${outputDirectory}/${projectionName}/model/model.json``.
* Build information about the projection build result, including the
  configuration of the projection and the validation events encountered when
  validating the projected model, are written to ``${outputDirectory}/${projectionName}/build-info/smithy-build-info.json``.
* All plugin artifacts are written to ``${outputDirectory}/${projectionName}/${pluginName}/${artifactName}``,
  where ``${artifactName}`` is the name of an artifact contributed by an
  instance of ``software.amazon.smithy.build.SmithyBuildPlugin``. The relative
  path of each artifact is resolved against ``${outputDirectory}/${projectionName}/${pluginName}/``.
  For example, given an artifact path of ``foo/baz.json``, the resolved path
  would become ``${outputDirectory}/${projectionName}/${pluginName}/foo/baz.json``.


.. _transforms:

Transforms
==========

Transforms are used to filter and modify the model for the projection.
Transforms are applied to the model, in order.

A transform accepts the following configuration:

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - name
      - ``string``
      - The required name of the transform.
    * - args
      - ``structure``
      - A structure that contains configuration key-value pairs.


.. _apply-transform:

apply
-----

Applies the transforms defined in the given projection names.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - projections
      - ``[string]``
      - The ordered list of projection names to apply. Each provided
        name must be a valid projection name. The transforms of the
        referenced projections are applied in the order provided.
        No cycles are allowed in ``apply``.

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "my-abstract-projection": {
                    "abstract": true,
                    "transforms": [
                        {"name": "foo"}
                    ]
                },
                "projection-name": {
                    "imports": ["projection-specific-imports/"],
                    "transforms": [
                        {"name": "baz"},
                        {
                            "name": "apply",
                            "args": {
                                "projections": ["my-abstract-projection"]
                            }
                        },
                        {"name": "bar"}
                    ]
                }
            }
        }


.. _changeStringEnumsToEnumShapes:

changeStringEnumsToEnumShapes
-----------------------------

Changes string shapes with enum traits into enum shapes.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - synthesizeNames
      - ``boolean``
      - Whether enums without names should have names synthesized if possible.

This transformer will attempt to convert strings with :ref:`enum traits <enum-trait>`
into enum shapes. To successfully convert to an enum shape, each enum
definition within the source enum trait must have a ``name`` property, or the
``synthesizeNames`` transform config option must be enabled. By default, ``synthesizeNames``
is disabled. If an enum definition from the enum trait is marked as deprecated, the
:ref:`deprecated trait <deprecated-trait>` will be applied to the resulting
enum shape member. Tags on the enum definition will be converted to a :ref:`tags trait <tags-trait>`
on the enum shape member. Additionally, if the enum definition is tagged as
internal, the enum shape member will have the :ref:`internal trait <internal-trait>`
applied.

.. code-block:: json

    {
        "version": "1.0",
        "projections": {
            "exampleProjection": {
                "transforms": [
                    {
                        "name": "changeStringEnumsToEnumShapes",
                        "args": {
                            "synthesizeNames": true
                        }
                    }
                ]
            }
        }
    }


.. _changeTypes:

changeTypes
-----------

Changes the types of shapes.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - shapeTypes
      - ``Map<ShapeId, String>``
      - A map of shape IDs to the type to assign to the shape.
    * - synthesizeEnumNames
      - ``boolean``
      - Whether enums without names should have names synthesized if possible.

Only the following shape type changes are supported:

* Any simple type to any other simple type
* List to set
* Set to list
* Structure to union
* Union to structure
* String to enum

.. code-block:: json

    {
        "version": "1.0",
        "projections": {
            "exampleProjection": {
                "transforms": [
                    {
                        "name": "changeTypes",
                        "args": {
                            "shapeTypes": {
                                "smithy.example#Foo": "string",
                                "smithy.example#Baz": "union",
                                "smithy.example#Qux": "enum"
                            },
                            "synthesizeEnumNames": true
                        }
                    }
                ]
            }
        }
    }


.. seealso:: :ref:`changeStringEnumsToEnumShapes`


.. _excludeShapesByTag-transform:

excludeShapesByTag
------------------

Removes shapes if they are tagged with one or more of the given ``tags`` via
the :ref:`tags trait <tags-trait>`.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - tags
      - ``[string]``
      - The set of tags that causes shapes to be removed.

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "excludeShapesByTag",
                            "args": {
                                "tags": ["foo", "baz"]
                            }
                        }
                    ]
                }
            }
        }

.. note::

    This transformer does not remove shapes from the prelude.


.. _excludeShapesByTrait-transform:

excludeShapesByTrait
--------------------

Removes shapes if they are marked with one or more specific traits.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - traits
      - ``[string]``
      - A list of trait :ref:`shape IDs <shape-id>`. If any of these traits
        are found on a shape, the shape is removed from the model. Relative
        shape IDs are assumed to be in the ``smithy.api``
        :ref:`prelude <prelude>` namespace.

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "excludeShapesByTrait",
                            "args": {
                                "traits": ["internal"]
                            }
                        }
                    ]
                }
            }
        }


.. _includeShapesByTag-transform:

includeShapesByTag
------------------

Removes shapes that are not tagged with at least one of the given ``tags``
via the :ref:`tags trait <tags-trait>`.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - tags
      - ``[string]``
      - The set of tags that causes shapes to be retained in the model.

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "includeShapesByTag",
                            "args": {
                                "tags": ["foo", "baz"]
                            }
                        }
                    ]
                }
            }
        }

.. note::

    This transformer does not remove shapes from the prelude.


.. _includeNamespaces-transform:

includeNamespaces
-----------------

Filters out shapes that are not part of one of the given :ref:`namespaces <namespaces>`.
Note that this does not filter out traits based on namespaces.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - namespaces
      - ``[string]``
      - The namespaces to include in the model.

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "includeNamespaces",
                            "args": {
                                "namespaces": ["com.foo.bar", "my.api"]
                            }
                        }
                    ]
                }
            }
        }

.. note::

    This transformer does not remove shapes from the prelude.


.. _includeServices-transform:

includeServices
---------------

Filters out service shapes that are not included in the ``services`` list of
shape IDs.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - services
      - ``[string]``
      - The service shape IDs to include in the model. Each entry MUST be
        a valid service shape ID.

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "includeServices",
                            "args": {
                                "services": ["my.api#MyService"]
                            }
                        }
                    ]
                }
            }
        }


.. _excludeTags-transform:

excludeTags
-----------

Removes tags from shapes and trait definitions that match any of the
provided ``tags``.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - tags
      - ``[string]``
      - The set of tags that are removed from the model.

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "excludeTags",
                            "args": {
                                "tags": ["tagA", "tagB"]
                            }
                        }
                    ]
                }
            }
        }


.. _excludeTraits-transform:

excludeTraits
-------------

Removes trait definitions from a model if the trait name is present in the
provided list of ``traits``. Any instance of a removed trait is also removed
from shapes in the model.

The shapes that make up trait definitions that are removed *are not*
automatically removed from the model. Use ``removeUnusedShapes`` to remove
orphaned shapes.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - traits
      - ``[string]``
      - The set of traits that are removed from the model. Arguments that
        end with "#" exclude the traits of an entire namespace. Trait
        shape IDs that are relative are assumed to be part of the
        ``smithy.api`` prelude namespace.

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "excludeTraits",
                            "args": {
                                "traits": ["since", "com.foo#customTrait"]
                            }
                        }
                    ]
                }
            }
        }


You can exclude all of the traits in a namespace by ending one of the
arguments with "#". For example, the following configuration excludes
all traits in the "example.foo" namespace:

.. tabs::

    .. code-tab:: json
    
        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "excludeTraits",
                            "args": {
                                "traits": ["example.foo#"]
                            }
                        }
                    ]
                }
            }
        }


.. _excludeTraitsByTag-transform:

excludeTraitsByTag
------------------

Removes trait definitions from a model if the trait definition has any of
the provided :ref:`tags <tags-trait>`. Any instance of a removed trait is
also removed from shapes in the model.

The shapes that make up trait definitions that are removed *are not*
automatically removed from the model. Use ``removeUnusedShapes`` to remove
orphaned shapes.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - tags
      - ``[string]``
      - The list of tags that, if present, cause a trait to be removed.

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "excludeTraitsByTag",
                            "args": {
                                "tags": ["internal"]
                            }
                        }
                    ]
                }
            }
        }

.. note::

    This transformer does not remove shapes from the prelude.


.. _filterSuppressions-transform:

filterSuppressions
------------------

Removes and modifies suppressions found in :ref:`metadata <suppression-definition>`
and the :ref:`suppress-trait`.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - removeUnused
      - ``boolean``
      - Set to true to remove suppressions that have no effect.

        Shapes and validators are often removed when creating a filtered
        version of model. After removing shapes and validators, suppressions
        could be left in the model that no longer have any effect. These
        suppressions could inadvertently disclose information about private
        or unreleased features.

        If a validation event ID is never emitted, then ``@suppress`` traits
        will be updated to no longer refer to the ID and removed if they no
        longer refer to any event. Metadata suppressions are also removed if
        they have no effect.
    * - removeReasons
      - ``boolean``
      - Set to true to remove the ``reason`` property from metadata suppressions.
        The reason for a suppression could reveal internal or sensitive
        information. Removing the "reason" from metadata suppressions is an
        extra step teams can take to ensure they do not leak internal
        information when publishing models outside of their organization.
    * - eventIdAllowList
      - ``[string]``
      - Sets a list of event IDs that can be referred to in suppressions.
        Suppressions that refer to any other event ID will be updated to
        no longer refer to them, or removed if they no longer refer to any
        events.

        This setting cannot be used in tandem with ``eventIdDenyList``.
    * - eventIdDenyList
      - ``[string]``
      - Sets a list of event IDs that cannot be referred to in suppressions.
        Suppressions that refer to any of these event IDs will be updated to
        no longer refer to them, or removed if they no longer refer to any
        events.

        This setting cannot be used in tandem with ``eventIdAllowList``.
    * - namespaceAllowList
      - ``[string]``
      - Sets a list of namespaces that can be referred to in metadata
        suppressions. Metadata suppressions that refer to namespaces
        outside of this list, including "*", will be removed.

        This setting cannot be used in tandem with ``namespaceDenyList``.
    * - namespaceDenyList
      - ``[string]``
      - Sets a list of namespaces that cannot be referred to in metadata
        suppressions. Metadata suppressions that refer to namespaces
        in this list, including "*", will be removed.

        This setting cannot be used in tandem with ``namespaceAllowList``.

The following example removes suppressions that have no effect in the
``exampleProjection``:

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "filterSuppressions",
                            "args": {
                                "removeUnused": true
                            }
                        }
                    ]
                }
            }
        }

The following example removes suppressions from metadata that refer to
deny-listed namespaces:

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "filterSuppressions",
                            "args": {
                                "namespaceDenyList": ["com.internal"]
                            }
                        }
                    ]
                }
            }
        }

The following example removes suppressions from metadata that refer to
namespaces outside of the allow-listed namespaces:

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "filterSuppressions",
                            "args": {
                                "namespaceAllowList": ["com.external"]
                            }
                        }
                    ]
                }
            }
        }

The following example removes suppressions that refer to deny-listed event IDs:

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "filterSuppressions",
                            "args": {
                                "eventIdDenyList": ["MyInternalValidator"]
                            }
                        }
                    ]
                }
            }
        }

The following example removes suppressions that refer to event IDs outside
of the event ID allow list:

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "filterSuppressions",
                            "args": {
                                "eventIdAllowList": ["A", "B", "C"]
                            }
                        }
                    ]
                }
            }
        }

The following example removes the ``reason`` property from metadata
suppressions:

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "filterSuppressions",
                            "args": {
                                "removeReasons": true
                            }
                        }
                    ]
                }
            }
        }


.. _includeTags-transform:

includeTags
-----------

Removes tags from shapes and trait definitions that are not in the ``tags``
list.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - tags
      - ``[string]``
      - The set of tags that are retained in the model.

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "includeTags",
                            "args": {
                                "tags": ["foo", "baz"]
                            }
                        }
                    ]
                }
            }
        }


.. _includeTraits-transform:

includeTraits
-------------

Removes trait definitions from a model if the trait name is not present in the
provided list of ``traits``. Any instance of a removed trait is also removed
from shapes in the model.

The shapes that make up trait definitions that are removed *are not*
automatically removed from the model. Use ``removeUnusedShapes`` to remove
orphaned shapes.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - traits
      - ``[string]``
      - The list of trait shape IDs to include. A trait ID that ends with "#"
        will include all traits from a namespace. Trait shape IDs that are
        relative are assumed to be part of the ``smithy.api``
        prelude namespace.

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "includeTraits",
                            "args": {
                                "traits": ["sensitive", "com.foo.baz#customTrait"]
                            }
                        }
                    ]
                }
            }
        }


You can include all of the traits in a namespace by ending one of the
arguments with "#". For example, the following configuration includes
all traits in the "smithy.api" namespace:

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "includeTraits",
                            "args": {
                                "traits": ["smithy.api#"]
                            }
                        }
                    ]
                }
            }
        }


.. _includeTraitsByTag-transform:

includeTraitsByTag
------------------

Removes trait definitions from a model if the trait definition does not
contain one of the provided :ref:`tags <tags-trait>`. Any instance of a
removed trait definition is also removed from shapes in the model.

The shapes that make up trait definitions that are removed *are not*
automatically removed from the model. Use ``removeUnusedShapes`` to remove
orphaned shapes.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - tags
      - ``[string]``
      - The list of tags that must be present for a trait to be included
        in the filtered model.

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "includeTraitsByTag",
                            "args": {
                                "tags": ["public"]
                            }
                        }
                    ]
                }
            }
        }

.. note::

    This transformer does not remove shapes from the prelude.


.. _excludeMetadata-transform:

excludeMetadata
---------------

Removes model :ref:`metadata <metadata>` key-value pairs from a model if the
key is in the provided ``keys`` list.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - keys
      - ``[string]``
      - The set of metadata keys that are removed from the model.

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "excludeMetadata",
                            "args": {
                                "keys": ["suppressions"]
                            }
                        }
                    ]
                }
            }
        }


.. _includeMetadata-transform:

includeMetadata
---------------

Removes model :ref:`metadata <metadata>` key-value pairs from a model if the
key is not in the provided ``keys`` list.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - keys
      - ``[string]``
      - The set of metadata keys that are retained in the model.

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "includeMetadata",
                            "args": {
                                "keys": ["authors"]
                            }
                        }
                    ]
                }
            }
        }

.. _flattenNamespaces:

flattenNamespaces
-----------------

Flattens namespaces of any shapes connected to a service into a target
namespace. Shapes not connected to a service will not be flattened.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - namespace
      - ``string``
      - **REQUIRED** The target namespace.
    * - service
      - ``shapeId``
      - **REQUIRED** The service to be flattened. All shapes within this
        :ref:`service closure <service-closure>` will be replaced with equivalent
        shapes in the target namespace.
    * - includeTagged
      - ``[string]``
      - The set of tags that, if found on a shape not connected to the service,
        forces the shape to have its namespace flattened into the target
        namespace. When additional shapes are included, the shapes are replaced
        entirely, along with any references to the shapes which may exist within
        separate :ref:`service closures <service-closure>`.

The following example will flatten the namespaces of the shapes connected to
the ``ns.bar#MyService`` service into the target namespace, ``ns.foo``. All
shapes within :ref:`service closure <service-closure>` with be flattened into
the target namespace, including shapes that have been renamed to disambiguate
them through the service shape's ``rename`` property. Shapes tagged with
``baz`` or ``qux`` will also be flattened into the ``ns.foo`` namespace, so
long as they don't conflict with a shape within the
:ref:`service closure <service-closure>`.

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "flattenNamespaces",
                            "args": {
                                "namespace": "ns.foo",
                                "service": "ns.bar#MyService",
                                "includeTagged": ["baz", "qux"]
                            }
                        }
                    ]
                }
            }
        }


.. _removeTraitDefinitions-transform:

removeTraitDefinitions
----------------------

Removes trait definitions from the model, but leaves the instances of traits
intact on any shapes.

You can *export* trait definitions by applying specific tags to the trait
definition and adding the list of export tags in the ``exportTagged`` argument.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - exportTagged
      - ``[string]``
      - The set of tags that, if found on a trait definition, forces the trait
        to be retained in the transformed model.

The following example removes trait definitions but keeps the instances of the
trait intact on shapes in the model:

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "removeTraitDefinitions",
                            "args": {
                                "exportTagged": [
                                    "export-tag1",
                                    "another-export-tag"
                                ]
                            }
                        }
                    ]
                }
            }
        }

.. _removeUnusedShapes-transform:

removeUnusedShapes
------------------

Removes shapes from the model that are not connected to any service shape
or to a shape definition.

You can *export* shapes that are not connected to any service shape by
applying specific tags to the shape and adding the list of export tags in
the ``exportTagged`` argument.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - exportTagged
      - ``[string]``
      - The set of tags that, if found on a shape, forces the shape to be
        present in the transformed model regardless of if it was connected
        to a service.

The following example removes shapes that are not connected to any service,
but keeps the shape if it has any of the provided tags:

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "removeUnusedShapes",
                            "args": {
                                "exportTagged": [
                                    "export-tag1",
                                    "another-export-tag"
                                ]
                            }
                        }
                    ]
                }
            }
        }

.. _renameShapes-transform:

renameShapes
------------

Renames shapes within the model, including updating any references to the
shapes that are being renamed.

.. list-table::
    :header-rows: 1
    :widths: 10 20 70

    * - Property
      - Type
      - Description
    * - renamed
      - ``Map<shapeId, shapeId>``
      - The map of :ref:`shape IDs <shape-id>` to rename. Each key ``shapeId`` will be
        renamed to the value ``shapeId``. Each :ref:`shape ID <shape-id>` must be
        be an absolute shape ID.

The following example renames the ``ns.foo#Bar`` shape to ``ns.foo#Baz``.
Any references to ``ns.foo#Bar`` on other shapes will also be updated.

.. tabs::

    .. code-tab:: json

        {
            "version": "1.0",
            "projections": {
                "exampleProjection": {
                    "transforms": [
                        {
                            "name": "renameShapes",
                            "args": {
                                "renamed": {
                                    "ns.foo#Bar": "ns.foo#Baz"
                                }
                            }
                        }
                    ]
                }
            }
        }

.. _build_envars:

Environment variables
=====================

Strings in ``smithy-build.json`` files can contain environment variable place
holders that are expanded at load-time into the value of a Java system
property or environment variable. The syntax of a placeholder is
``${NAME}`` where "NAME" is the name of the system property or environment
variable. A placeholder can be escaped using a backslash (``\``) before the
"$". For example, ``\${FOO}`` expands to the literal string ``${FOO}``.
A non-existent system property or environment variable will cause the file
to fail to load. System property values take precedence over environment
variables.

Consider the following ``smithy-build.json`` file:

.. code-block:: json

    {
        "version": "1.0",
        "projections": {
            "a": {
                "transforms": [
                    {
                        "${NAME_KEY}": "includeShapesByTag",
                        "args": {
                            "tags": ["${FOO}", "\\${BAZ}"]
                        }
                    }
                ]
            }
        }
    }

Assuming that ``NAME_KEY`` is a system property set to "name", and ``FOO`` is an
environment variable set to "hi", this file is equivalent to:

.. code-block:: json

    {
        "version": "1.0",
        "projections": {
            "a": {
                "transforms": [
                    {
                        "name": "includeShapesByTag",
                        "args": {
                            "tags": ["Hi", "${BAZ}"]
                        }
                     }
                ]
            }
        }
    }


.. _plugins:

Plugins
=======

Plugins are defined in either the top-level "plugins" key-value pair of the
configuration file, or in the "plugins" key-value pair of a projection.
Plugins defined at the top-level of the configuration file are applied to
every projection. Projections that define plugins of the same name as a
top-level plugin completely overwrite the top-level plugin for that projection;
projection settings are not merged in any way.

Plugin names MUST match the following pattern: ``^[A-Za-z0-9\-_.]+$``.

smithy-build will attempt to resolve plugin names using `Java SPI`_
to locate an instance of ``software.amazon.smithy.build.SmithyBuildPlugin``
that matches the given plugin name (via ``matchesPluginName``). smithy-build
will log a warning when a plugin cannot be resolved.

smithy-build DOES NOT attempt to automatically download and install plugins.
Plugins MUST be available in the Java class path or module path in order for
them to be discovered.

The ``model``, ``build-info``, and ``sources`` plugins are plugins that are
always run in every non-abstract projection.


.. _model-plugin:

model plugin
------------

The ``model`` plugin serializes a self-contained and filtered version of the
model as a single file. All of the dependencies of the model are included
in the file.


.. _build-info-plugin:

build-info plugin
-----------------

The ``build-info`` plugin produces a JSON document that contains information
about the projection and model.


.. _sources-plugin:

sources plugin
--------------

The ``sources`` plugin copies the source models and creates a manifest.
When building the ``source`` projection, the models that were used to build the
model are copied over literally. When a JAR is used as a source model, the
Smithy models contained within the JAR are copied as a source model while the
JAR itself is not copied. If there are no source models, an empty manifest is
created.

When building projections other than ``source``, a new model file is created
that contains only the shapes, trait definitions, and metadata that were
defined in a source model *and* all of the newly added shapes, traits, and
metadata.

The manifest file is a newline (``\n``) separated file that contains the
relative path from the manifest file to each model file created by the
sources plugin. Lines that start with a number sign (#) are comments and are
ignored. A Smithy manifest file is stored in a JAR as ``META-INF/smithy/manifest``.
All model files referenced by the manifest are relative to ``META-INF/smithy/``.

.. _Java SPI: https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html
