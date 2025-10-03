
.PHONY: build
build: clean
	mvn package

.PHONY: ui
ui:
	cd chat-ui && npm run dev


.PHONY: test
test:
	mvn verify

.PHONY: run
run:
	mvn spring-boot:run -pl :app

.PHONY: clean
clean:
	mvn clean

.PHONY: no-leaks
no-leaks:
	gitleaks git .

.PHONY: otel
otel:
	lsof -ti:8888 | xargs kill -9
	osascript -e 'tell app "Terminal" to do script "otel-tui"'
	#open -a Terminal otel-tui
	#otel-tui

.PHONY: promptfoo
promptfoo:
	(cd promptfoo && promptfoo eval --watch --output output.yml --env-file .env)
	#(cd promptfoo && promptfoo eval --watch --output output.yml --no-progress-bar --env-file .env)

.PHONY: promptfoo-ui
promptfoo-ui:
	(cd promptfoo && promptfoo view --yes --env-file .env)


prepare:
	command -v gitleaks >/dev/null 2>&1 || brew install gitleaks
	command -v otel-tui >/dev/null 2>&1 || brew install otel-tui
	command -v promptfoo >/dev/null 2>&1 || brew install promptfoo
