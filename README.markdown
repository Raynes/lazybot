# sexpbot

[sexpbot](http://github.com/Raynes/sexpbot) is an IRC bot written in Clojure. Under the hood, it uses [irclj](http://github.com/Raynes/irclj) for talking to IRC. The bot is highly extensible via plugins. Plugins are written in Clojure, using a lightweight DSL. There are already plugins for tons of stuff, including Haskell and Clojure code evaluation, googling, and using the bot as an operator bot. You can view all of the plugins in src/sexpbot/plugins.

The project aims to create what will eventually be a user-friendly and powerful IRC bot. Whereas clojurebot is mostly aimed at providing helpful services in the #clojure IRC channel, sexpbot is focused on being generally useful to people who aren't Clojurians. However, sexpbot also provides Clojure code evaluation, and is used by some Clojurians in their respective IRC channels primarily for that purpose.

## Usage 

Several of this bot's plugins require that you have MongoDB installed and running. The core plugins that currently do are mail, whatis, macro, seen, help, fortune and karma. You can find some quickstart guides for setting up MongoDB here: http://www.mongodb.org/display/DOCS/Quickstart. It's usually pretty painless and fast.

As for what OSes this bot actually runs on: I have no clue. I've seen it run on several Ubuntu distros, but nobody (that I know of) has yet to venture into the world of sexpbot-on-windows or mac. If you do, please let me know how it goes.

Right now, there are no distributions, so the easiest way to run the bot is to clone the repository. Install leiningen or cake and do 'lein deps' or 'cake deps' to install the project's dependencies into the lib/ directory Edit .sexpbot/info.clj to put the servers and other information the bot needs to have in there, get MongoDB running, and then run ./sexpbot. After you run the bot the first time, you'll have to edit configuration in ~/.sexpbot/info.clj.

IMPORTANT: If you load the eval plugin, you must rename and move "example.policy" to "~/.java.policy" to avoid some troublesome security errors!

## Commands

Unfortunately, I've yet to compile a list of commands. However, sexpbot has a help system. You can get help with individual commands by using `<prefix>help <command>`.

## License

Licensed under the same thing Clojure is licensed under. The EPL, of which you can find a copy of here: http://www.eclipse.org/legal/epl-v10.html and at the root of this directory.
