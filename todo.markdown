## Whatis

First of all, the `whatis` command should be renamed to `explain`.

I'd like to make whatis a little more like clojurebot's. Instead of the results of `explain` being "key = value", I'd rather them be "key <separator> value". The user should be able to do something like "$learn awake |means| not asleep" and then "$explain awake" would result in "awake means not asleep". I'd like for the separator to be allowed more than a single word. Using || to indicate a separator is just for example and not necessarily what has to be used.
