# You can set these variables from the command line.
SPHINXOPTS    =
SPHINXBUILD   = python3 -msphinx
SPHINXPROJ    = Smithy
SHELL         := /bin/bash

install:
	python3 -m venv build/venv
	source build/venv/bin/activate && pip3 install -e .

clean:
	-rm -rf build/* install

html1:
	@source build/venv/bin/activate && $(SPHINXBUILD) -M html "source-1.0" "build/1.0" $(SPHINXOPTS) -W --keep-going -n $(O)

html2:
	@source build/venv/bin/activate && $(SPHINXBUILD) -M html "source-2.0" "build/2.0" $(SPHINXOPTS) -W --keep-going -n $(O)

llms-txt: html2
	python3 generate_llms_txt.py

html: html2 html1 merge-versions llms-txt

serve:
	npx http-server build/html

merge-versions:
	mkdir -p build/html
	rm -rf "build/html/1.0" "build/html/2.0"
	cp -R "build/1.0/html" "build/html/1.0"
	cp -R "build/2.0/html" "build/html/2.0"
	cp -R root/* "build/html"
	# Landing page: a single self-contained static page (no build step).
	# Ship only the page + its assets (not README.md or the _og-card source).
	cp landing/index.html "build/html/"
	cp -R landing/assets "build/html/"

openhtml:
	open "build/html/index.html"

dev: install
	@source build/venv/bin/activate && sphinx-autobuild "source-2.0" "build/2.0" $(SPHINXOPTS) $(O)

.PHONY: Makefile clean
