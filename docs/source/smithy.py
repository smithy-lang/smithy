from sphinx.writers.html import HTMLTranslator as SphinxHTMLTranslator
from docutils.writers.html4css1 import HTMLTranslator as BaseTranslator
from docutils import nodes

from sphinx.locale import _


def setup(app):
    app.set_translator('html', HTMLTranslator)

class HTMLTranslator(SphinxHTMLTranslator):

    # Make production lists use ABNF and not BNF notation.
    def visit_productionlist(self, node):
        # type: (nodes.Node) -> None
        self.body.append(self.starttag(node, 'pre'))
        names = []
        for production in node:
            names.append(production['tokenname'])
        maxlen = max(len(name) for name in names)
        lastname = None
        for production in node:
            if production['tokenname']:
                if lastname is not None:
                    self.body.append('\n')
                lastname = production['tokenname'].ljust(maxlen)
                self.body.append(self.starttag(production, 'strong', ''))
                self.body.append(lastname + '</strong> = ')
            elif lastname is not None:
                self.body.append('%s   ' % (' ' * len(lastname)))
            production.walkabout(self)
            self.body.append('\n')
        self.body.append('</pre>\n')
        raise nodes.SkipNode
