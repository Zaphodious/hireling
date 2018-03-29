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

# Library Rationale and Goal

As ClojureScript tooling expects one js output file and one
 execution context, it can be challenging to set up and properly
 deploy service workers with our apps. As service workers are explicitely
 seperate from and longer lived then the execution context that spawns them,
 it can be frustrating to iterate on them. As the service worker api is relitively
 low-level and complex, writing one in ClojureScript via interop can be painful.

 Hireling attempts to fix this by creating an abstraction over the service
 worker that a) automatically handles boilerplate tasks like registration,
 general cache handling, etc, b) exposes hooks that a developer may use to
 fully customize service worker execution, and c) allows developers to write
 their hooks such that they can be tested in a friendlier context (ie, the
 browser window where all the nice tooling is).

# Testing and Development.

## Dev Environment

Boot up a repl with the dev profile, run (user/start) or (user/reset), and open up
http://localhost:3000 in whatever browser(s) you wish to test in. Any
changes to the cljs files will trigger a recompile on page refresh.

Because of the challenges presented by service workers, special care
must be taken in designing the testing environment of any library or system
that seeks to interact with them. This library runs its tests in the context
of a simple webapp, with a simple HTTPKit server that compiles CLJS at
request time.

The testing framework is custom-built for this project. Each thing under test
gets its own map
```clojurescript
 {:on "thing-being-tested" :tests [{test-map-1}, {test-map2}...]}
```
Each individual test is a map with the following shape:
``` clojurescript
{:aspect "required, aspect under test of the subject being proven"
 :should-be "Optional, the expected result. Defaults to 'true'"
 :testing-args ["Optional, additional arguments to be passed in to the test fn.",
                "Defaults to the empty vector."]
 :test-fn (fn [is testing-arg1 testing-arg2]
            "Function wherein the test runs.
            The first argument (is) is a callback that takes the result
            of the test, and checks it against :should-be. Can be called
            within an asychronious context such as a core.async/go block
            or a js promise.
            Additional arguments are supplied by :testing-args. Without
            the declaration, this function would take one argument.
            The return value of this function is ignored.
            "
             (is "test-result")} ; This test fails, btw
```

Test groups are individually passed to the RUM component
hireling.test-runner/tests-on, and from there are ran
and rendered to the screen.

## Usage

This library is under heavy development, and as such should *not* be used
as it is right now.

[![Clojars Project](https://img.shields.io/clojars/v/hireling.svg)](https://clojars.org/hireling)

    (:require [hireling.core :as hireling])

## License

Copyright © 2018 Alex Chythlook

Distributed under the Eclipse Public License 1.0, same as Clojure.

[xkcd]: https://imgs.xkcd.com/comics/installing.png