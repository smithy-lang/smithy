package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionNode;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.RecordLiteral;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.StringLiteral;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.TupleLiteral;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;

public abstract class TreeMapper {

    public Rule rule(Rule r) {
        if (r instanceof TreeRule) {
            return treeRule((TreeRule) r);
        } else if (r instanceof EndpointRule) {
            return endpointRule((EndpointRule) r);
        } else if (r instanceof ErrorRule) {
            return errorRule((ErrorRule) r);
        }
        return r;
    }

    public Rule treeRule(TreeRule tr) {
        return Rule.builder()
                .description(tr.getDocumentation().orElse(null))
                .conditions(conditions(tr, tr.getConditions()))
                .treeRule(rules(tr, tr.getRules()));
    }

    public List<Rule> rules(TreeRule tr, List<Rule> rules) {
        List<Rule> updated = new ArrayList<>(tr.getRules().size());
        for (Rule rule : rules) {
            Rule mapped = rule(rule);
            if (mapped != null) {
                updated.add(mapped);
            }
        }
        return updated;
    }

    public List<Condition> conditions(Rule rule, List<Condition> conditions) {
        List<Condition> updated = new ArrayList<>(conditions.size());
        for (Condition condition : conditions) {
            Condition mapped = condition(rule, condition);
            if (mapped != null) {
                updated.add(mapped);
            }
        }
        return updated;
    }

    public Condition condition(Rule rule, Condition condition) {
        return Condition.builder()
                .fn(libraryFunction(condition.getFunction()))
                .result(result(rule, condition, condition.getResult().orElse(null)))
                .build();
    }

    public Identifier result(Rule rule, Condition condition, Identifier result) {
        return result;
    }

    public Rule endpointRule(EndpointRule er) {
        return Rule.builder()
                .description(er.getDocumentation().orElse(null))
                .conditions(conditions(er, er.getConditions()))
                .endpoint(endpoint(er.getEndpoint()));
    }

    public Rule errorRule(ErrorRule er) {
        return Rule.builder()
                .description(er.getDocumentation().orElse(null))
                .conditions(conditions(er, er.getConditions()))
                .error(error(er, er.getError()));
    }

    public Expression error(ErrorRule er, Expression e) {
        return expression(e);
    }

    public LibraryFunction libraryFunction(LibraryFunction fn) {
        boolean changed = false;
        List<Expression> rewrittenArgs = new ArrayList<>(fn.getArguments().size());
        for (int i = 0; i < fn.getArguments().size(); i++) {
            Expression argument = fn.getArguments().get(i);
            Expression rewritten = argument(fn, i, argument);
            rewrittenArgs.add(rewritten);
            if (rewritten != argument) {
                changed = true;
            }
        }

        if (!changed) {
            return fn;
        }

        return fn.getFunctionDefinition().createFunction(FunctionNode.builder()
                .name(Node.from(fn.getName()))
                .arguments(rewrittenArgs)
                .build());
    }

    public Expression argument(LibraryFunction fn, int position, Expression argument) {
        return expression(argument);
    }

    public Expression expression(Expression expression) {
        if (expression instanceof Literal) {
            return literal((Literal) expression);
        } else if (expression instanceof Reference) {
            return reference((Reference) expression);
        } else if (expression instanceof LibraryFunction) {
            return libraryFunction((LibraryFunction) expression);
        } else {
            return expression;
        }
    }

    public Reference reference(Reference reference) {
        return reference;
    }

    public Literal literal(Literal literal) {
        if (literal instanceof StringLiteral) {
            return stringLiteral((StringLiteral) literal);
        } else if (literal instanceof TupleLiteral) {
            return tupleLiteral((TupleLiteral) literal);
        } else if (literal instanceof RecordLiteral) {
            return recordLiteral((RecordLiteral) literal);
        } else {
            return literal;
        }
    }

    public Literal tupleLiteral(TupleLiteral tuple) {
        List<Literal> rewrittenMembers = new ArrayList<>();
        boolean changed = false;

        for (Literal member : tuple.members()) {
            Literal rewritten = (Literal) expression(member);
            rewrittenMembers.add(rewritten);
            if (rewritten != member) {
                changed = true;
            }
        }

        return changed ? Literal.tupleLiteral(rewrittenMembers) : tuple;
    }

