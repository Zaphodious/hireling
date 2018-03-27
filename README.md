# hireling

![But still, my scheme for creating and saving user config files and data locally to preserve them across reinstalls might be useful for--wait, that's cookies.][xkcd]

A ClojureScript library for working with service workers.

# Running Tests and Development.

Boot up a repl with the dev profile, run (start) or (reset), open up
http://localhost:3000 in whatever browser(s) you wish to test/dev in.

As Service Workers are incredibly specialized micro-programs that run
in *one* context and no other, development and testing must be done in
that spcific context. That context is a webapp, and the testing
framework is a simple affair that renders results to the screen.

## Usage

[![Clojars Project](https://img.shields.io/clojars/v/hireling.svg)](https://clojars.org/hireling)

FIXME

## License

Copyright © 2018 Alex Chythlook

Distributed under the Eclipse Public License 1.0, same as Clojure.

[xkcd]: https://imgs.xkcd.com/comics/installing.png