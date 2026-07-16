from docutils import nodes
from docutils.parsers.rst import Directive, directives

from sphinx.locale import _
from sphinx.util import logging

logger = logging.getLogger(__name__)

SMITHY_TRAITS_ATTR = "smithy_traits"

# Traits in this namespace are part of the prelude and are always available, so
# there's no package for a user to depend on. They're grouped under this label
# in the index instead of a package name, and note that they come from the
# prelude rather than an artifact.
PRELUDE_NAMESPACE = "smithy.api"
PRELUDE_GROUP = "Prelude"

# The reStructuredText label of the section that documents the prelude, linked
# to from the "Provided by" note on prelude traits.
PRELUDE_LABEL = "prelude"

# The Maven group a bare :package: name belongs to (packages outside it are
# authored as a full "group:artifact" coordinate), and the artifact page a
# package's "Provided by" note links to.
DEFAULT_PACKAGE_GROUP = "software.amazon.smithy"
MAVEN_ARTIFACT_URL = "https://central.sonatype.com/artifact/{group}/{artifact}"


# Placeholder node that will be replaced with the trait index.
class SmithyTraitIndexNode(nodes.General, nodes.Element):
    pass


# Directive that places a SmithyTraitIndexNode in the document.
# Usage: .. smithy-trait-index::
class SmithyTraitIndexDirective(Directive):
    def run(self):
        # The index aggregates traits from every other page, so it has to be
        # re-rendered whenever any of them changes, not just when its own source
        # does. Marking it for a re-read on every build keeps it from going
        # stale during incremental builds.
        self.state.document.settings.env.note_reread()
        return [SmithyTraitIndexNode("")]


# Directive that marks the location of a smithy trait in the docs.
# Usage:
#     .. smithy-trait:: ns.example#shapeId
#        :package: some-package
#
# The package is the artifact that provides the trait (a bare artifact name for
# artifacts in the software.amazon.smithy group, or a group:artifact coordinate
# otherwise). It's used to group the trait index by package and to note the
# providing package on the trait itself. Prelude traits may omit it, but it's
# otherwise required whenever any trait declares one. This is validated by
# validate_trait_packages.
class SmithyTraitDirective(Directive):

    # Makes the shape id argument required
    required_arguments = 1

    option_spec = {"package": directives.unchanged}

    def run(self):
        env = self.state.document.settings.env
        shape_id = self.arguments[0]
        package = self.options.get("package")

        # Make the shape id url-friendly
        stripped_shape_id = shape_id.strip().replace("#", "-").replace(".", "-").lower()

        # Create a new target node that can be referenced
        target_id = stripped_shape_id + "-trait"
        target_node = nodes.target("", "", ids=[target_id])

        # Add a list of the smithy trait targets to the envirnonment so that
        # they can be referenced later when building the trait index.
        if not hasattr(env, SMITHY_TRAITS_ATTR):
            env.smithy_traits = []

        env.smithy_traits.append(
            {
                "docname": env.docname,
                "lineno": self.lineno,
                "target": target_node,
                "target_id": target_id,
                "shape_id": shape_id,
                "package": package,
            }
        )

        return [target_node]


# If the source file changes, remove any cached smithy traits for it. If there
# are any remaining in the file itself, they'll be re-added in the normal way.
def purge_smithy_traits(app, env, docname):
    if not hasattr(env, SMITHY_TRAITS_ATTR):
        return
    env.smithy_traits = [t for t in env.smithy_traits if t["docname"] != docname]


# In the case of a parallel build, there will be multiple envs. This handles
# merging all the smithy traits into a single list at the end.
def merge_smithy_traits(app, env, docnames, other):
    if not hasattr(env, SMITHY_TRAITS_ATTR):
        env.smithy_traits = []
    if hasattr(other, SMITHY_TRAITS_ATTR):
        env.smithy_traits.extend(other.smithy_traits)


def is_prelude(smithy_trait_info):
    return smithy_trait_info["shape_id"].split("#", 1)[0] == PRELUDE_NAMESPACE


def is_aws(smithy_trait_info):
    return smithy_trait_info["shape_id"].startswith("aws.")


