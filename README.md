# CHAMELEON

Feeds the Gallifrey time database with entity events

### Building the project

1. mvn clean install
2. mvn package

### Generating a code coverage report

1. mvn clean install
2. mvn exec:java -Dexec.classpathScope=test -Dexec.mainClass='clojure.main' -Dexec.args="--main cloverage.coverage -n 'chameleon.*' -t 'chameleon.testing'"

    2.1. Follow the instructions to view the report.

3. To tweak the report generation, follow the instrustions here:
   https://github.com/cloverage/cloverage

### Developing

1. I use Leiningen for building my clojure project and for dependency
   management. You can read about Leiningen and follow the tutorial at
   the link:
   https://github.com/technomancy/leiningen

2. If you make ** changes to the project.clj file, you are required to
   re-generate the pom.xml by running `lein pom`** (requires leiningen
   to be instaled).

   You can also add your changes to the pom.xml file manually.

3. I use Emacs as my editor and CIDER for interactive development in
   Clojure.
   - CIDER: https://github.com/clojure-emacs/cider
   - Emacs: https://www.gnu.org/software/emacs/

   - Here is a tutorial on Emacs: https://www.gnu.org/software/emacs/tour/
   - Here is the emacs configuration I've used, it includes the CIDER
   package:
   https://github.com/sandhu/emacs.d
