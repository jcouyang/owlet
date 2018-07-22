docs: examples
	sbt doc
	sbt docs/publishMicrosite

docs-dev: examples
	sbt docs/makeMicrosite

examples: docs/src/main/tut/todomvc/app.js docs/src/main/tut/demo/owlet-opt.js

.PHONY: examples

docs/src/main/tut/todomvc/app.js: todomvc/src/main/scala/*
	sbt todomvc/fastOptJS
	cp todomvc/target/scala-2.12/todomvc-fastopt.js docs/src/main/tut/todomvc/app.js

docs/src/main/tut/demo/owlet-opt.js: example/src/main/scala/*
	sbt example/fastOptJS
	cp example/target/scala-2.12/example-fastopt.js docs/src/main/tut/demo/owlet-opt.js