# The label that a trait is grouped under in the index. This is the prelude
# group for prelude traits, otherwise its providing package. If there's no
# package, it's reported as "unknown". validate_trait_packages will ensure
# that this results in a build time error.
def trait_group(smithy_trait_info):
    if is_prelude(smithy_trait_info):
        return PRELUDE_GROUP
    return smithy_trait_info.get("package", "unknown")


# Orders the index groups: the prelude first (it holds the core traits), then
# non-AWS packages alphabetically, then AWS packages last (they only matter to
# models that integrate with AWS). A group counts as AWS when its traits are in
# an aws.* namespace.
def sorted_groups(groups):
    def key(name):
        is_prelude_group = name == PRELUDE_GROUP
        is_aws_group = any(is_aws(t) for t in groups[name])
        return (not is_prelude_group, is_aws_group, name.lower())

    return sorted(groups, key=key)


# Builds a link to the section labeled by a std-domain reference label, or None
# if the label can't be resolved. Resolves the label directly since this runs
# after cross-references have already been processed.
def label_reference(app, fromdocname, label, text):
    std = app.builder.env.get_domain("std")
    if label not in std.labels:
        return None
    docname, target_id, _title = std.labels[label]

    ref_node = nodes.reference("", "")
    ref_node["refuri"] = app.builder.get_relative_uri(fromdocname, docname)
    if target_id:
        ref_node["refuri"] += "#" + target_id
    ref_node += nodes.Text(text)
    return ref_node


# The Maven Central page for a :package:, whose value is either a bare artifact
# name in the default group or a full "group:artifact" coordinate.
def maven_artifact_url(package):
    group, _, artifact = package.rpartition(":")
    return MAVEN_ARTIFACT_URL.format(
        group=group or DEFAULT_PACKAGE_GROUP, artifact=artifact
    )


# Notes where a trait comes from by appending a "Provided by" entry to the
# definition list that describes it (the same list that holds its Summary,
# selector, and value type). Only runs once package attribution is in use (the
# 2.0 docs). The 1.0 docs declare no packages, so they remain unchanged.
def annotate_trait_packages(app, doctree, fromdocname):
    env = app.builder.env
    smithy_traits = getattr(env, SMITHY_TRAITS_ATTR, [])

    # Bail early if there are no package attributions.
    if not any(t.get("package") for t in smithy_traits):
        return

    trait_by_target_id = {t["target_id"]: t for t in smithy_traits}

    for section in doctree.traverse(nodes.section):
        # The trait's target sits just before its heading, so docutils folds the
        # target id onto the section that the heading opens.
        smithy_trait_info = next(
            (trait_by_target_id[i] for i in section["ids"] if i in trait_by_target_id),
            None,
        )
        if smithy_trait_info is None:
            continue

        # The Summary/selector/value-type block is authored as a definition list.
        definition_list = next(
            (c for c in section.children if isinstance(c, nodes.definition_list)),
            None,
        )

        if definition_list is None:
            logger.warning(
                "smithy-trait %s is followed by a trait definition that omits "
                "a definition list, so it may be malformed. The :package: "
                "attribution could therefore not be added.",
                smithy_trait_info["shape_id"],
                location=(smithy_trait_info["docname"], smithy_trait_info["lineno"]),
            )
            continue

        paragraph = provided_by_paragraph(app, fromdocname, smithy_trait_info)
        if paragraph is None:
            continue

        definition = nodes.definition()
        definition += paragraph
        item = nodes.definition_list_item()
        item += nodes.term("", "Provided by")
        item += definition
        definition_list += item


# The "Provided by" note shown on a trait, as a paragraph, or None if there's
# no known pacakge. Prelude traits get a fixed note that links to the prelude
# while every other trait names its providing artifact.
def provided_by_paragraph(app, fromdocname, smithy_trait_info):
    paragraph = nodes.paragraph()
    if is_prelude(smithy_trait_info):
        prelude = label_reference(app, fromdocname, PRELUDE_LABEL, "prelude")
        paragraph += nodes.Text("Provided by the ")
        paragraph += prelude if prelude is not None else nodes.Text("prelude")
        paragraph += nodes.Text(", available to all models.")
    elif smithy_trait_info.get("package"):
        package = smithy_trait_info["package"]
        link = nodes.reference("", "", refuri=maven_artifact_url(package))
        link += nodes.literal("", package)
        paragraph += nodes.Text("Provided by the ")
        paragraph += link
        paragraph += nodes.Text(" package.")
    else:
        return None
    return paragraph


