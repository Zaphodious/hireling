# hireling

> Let’s face it: sometimes you need a tank and you just don’t have one
in the party. Whether you’re looking to hire a few meat shields or trying
to ensure that you have a vital role covered in a smaller-than-average
party, hirelings are a way to shore things up a bit.
>
> Hirelings are designed to be simple and to get out of the way when you
don’t need them. You only have to worry about a few things.

from [Hirelings in Peril: An Option for Your Fifth Edition D&D Game](https://koboldpress.com/hirelings-in-peril-an-option-for-your-fifth-edition-dd-game/)

A ClojureScript library that helps simplify [service workers](https://developers.google.com/web/fundamentals/primers/service-workers/).

# Testing and Development.

Boot up a repl with the dev profile, run (start) or (reset), and open up
http://localhost:3000 in whatever browser(s) you wish to test/dev in.

As Service Workers are incredibly specialized micro-programs that run
in *one* context and no other, development and testing must be done in
that spcific context. That context is a webapp, and the testing
framework is a simple affair that renders results to the page.

## Usage

[![Clojars Project](https://img.shields.io/clojars/v/hireling.svg)](https://clojars.org/hireling)

    (:require [hireling.core :as hireling])

## License

Copyright © 2018 Alex Chythlook

Distributed under the Eclipse Public License 1.0, same as Clojure.

[xkcd]: https://imgs.xkcd.com/comics/installing.png