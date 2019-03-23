/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import software.amazon.smithy.model.loader.ModelDiscovery;
import software.amazon.smithy.model.traits.AuthenticationSchemesTrait;
import software.amazon.smithy.model.traits.AuthenticationTrait;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.CollectionOperationTrait;
import software.amazon.smithy.model.traits.CorsTrait;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.EndpointTrait;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.ErrorTrait;
import software.amazon.smithy.model.traits.EventHeaderTrait;
import software.amazon.smithy.model.traits.EventPayloadTrait;
import software.amazon.smithy.model.traits.ExamplesTrait;
import software.amazon.smithy.model.traits.ExternalDocumentationTrait;
import software.amazon.smithy.model.traits.HostLabelTrait;
import software.amazon.smithy.model.traits.HttpErrorTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.HttpTrait;
import software.amazon.smithy.model.traits.IdRefTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.IdempotentTrait;
import software.amazon.smithy.model.traits.InputEventStreamTrait;
import software.amazon.smithy.model.traits.InstanceOperationTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.MediaTypeTrait;
import software.amazon.smithy.model.traits.OutputEventStreamTrait;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.traits.ProtocolsTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.model.traits.ReferencesTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.ResourceIdentifierTrait;
import software.amazon.smithy.model.traits.RetryableTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.SinceTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.SyntheticTrait;
import software.amazon.smithy.model.traits.TagsTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.TitleTrait;
import software.amazon.smithy.model.traits.TraitService;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.model.traits.XmlAttributeTrait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.model.traits.XmlNamespaceTrait;
import software.amazon.smithy.model.transform.ModelTransformerPlugin;
import software.amazon.smithy.model.transform.plugins.CleanBindings;
import software.amazon.smithy.model.transform.plugins.CleanOperationStructures;
import software.amazon.smithy.model.transform.plugins.CleanResourceReferences;
import software.amazon.smithy.model.transform.plugins.CleanStructureAndUnionMembers;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.model.validation.ValidatorService;
import software.amazon.smithy.model.validation.builtins.AuthenticationProtocolsValidator;
import software.amazon.smithy.model.validation.builtins.AuthenticationValidator;
import software.amazon.smithy.model.validation.builtins.EnumTraitValidator;
import software.amazon.smithy.model.validation.builtins.EventPayloadTraitValidator;
import software.amazon.smithy.model.validation.builtins.EventStreamValidator;
import software.amazon.smithy.model.validation.builtins.ExamplesTraitValidator;
import software.amazon.smithy.model.validation.builtins.ExclusiveStructureMemberTraitValidator;
import software.amazon.smithy.model.validation.builtins.HostLabelTraitValidator;
import software.amazon.smithy.model.validation.builtins.HttpBindingsMissingValidator;
import software.amazon.smithy.model.validation.builtins.HttpHeaderTraitValidator;
import software.amazon.smithy.model.validation.builtins.HttpLabelTraitValidator;
import software.amazon.smithy.model.validation.builtins.HttpPayloadValidator;
import software.amazon.smithy.model.validation.builtins.HttpPrefixHeadersTraitValidator;
import software.amazon.smithy.model.validation.builtins.HttpQueryTraitValidator;
import software.amazon.smithy.model.validation.builtins.HttpUriConflictValidator;
import software.amazon.smithy.model.validation.builtins.PaginatedTraitValidator;
import software.amazon.smithy.model.validation.builtins.PrivateAccessValidator;
import software.amazon.smithy.model.validation.builtins.RangeTraitValidator;
import software.amazon.smithy.model.validation.builtins.ReferencesTraitValidator;
import software.amazon.smithy.model.validation.builtins.ResourceCycleValidator;
import software.amazon.smithy.model.validation.builtins.ResourceIdentifierBindingValidator;
import software.amazon.smithy.model.validation.builtins.ResourceIdentifierValidator;
import software.amazon.smithy.model.validation.builtins.ResourceLifecycleValidator;
import software.amazon.smithy.model.validation.builtins.ResourceOperationBindingValidator;
import software.amazon.smithy.model.validation.builtins.ServiceValidator;
import software.amazon.smithy.model.validation.builtins.SetTargetValidator;
import software.amazon.smithy.model.validation.builtins.ShapeIdConflictValidator;
import software.amazon.smithy.model.validation.builtins.SingleOperationBindingValidator;
import software.amazon.smithy.model.validation.builtins.SingleResourceBindingValidator;
import software.amazon.smithy.model.validation.builtins.TargetValidator;
import software.amazon.smithy.model.validation.builtins.TraitConflictValidator;
import software.amazon.smithy.model.validation.builtins.TraitDefinitionShapeValidator;
import software.amazon.smithy.model.validation.builtins.TraitTargetValidator;
import software.amazon.smithy.model.validation.builtins.TraitValueValidator;
import software.amazon.smithy.model.validation.builtins.XmlNamespaceTraitValidator;

