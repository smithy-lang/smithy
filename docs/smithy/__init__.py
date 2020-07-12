import re

from sphinx.locale import _
from sphinx.writers.html import HTMLTranslator as SphinxHTMLTranslator

from docutils import nodes
from docutils.parsers.rst import Directive, directives
from docutils.statemachine import StringList
from docutils.writers.html4css1 import HTMLTranslator as BaseTranslator


def setup(app):
    app.set_translator('html', HTMLTranslator)
    app.add_directive("text-figure", TextFigure)

# Finds the href part of a header.
HREF = re.compile('href="([^"]+)"')

class HTMLTranslator(SphinxHTMLTranslator):

    # Removes the paragraph icon, and makes links clickable.
    def depart_title(self, node):
        SphinxHTMLTranslator.depart_title(self, node)
        # The second to last element added contains a "headerlink" class.
        # See https://bitbucket.org/birkenfeld/sphinx/src/72dceb35264e9429c8d9bb1a249a638ac21f0524/sphinx/writers/html.py#lines-501
        if len(self.body) > 2 and "headerlink" in self.body[-2]:
            match = HREF.search(self.body[-2])
            if match:
                link = match.group(1)
                # If header link was injected and the href could be
                # found, then change the href node to a closing </a>
                # anchor tag. Look at most at the 10 most recent nodes
                # to find where the anchor was opened.
                for i in range(-2, max(-10, -len(self.body)), -1):
                    if self.body[i].startswith("<h"):
                        self.body[i] += '<a href="' + link + '">'
                        self.body[-2] = "</a>"

    # Make production lists use ABNF and not BNF notation.
    def visit_productionlist(self, node):
        # type: (nodes.Node) -> None
        node.get('classes', []).append("productionlist")
        self.body.append(self.starttag(node, 'pre'))
        lastname = None
        for production in node:
            if production['tokenname']:
                if lastname is not None:
                    self.body.append('\n')
                lastname = production['tokenname']
                self.body.append(self.starttag(production, 'strong', ''))
                self.body.append(lastname + "</strong> =\n    ")
            elif lastname is not None:
                self.body.append('  ')
            production.walkabout(self)
            self.body.append('\n')
        self.body.append('</pre>\n')
        raise nodes.SkipNode


# Use .. text-figure to create text diagrams that look like figures.
class TextFigure(Directive):

    required_arguments = 0
    optional_arguments = 2
    option_spec = {
        'caption': directives.unchanged_required,
        'name': directives.unchanged,
    }
    has_content = True
    final_argument_whitespace = False

    def run(self):
        self.assert_has_content()
        text = '\n'.join(self.content)
        literal = nodes.literal_block(text, text)
        literal = self.__container_wrapper(literal, self.options.get('caption'))
        self.add_name(literal)
        literal['classes'].append("text-figure")
        return [literal]

    # This is a modified version of https://github.com/sphinx-doc/sphinx/blob/3.x/sphinx/directives/code.py
    # that places the caption after the text rather than before.
    def __container_wrapper(self, literal_node, caption):
        container_node = nodes.container('', literal_block=True,
                                         classes=['literal-block-wrapper'])
        parsed = nodes.Element()
        self.state.nested_parse(StringList([caption], source=''),
                                self.content_offset, parsed)
        if isinstance(parsed[0], nodes.system_message):
            msg = __('Invalid caption: %s' % parsed[0].astext())
            raise ValueError(msg)
        elif isinstance(parsed[0], nodes.Element):
            caption_node = nodes.caption(parsed[0].rawsource, '',
                                         *parsed[0].children)
            caption_node.source = literal_node.source
            caption_node.line = literal_node.line
            container_node += literal_node
            container_node += caption_node
            return container_node
        else:
            raise RuntimeError  # never reached
