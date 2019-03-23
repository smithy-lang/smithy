package software.amazon.smithy.aws.traits.apigateway;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.traits.AuthenticationTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Each authorizers must match one of the authentication schemes on the service.
 */
public class AuthorizersTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.getShapeIndex().shapes(ServiceShape.class)
                .flatMap(this::validateService)
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateService(ServiceShape service) {
        var schemeNames = service.getTrait(AuthenticationTrait.class)
                .map(AuthenticationTrait::getAuthenticationSchemes)
                .map(Map::keySet)
                .orElse(Set.of());
        var authorizerNames = service.getTrait(AuthorizersTrait.class)
                .map(AuthorizersTrait::getAllAuthorizers)
                .map(map -> new HashSet<>(map.keySet()))
                .orElseGet(HashSet::new);

        authorizerNames.removeAll(schemeNames);
        return authorizerNames.stream().map(name -> error(service, String.format(
                "Invalid `%s` entry `%s` does not match one of the `authentication` schemes defined "
                + "on the service: [%s]",
                AuthorizersTrait.NAME, name, ValidationUtils.tickedList(schemeNames))));
    }
}