# Builds a link to a documented trait, shown as its emphasized shape id.
def trait_reference(app, fromdocname, smithy_trait_info):
    shape_id = smithy_trait_info["shape_id"]

    ref_node = nodes.reference("", "")
    ref_node["refdocname"] = smithy_trait_info["docname"]
    ref_node["refuri"] = app.builder.get_relative_uri(
        fromdocname, smithy_trait_info["docname"]
    )
    ref_node["refuri"] += "#" + smithy_trait_info["target"]["refid"]
    ref_node += nodes.emphasis(_(shape_id), _(shape_id))

    para = nodes.paragraph()
    para += ref_node
    list_item = nodes.list_item()
    list_item += para
    return list_item


# A flat, alphabetical list of links to every trait.
def flat_index(app, fromdocname, smithy_traits):
    bullet_list = nodes.bullet_list()
    for smithy_trait_info in smithy_traits:
        bullet_list += trait_reference(app, fromdocname, smithy_trait_info)
    return [bullet_list]


# Links to every trait, split into a section per providing package (plus the
# prelude), each holding an alphabetical list of the traits it provides.
def grouped_index(app, fromdocname, smithy_traits):
    groups = {}
    for smithy_trait_info in smithy_traits:
        groups.setdefault(trait_group(smithy_trait_info), []).append(smithy_trait_info)

    index_nodes = []
    for group in sorted_groups(groups):
        rubric = nodes.rubric()
        rubric += nodes.Text(group)
        index_nodes.append(rubric)
        index_nodes += flat_index(app, fromdocname, groups[group])
    return index_nodes


# Generates the trait index, replacing any SmithyTraitIndexNodes. The index is
# grouped by providing package when any trait declares one, and is otherwise a
# single flat list (e.g. the 1.0 docs, which predate package attribution).
def process_smithy_nodes(app, doctree, fromdocname):
    env = app.builder.env

    if not hasattr(env, SMITHY_TRAITS_ATTR):
        env.smithy_traits = []

    smithy_traits = sorted(env.smithy_traits, key=lambda x: x["shape_id"])
    has_packages = any(t.get("package") for t in smithy_traits)

    for node in doctree.traverse(SmithyTraitIndexNode):
        if has_packages:
            index_nodes = grouped_index(app, fromdocname, smithy_traits)
        else:
            index_nodes = flat_index(app, fromdocname, smithy_traits)
        node.replace_self(index_nodes)


# Once any trait declares a :package:, every non-prelude trait must, so no
# providing package is silently dropped.
def validate_trait_packages(app, env):
    smithy_traits = getattr(env, SMITHY_TRAITS_ATTR, [])
    if not any(t.get("package") for t in smithy_traits):
        return

    for smithy_trait_info in smithy_traits:
        if smithy_trait_info.get("package") or is_prelude(smithy_trait_info):
            continue
        logger.warning(
            "smithy-trait %s is missing a :package: option, which is required for "
            "all non-prelude traits once any trait declares one.",
            smithy_trait_info["shape_id"],
            location=(smithy_trait_info["docname"], smithy_trait_info["lineno"]),
        )


def setup_smithy_trait_index(app):
    app.add_node(SmithyTraitIndexNode)
    app.add_directive("smithy-trait", SmithyTraitDirective)
    app.add_directive("smithy-trait-index", SmithyTraitIndexDirective)
    app.connect("doctree-resolved", annotate_trait_packages)
    app.connect("doctree-resolved", process_smithy_nodes)
    app.connect("env-purge-doc", purge_smithy_traits)
    app.connect("env-merge-info", merge_smithy_traits)
    app.connect("env-check-consistency", validate_trait_packages)
