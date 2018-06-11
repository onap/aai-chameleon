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

4. Start nrepl `C-c M-j` (you have to be in any *.clj file in the
   chameleon project). If the cider-repl buffer doesn't open
   automatically, use `C-x b` and use the arrow keys to find the
   `cider-repl chameleon` buffer and press enter.

5. Open the dev.clj file by `C-x C-f`, load it `C-c C-k` and switch
   your namespace to dev `C-c M-n`.

6. In the repl buffer you should see that the namespace went from `user`
   to `dev`.

7. Make sure the configuration is correct in the dev.clj file and once
   verified, in the repl buffer type `(go)` and press enter.

8. You can verify everthing is running by generating/adding an event
   to dmaap and seeing it flow through chameleon in the error.log file.

### Running it locally (Assumming you're not using Emacs and using
   `lein repl` from the command line)

**Make you're in the directory at the root of your project. Open the
    file `chameleon/dev.clj` and update the config to the correct
    dmaap host, topic, and make sure you're using the correct
    consumer group and id.**

1. On the command line, execute `lein repl`

2. Once in the clojure repl, load the dev namespace
   (load "dev")

3. Go into the dev namespace
   (in-ns 'dev)

4. Run the following command in the repl
   (go)
