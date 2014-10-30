# lazybot

[lazybot](http://github.com/Raynes/lazybot) is an IRC bot written in Clojure. Under the hood, it uses [irclj](http://github.com/Raynes/irclj) for talking to IRC. The bot is highly extensible via plugins. Plugins are written in Clojure, using a lightweight DSL. There are already plugins for tons of stuff, including Haskell and Clojure code evaluation, googling, and using the bot as an operator bot. You can view all of the plugins in src/lazybot/plugins.

The project aims to create what will eventually be a user-friendly and powerful IRC bot. Whereas clojurebot is mostly aimed at providing helpful services in the #clojure IRC channel, lazybot is focused on being generally useful to people who aren't Clojurians. However, lazybot also provides Clojure code evaluation, and is used by some Clojurians in their respective IRC channels primarily for that purpose.

## Usage 

Several of this bot's plugins require that you have MongoDB installed and running. The core plugins that currently do are mail, whatis, macro, seen, help, fortune and karma. You can find some quickstart guides for setting up MongoDB here: http://www.mongodb.org/display/DOCS/Quickstart. It's usually pretty painless and fast.

As for what OSes this bot actually runs on: I have no clue. I've seen it run on several Ubuntu distros and OS X, but nobody (that I know of) has yet to venture into the world of lazybot-on-windows. If you do, please let me know how it goes.

The easiest way to get the bot going is to pull the repository, install [leiningen](http://github.com/technomancy/leiningen), and run `lein deps`. Additionally, you will need MongoDB installed (in some future version, this will be an optional dependency). Edit .lazybot/config.clj to put the servers and other information the bot needs to have in there, get MongoDB running, and then run ./lazybot. After you run the bot the first time, you'll have to edit configuration in ~/.lazybot/config.clj (the .lazybot directory is copied to your home directory the first time).

You can also run `lein uberjar` which will create a standalone jar file for you to use to run the bot. You can just do java -jar jarfile to run it.

Lazybot has some basic background functionality. In order to use it, you must have lazybot uberjar'd. Create a file with any name. This will be your log file, where all the output from the bot is put. Next, rename lazybot-<version>-standalone.jar to lazybot.jar (this step will become unnecessary soon). After that, run `java -jar lazybot.jar --background --logpath /path/to/your/logfile`. lazybot will then start up and pump logs into the log file. Note however that this is NOT daemon functionality. If you start the bot this way, in order to kill him, you'll have to use the admin-only `die` command from the utils plugin, or look up the process id and kill him the low-level way. I may add full daemon functionality eventually, but this will suffice for now.

**IMPORTANT: If you load the clojure plugin, you must rename and move "example.policy" to "~/.java.policy" to avoid some troublesome security errors!**


## Commands

The current list of commands is maintained at [https://github.com/flatland/lazybot/wiki/Commands](), and you can find help on any particular command with $help <command> (eg $help fcst).

## Development

Contributions to lazybot are welcome, most often in the form of new plugins. If you have a great idea for something lazybot could do that would be useful or neat, don't just sit on it: Write a plugin, and send a pull request! But before you get started, take a look at [our plugin policy][before-plugin] and the [plugin quick-start guide][plugin-guide]. If you want to see how much work it takes to write a plugin, take a look at a couple plugins added by developers not on the core team:

* [$tell][], an improvement to $whatis from [ghoseb][]

* [$findfn][], for finding the clojure function you're looking for, by [jColeChanged][]

## License

Licensed under the same thing Clojure is licensed under, [the EPL](http://www.eclipse.org/legal/epl-v10.html). You can find a copy in the root of this directory.


[before-plugin]: https://github.com/flatland/lazybot/wiki/Read-this-before-writing-your-plugin
[plugin-guide]: https://github.com/flatland/lazybot/wiki/Plugin-quick-start-guide
[$findfn]: https://github.com/flatland/lazybot/compare/544566f7ee740731ca69...da4fcae5f3afe6cc9e6c
[$tell]: https://github.com/flatland/lazybot/commit/b94c36c52271766c07de9f6bfb7c4d2a429ba498
[ghoseb]: https://github.com/ghoseb
[jColeChanged]: https://github.com/jColeChanged

## Contributors

These are people who have contributed to lazybot since the beginning. These may not all be active contributors. Even one-off contributors will be added to this list.

* Anthony Grimes ([Raynes](https://github.com/Raynes))
* Alan Malloy ([amalloy](https://github.com/amalloy))
* Andrew Brehaut ([brehaut](https://github.com/brehaut))
* Baishampayan Ghose ([ghoseb](https://github.com/ghoseb))
* Curtis McEnroe ([programble](https://github.com/programble))
* Erik Price ([boredomist](https://github.com/boredomist))
* Joshua ([jColeChanged](https://github.com/jColeChanged))
* Michael D. Ivey ([ivey](https://github.com/ivey))
* Pepijn de Vos ([fliebel](https://github.com/fliebel))
* Justin Balthrop ([ninjudd](https://github.com/ninjudd))
