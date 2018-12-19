docs: examples bootstrap
	sbt doc
	sbt docs/publishMicrosite

docs-dev: examples
	sbt docs/makeMicrosite

examples: docs/src/main/tut/demo/owlet-opt.js
bootstrap: docs/src/main/tut/demo/bootstrap-opt.js

.PHONY: examples

docs/src/main/tut/demo/owlet-opt.js: example/src/main/scala/* src/main/scala/*
	sbt example/fastOptJS
	cp example/target/scala-2.12/example-fastopt.js docs/src/main/tut/demo/owlet-opt.js

docs/src/main/tut/demo/bootstrap-opt.js: bootstrap/src/main/scala/* src/main/scala/*
	sbt bootstrap/fastOptJS
	cp bootstrap/target/scala-2.12/bootstrap-fastopt.js docs/src/main/tut/demo/bootstrap-opt.js
