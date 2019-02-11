docs: examples bootstrap todomvc
	sbt doc
	sbt docs/publishMicrosite

docs-dev: examples bootstrap todomvc
	sbt docs/makeMicrosite

examples: docs/src/main/tut/demo/owlet-opt.js
bootstrap: docs/src/main/tut/demo/bootstrap-opt.js
todomvc: docs/src/main/tut/demo/todomvc-opt.js

.PHONY: examples bootstrap todomvc

docs/src/main/tut/demo/owlet-opt.js: example/src/main/scala/* src/main/scala/*
	sbt example/fullOptJS
	cp example/target/scala-2.12/example-opt.js docs/src/main/tut/demo/owlet-opt.js

docs/src/main/tut/demo/bootstrap-opt.js: bootstrap/src/main/scala/* src/main/scala/*
	sbt bootstrap/fullOptJS
	cp bootstrap/target/scala-2.12/bootstrap-opt.js docs/src/main/tut/demo/bootstrap-opt.js

docs/src/main/tut/demo/todomvc-opt.js: todomvc/src/main/scala/* src/main/scala/*
	sbt todomvc/fullOptJS
	cp todomvc/target/scala-2.12/todomvc-opt.js docs/src/main/tut/demo/todomvc-opt.js