    public Literal recordLiteral(RecordLiteral record) {
        Map<Identifier, Literal> rewrittenMembers = new LinkedHashMap<>();
        boolean changed = false;

        for (Map.Entry<Identifier, Literal> entry : record.members().entrySet()) {
            Literal original = entry.getValue();
            Literal rewritten = (Literal) expression(original);
            rewrittenMembers.put(entry.getKey(), rewritten);
            if (rewritten != original) {
                changed = true;
            }
        }

        return changed ? Literal.recordLiteral(rewrittenMembers) : record;
    }

    public Literal stringLiteral(StringLiteral str) {
        Template template = str.value();
        if (template.isStatic()) {
            return str;
        }

        StringBuilder templateBuilder = new StringBuilder();
        boolean changed = false;

        for (Template.Part part : template.getParts()) {
            if (part instanceof Template.Dynamic) {
                Template.Dynamic dynamic = (Template.Dynamic) part;
                Expression original = dynamic.toExpression();
                Expression rewritten = expression(original);
                if (rewritten != original) {
                    changed = true;
                }
                templateBuilder.append('{').append(rewritten).append('}');
            } else {
                templateBuilder.append(((Template.Literal) part).getValue());
            }
        }

        return changed ? Literal.stringLiteral(Template.fromString(templateBuilder.toString())) : str;
    }

    public Endpoint endpoint(Endpoint endpoint) {
        Expression rewrittenUrl = expression(endpoint.getUrl());
        Map<String, List<Expression>> rewrittenHeaders = rewriteHeaders(endpoint.getHeaders());
        Map<Identifier, Literal> rewrittenProperties = rewriteProperties(endpoint.getProperties());

        // Only create new endpoint if something changed
        if (rewrittenUrl != endpoint.getUrl()
                || rewrittenHeaders != endpoint.getHeaders()
                || rewrittenProperties != endpoint.getProperties()) {
            return Endpoint.builder()
                    .url(rewrittenUrl)
                    .headers(rewrittenHeaders)
                    .properties(rewrittenProperties)
                    .build();
        }

        return endpoint;
    }

    private Map<String, List<Expression>> rewriteHeaders(Map<String, List<Expression>> headers) {
        if (headers.isEmpty()) {
            return headers;
        }

        Map<String, List<Expression>> rewritten = null;
        boolean changed = false;

        for (Map.Entry<String, List<Expression>> entry : headers.entrySet()) {
            List<Expression> originalValues = entry.getValue();
            List<Expression> rewrittenValues = null;

            for (int i = 0; i < originalValues.size(); i++) {
                Expression original = originalValues.get(i);
                Expression rewrittenExpr = expression(original);

                if (rewrittenExpr != original) {
                    if (rewrittenValues == null) {
                        rewrittenValues = new ArrayList<>(originalValues.subList(0, i));
                    }
                    rewrittenValues.add(rewrittenExpr);
                    changed = true;
                } else if (rewrittenValues != null) {
                    rewrittenValues.add(original);
                }
            }

            if (changed && rewritten == null) {
                rewritten = new LinkedHashMap<>();
                // Copy all previous entries
                for (Map.Entry<String, List<Expression>> prev : headers.entrySet()) {
                    if (prev.getKey().equals(entry.getKey())) {
                        break;
                    }
                    rewritten.put(prev.getKey(), prev.getValue());
                }
            }

            if (rewritten != null) {
                rewritten.put(entry.getKey(),
                              rewrittenValues != null ? rewrittenValues : originalValues);
            }
        }

        return changed ? rewritten : headers;
    }

    private Map<Identifier, Literal> rewriteProperties(Map<Identifier, Literal> properties) {
        if (properties.isEmpty()) {
            return properties;
        }

        Map<Identifier, Literal> rewritten = null;
        boolean changed = false;

        for (Map.Entry<Identifier, Literal> entry : properties.entrySet()) {
            Expression rewrittenExpr = expression(entry.getValue());

            if (rewrittenExpr != entry.getValue()) {
                if (!(rewrittenExpr instanceof Literal)) {
                    throw new IllegalStateException("Property value must be a literal");
                }

                if (rewritten == null) {
                    rewritten = new LinkedHashMap<>();
                    // Copy all previous entries
                    for (Map.Entry<Identifier, Literal> prev : properties.entrySet()) {
                        if (prev.getKey().equals(entry.getKey())) {
                            break;
                        }
                        rewritten.put(prev.getKey(), prev.getValue());
                    }
                }

                rewritten.put(entry.getKey(), (Literal) rewrittenExpr);
                changed = true;
            } else if (rewritten != null) {
                rewritten.put(entry.getKey(), entry.getValue());
            }
        }

        return changed ? rewritten : properties;
    }
}