module software.amazon.smithy.model {
    requires java.logging;
    requires com.fasterxml.jackson.core;

    exports software.amazon.smithy.model;
    exports software.amazon.smithy.model.knowledge;
    exports software.amazon.smithy.model.loader;
    exports software.amazon.smithy.model.neighbor;
    exports software.amazon.smithy.model.node;
    exports software.amazon.smithy.model.selector;
    exports software.amazon.smithy.model.shapes;
    exports software.amazon.smithy.model.traits;
    exports software.amazon.smithy.model.validation;
    exports software.amazon.smithy.model.validation.testrunner;
    exports software.amazon.smithy.model.transform;
    exports software.amazon.smithy.model.transform.plugins;

    uses ModelTransformerPlugin;
    uses TraitService;
    uses Validator;
    uses ValidatorService;
    uses ModelDiscovery;

    // Concrete types for Traits.
    provides TraitService with
            AuthenticationSchemesTrait,
            AuthenticationTrait.Provider,
            BoxTrait,
            CollectionOperationTrait,
            CorsTrait.Provider,
            DeprecatedTrait,
            DocumentationTrait,
            EndpointTrait,
            EnumTrait.Provider,
            ErrorTrait,
            EventHeaderTrait,
            EventPayloadTrait,
            ExamplesTrait.Provider,
            ExternalDocumentationTrait,
            HostLabelTrait,
            HttpErrorTrait,
            HttpHeaderTrait,
            HttpLabelTrait,
            HttpPayloadTrait,
            HttpPrefixHeadersTrait,
            HttpQueryTrait,
            HttpTrait,
            IdempotencyTokenTrait,
            IdempotentTrait,
            IdRefTrait.Provider,
            InputEventStreamTrait,
            InstanceOperationTrait,
            JsonNameTrait,
            LengthTrait.Provider,
            MediaTypeTrait,
            OutputEventStreamTrait,
            PaginatedTrait.Provider,
            PatternTrait,
            PrivateTrait,
            ProtocolsTrait,
            RangeTrait.Provider,
            ReadonlyTrait,
            ReferencesTrait.Provider,
            RequiredTrait,
            ResourceIdentifierTrait,
            RetryableTrait,
            SensitiveTrait,
            SinceTrait,
            StreamingTrait,
            SyntheticTrait,
            TagsTrait,
            TimestampFormatTrait,
            TitleTrait,
            UniqueItemsTrait,
            XmlAttributeTrait,
            XmlFlattenedTrait,
            XmlNamespaceTrait.Provider,
            XmlNameTrait;

    // Built-in validators applied to every model.
    provides Validator with
            AuthenticationValidator,
            AuthenticationProtocolsValidator,
            EnumTraitValidator,
            EventPayloadTraitValidator,
            EventStreamValidator,
            ExamplesTraitValidator,
            ExclusiveStructureMemberTraitValidator,
            HostLabelTraitValidator,
            HttpBindingsMissingValidator,
            HttpHeaderTraitValidator,
            HttpLabelTraitValidator,
            HttpPayloadValidator,
            HttpPrefixHeadersTraitValidator,
            HttpQueryTraitValidator,
            HttpUriConflictValidator,
            PaginatedTraitValidator,
            PrivateAccessValidator,
            RangeTraitValidator,
            ReferencesTraitValidator,
            ResourceCycleValidator,
            ResourceIdentifierBindingValidator,
            ResourceIdentifierValidator,
            ResourceLifecycleValidator,
            ResourceOperationBindingValidator,
            ServiceValidator,
            SetTargetValidator,
            ShapeIdConflictValidator,
            SingleOperationBindingValidator,
            SingleResourceBindingValidator,
            TargetValidator,
            TraitConflictValidator,
            TraitDefinitionShapeValidator,
            TraitTargetValidator,
            TraitValueValidator,
            XmlNamespaceTraitValidator;

    provides ModelTransformerPlugin with
            CleanBindings,
            CleanOperationStructures,
            CleanResourceReferences,
            CleanStructureAndUnionMembers;
}
