The `document.html.erb` template in this directory is used to render HTML documentation 
using AsciiDoctor. This template is a copy of 
https://github.com/asciidoctor/asciidoctor-backends/blob/master/erb/html5/document.html.erb 
with various modifications, like:

* Add Jekyll front matter for GitHub Pages deployment
* Add Fortify title banner
* Add navigation/version banner
* Various styling updates
* Dark mode
* Copy button for code blocks

IMPORTANT NOTE: All CSS and JavaScript defined in the template apply to static HTML pages
                only. For GitHub Pages deployment, JavaScript and stylesheets are defined
                in the gh-pages branch. When updating either one, the other likely needs
                to be updated as well.
