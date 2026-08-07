# Build Automation and Development Commands

.PHONY: all clean build test release help validate format analyze coverage docker-build

## Default target
all: validate build test

## Configuration
MVN = mvn
JAVA_VERSION = 21
ARTIFACT_NAME = truecombatmanager

# Parse version from pom.xml
VERSION = $(shell $(MVN) help:evaluate -Dexpression=project.version -q -DforceStdout)
JAR_FILE = target/$(ARTIFACT_NAME)-$(VERSION).jar

help: ## Show this help message
	@echo "True Combat Manager - Build Commands"
	@echo "====================================="
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'
	@echo ""

clean: ## Clean build artifacts
	$(MVN) clean

validate: ## Validate project and check dependencies
	$(MVN) validate dependency:resolve

compile: ## Compile source files
	$(MVN) compile

build: ## Build the project (skip tests)
	$(MVN) clean package -DskipTests

test: ## Run all tests
	$(MVN) clean test

test-unit: ## Run only unit tests
	$(MVN) test -Dtest="**/*Test.java"

test-integration: ## Run integration tests
	$(MVN) test -Pintegration-tests

coverage: ## Run tests with code coverage
	$(MVN) clean test jacoco:report
	@echo "Coverage report generated at: target/site/jacoco/index.html"

format: ## Format code
	$(MVN) spotless:apply || echo "Spotless not configured, skipping..."

analyze: ## Run static analysis (PMD, SpotBugs)
	$(MVN) pmd:check spotbugs:check || echo "Analysis completed with warnings"

lint: ## Check code style without modifying
	$(MVN) checkstyle:check

release: ## Prepare for release (validate version, build, create checksums)
	@echo "Preparing release for version $(VERSION)"
	@if echo "$(VERSION)" | grep -qiE "snapshot"; then \
		echo "⚠️  Warning: Version contains 'SNAPSHOT'"; \
		echo "Consider updating version in pom.xml before release"; \
	fi
	$(MVN) clean package -DskipTests
	@echo "Generating checksums..."
	cd target && sha256sum $(JAR_FILE) > $(JAR_FILE).sha256
	cd target && md5sum $(JAR_FILE) > $(JAR_FILE).md5
	@echo "✓ Release artifacts ready in target/"
	@ls -lh target/*.jar

release-tag: ## Create git tag for release
	@echo "Creating tag v$(VERSION)..."
	git tag -a "v$(VERSION)" -m "Release version $(VERSION)"
	@echo "✓ Tag created. Push with: git push origin v$(VERSION)"

install-local: ## Install to local Maven repository
	$(MVN) install

deploy: ## Deploy to remote repository
	$(MVN) deploy

docker-build: ## Build Docker image
	docker build -t $(ARTIFACT_NAME):$(VERSION) .

docker-run: ## Run Docker container locally
	docker run --rm -it $(ARTIFACT_NAME):$(VERSION)

dev: ## Development mode - watch and rebuild
	$(MVN) clean compile

docs: ## Generate Javadoc
	$(MVN) javadoc:javadoc
	@echo "Javadoc generated at: target/site/apidocs/index.html"

dependency-tree: ## Show dependency tree
	$(MVN) dependency:tree

dependency-analyze: ## Analyze dependencies for issues
	$(MVN) dependency:analyze

security-check: ## Run OWASP dependency check
	$(MVN) org.owasp:dependency-check-maven:check

bump-version: ## Bump version (usage: make bump-version VERSION=1.4.0)
ifndef VERSION
	$(error VERSION is required. Usage: make bump-version VERSION=1.4.0)
endif
	@echo "Bumping version to $(VERSION)"
	mvn versions:set -DnewVersion=$(VERSION)
	@echo "✓ Version updated. Don't forget to commit changes!"

snapshot: ## Convert to snapshot version (usage: make snapshot)
	$(MVN) versions:set -DnewVersion=$(shell echo $(VERSION) | sed 's/-SNAPSHOT//')-SNAPSHOT

## Quick commands for common workflows
dev-build: clean compile ## Quick development build (no tests)
prod-build: validate build ## Production build
ci-test: validate test ## CI-style test run
full-build: clean validate test build ## Full build with tests

# Verify JAR file exists after build
verify-build: build
	@if [ ! -f "$(JAR_FILE)" ]; then \
		echo "::error::Build failed - JAR not found: $(JAR_FILE)"; \
		exit 1; \
	else \
		echo "✓ Build successful: $(JAR_FILE)"; \
		file "$(JAR_FILE)"; \
	fi
