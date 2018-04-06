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

# Part 1, What is a Service Worker?

A service worker is like a server extention. It installs in the user's
browser, intercepts network requests both to and from the page, and does
things with those requests according to how its programmed. Principally,
it can act as a proxy server that caches data for the user, so that
network requests resolve faster, web sites are available offline, and PUT/POST
requests retry in the background *after the user has closed the browser*,
should the user try one without being online. They even let you recieve push
notifications with the browser closed!

These things need to be programmed, however. They only do what you tell
them to do.

I recommend watching [Jake Archibald's Google I/O 2016 talk.](https://www.youtube.com/watch?v=cmGr0RszHc8)
It fairly neatly explains it. Then check [Is Service Worker Ready](https://jakearchibald.github.io/isserviceworkerready/)
to see that, indeed!, service workers are supported in all major browsers.

# Library Rationale and Goal

Clojurescript tooling is designed for a certain way of doing things. That's
not bad! Its actually fairly good, if you think about it. Regardless of
which tool you use to get moving, it's basically the same setup. Figwheel and
other tooling make dev a breeze with quick reloads and great error handling.
Development can be done in any browser, and for the most part things will just work!
Plus, almost everything you want to do in javascript has a clojurescript
wrapper on top with soft, fuzzy persistent data structures to play with.
Life couldn't be simpler.

Service Workers throw a monkey wrench into this. They require a build setup
that the tooling usually doesn't accout for, they require a very spcific setup in the
browser to develop, they don't reload quickly *at all*, they are async
*and stateful*, they expose a lower level api then what clojurians are used
to dealing with, and they are absolute nightmares when it comes to errors.

In short, they're a pain.

The solution? Abstract over the service worker api, and make using one
as simple as writing a configuration map. That's what this library does.
Built on top of Google Workbox, this library allows developers to
write simple and easy-to-validate clojurescript that runs at production
time, alleviating the need to constantly recompile the service worker.

# Testing and Development.

## Dev Environment

Prerequisites - Leinigen 2 or higher, Java 1.8 or higher, most recent
Chrome or Firefox, and an internet connection (due to Workbox being
served over a CDN in the default lib config).

Boot up a repl with the dev profile, run (user/start) or (user/reset), and open up
http://localhost:3000 in whatever browser(s) you wish to test in.
Assuming Chrome, open up devtools, go to the "Application" tab, check
"Update on reload". Refresh the browser a few times until all tests
show green. Any changes to the cljs files will trigger a recompile on
page refresh. Any change to dev (including gardener.clj or routes.cljc)
require the changed namespaces to be reloaded, and (user/reset) to be run.

## Testing framework

Because of the challenges presented by service workers, special care
must be taken in designing the testing environment of any library or system
that seeks to interact with them. This library runs its tests in the context
of a simple webapp, with a simple HTTPKit server that compiles CLJS at
request time. In short, a perfectly valid way to use the library is documented
by the tests, as it should be.

The testing framework is custom-built for this project. Each thing under test
gets its own map
```clojure
 {:on "thing-being-tested" :tests [{test-map-1}, {test-map2}...]}
```
Each individual test is a map with the following shape:
``` clojure
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

This library is under heavy development, but is functional enough that
a small project might get some use out of it. Be warned- breaking changes
are planned. There's plenty of refactoring to be done if nothing else.

[![Clojars Project](https://img.shields.io/clojars/v/hireling.svg)](https://clojars.org/hireling)

Requires [org.clojure/clojurescript "1.10.238"] or later.

This walkthrough is going to assume that the app getting the shiny new service worker
is one similar to what you get with lein new figwheel or lein new chestnut.
If that's the case, there are a few things that need to be in place for service workers
to work for us. Only two of them involve this library. Will there, possibly,
be a lein plugin that handles this? No. There are many means by which one
can build Clojurescript. This library is intended to be useful to all of them.

There may be a ring handler coming, however.

Anyways...

In the app's startup namespace, register the worker.
``` clojure
(ns myapp.core
  (:require [hireling.core :as hireling]))
(hireling/register-service-worker "worker-script-name.js")
```
Set up the compiler. Here's a Sample cljsbuild config that spits out a
single js file, probably for deployment. Another can be found at dev/user/worker-js-builder in this repository.
Note the :target :webworker. That's hugely important, as not only does it allow Google
Closure to correctly eliminate dead code, but it also ensures that non-DOM
bootstrapping code is included so that it will run in the first place.

``` clojure
{:id "service-worker"
 :source-paths ["src-cljc" "src-worker"]
 :compiler {:optimizations  :advanced
            :output-to "public_html/worker.js"
            :cache-analysis true
            :pretty-print   false
            :warnings       true
            :target         :webworker
            :main           "myapp.worker"}}
```
In the deployment profile map in project.clj
``` clojure
:prep-tasks ["cljsbuild" "once" "service-worker"]
```

Note - A service worker can only control stuff served from its directory or below it.
Thus, if your page is availble from "culehosts.larp/~usarnaem/index.html", your worker should be at
"culehosts.larp/~usarnaem/worker.js". [For more information...](https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API/Using_Service_Workers#Why_is_my_service_worker_failing_to_register)

In myapp.worker, which exists under the new "src-worker" sourcepath
``` clojure
(hireling/start-service-worker! {configmap})
```

The config map is documented in [hireling/worker.cljs](hireling/worker-test/hireling/worker.cljs).
As development rages on, that's where changes will be reflected. Once the
project leaves alpha and the api osifies the full documentation will exist
either in this readme.md or in a wiki page.

To get started, your
:version should be 1, your :app-name should be "myapp", paths for your
index.html, css files, js files, and any necessary resources should be under :cache-routes,
(for help, see hireling/worker.cljs).

If all went well, you now have a bit of middleware that silently, without
fanfare, and without *that* much configuration, improves your user's
experience on your site. If the site is simple (ie, just loads and does
things without requesting much else from the network) it might be
entirely functional offline! More complex apps will be enabled in future
updates, but even for them subsiquent site visits should be loads faster.

## License

Copyright © 2018 Alex Chythlook

Distributed under the Eclipse Public License 1.0, same as Clojure.

[xkcd]: https://imgs.xkcd.com/comics/installing.png