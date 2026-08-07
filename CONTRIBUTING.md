# Contributing to True Combat Manager

Thank you for considering contributing to True Combat Manager! This document provides guidelines and instructions for contributors.

## Table of Contents

- [Development Setup](#development-setup)
- [Development Workflow](#development-workflow)
- [Branch Strategy](#branch-strategy)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Release Process](#release-process)

## Development Setup

### Prerequisites

- **JDK 21** or higher
- **Maven 3.8+**
- **Git**
- **Make** (optional, for convenience commands)

### Setting Up Your Environment

1. **Fork the repository** on GitHub

2. **Clone your fork:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/truecombatmanager.git
   cd truecombatmanager
   ```

3. **Set up upstream remote:**
   ```bash
   git remote add upstream https://github.com/ORIGINAL_OWNER/truecombatmanager.git
   ```

4. **Verify build:**
   ```bash
   make validate
   # or
   mvn validate
   ```

5. **Run tests:**
   ```bash
   make test
   # or
   mvn test
   ```

## Development Workflow

### Using Make Commands

We provide a Makefile with convenient commands:

```bash
make help              # Show all available commands
make clean             # Clean build artifacts
make build             # Build without tests
make test              # Run all tests
make coverage          # Generate code coverage report
make analyze           # Run static analysis
make format            # Format code
make release           # Prepare release artifacts
```

### Maven Commands

If you prefer direct Maven usage:

```bash
mvn clean compile              # Compile
mvn clean test                 # Run tests
mvn clean package -DskipTests  # Build JAR
mvn jacoco:report              # Generate coverage
```

## Branch Strategy

We follow a simplified Git Flow:

- **`main`**: Production-ready code. Only accept PRs from `develop` or hotfix branches.
- **`develop`**: Integration branch for features. Default target for feature PRs.
- **`feature/*`**: New features (e.g., `feature/combat-logging`)
- **`bugfix/*`**: Bug fixes (e.g., `bugfix/null-pointer-combat`)
- **`hotfix/*`**: Critical production fixes
- **`release/*`**: Release preparation branches

### Creating a Feature Branch

```bash
git checkout develop
git pull upstream develop
git checkout -b feature/your-feature-name
```

## Commit Guidelines

### Commit Message Format

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `build`: Build system changes
- `ci`: CI/CD configuration changes
- `chore`: Maintenance tasks
- `revert`: Reverting previous commits

### Examples

```bash
feat(combat): add combo counter system

Implement combo counter that tracks consecutive hits.
Resets after 3 seconds of no combat action.

Closes #123

---

fix(database): resolve connection pool leak

Ensure connections are properly returned to the pool
after query execution.

Fixes #456

---

docs(readme): update installation instructions

Add detailed steps for PaperMC server setup.
```

### Pre-commit Checklist

Before committing:

- [ ] Code compiles without errors
- [ ] Tests pass locally
- [ ] Code is formatted
- [ ] No obvious performance issues
- [ ] Commit message follows convention

## Pull Request Process

### Creating a PR

1. **Push your branch:**
   ```bash
   git push origin feature/your-feature
   ```

2. **Open a Pull Request** against the `develop` branch

3. **Fill out the PR template** completely

### PR Requirements

- **Descriptive title** following commit message format
- **Complete description** explaining:
  - What changes were made
  - Why they were made
  - How to test the changes
- **Linked issues** (if applicable)
- **Passing CI checks**
- **Code review approval**

### PR Labels

Use appropriate labels:

- `feature` - New functionality
- `bugfix` - Bug fixes
- `documentation` - Docs only
- `performance` - Performance-related
- `breaking-change` - Breaking API changes
- `needs-tests` - Requires additional tests
- `ready-for-review` - Ready for review

### Review Process

1. **Automated Checks**: CI must pass
2. **Code Review**: At least one maintainer approval
3. **Testing**: QA testing for significant changes
4. **Merge**: Squash and merge by maintainer

## Release Process

### Version Numbering

We follow [Semantic Versioning](https://semver.org/):

- **MAJOR.MINOR.PATCH** (e.g., 1.3.0)
- Pre-release suffixes: `-alpha`, `-beta`, `-rc1`

### Creating a Release

#### Option 1: Using Make

```bash
# Update version in pom.xml
make bump-version VERSION=1.4.0

# Verify and create tag
make release
make release-tag

# Push tag (triggers release workflow)
git push origin v1.4.0
```

#### Option 2: Manual Process

1. **Update version in `pom.xml`:**
   ```xml
   <version>1.4.0</version>
   ```

2. **Commit the change:**
   ```bash
   git add pom.xml
   git commit -m "chore(release): prepare release v1.4.0"
   ```

3. **Create and push tag:**
   ```bash
   git tag -a v1.4.0 -m "Release v1.4.0"
   git push origin v1.4.0
   ```

4. **GitHub Actions** will automatically:
   - Validate the release
   - Build artifacts
   - Create checksums
   - Publish GitHub Release
   - Deploy to Maven repository (for stable releases)

### Pre-release Versions

For alpha/beta releases:

```bash
# Set pre-release version
make snapshot
# Or manually set: 1.4.0-beta

# Follow same release process
# The workflow will detect pre-release and mark accordingly
```

## Testing Guidelines

### Writing Tests

- **Unit tests**: Test individual components in isolation
- **Integration tests**: Test component interactions
- **Mock external dependencies** when appropriate

### Running Tests

```bash
# All tests
make test

# Unit tests only
make test-unit

# Integration tests
make test-integration

# With coverage
make coverage
```

### Test Coverage

- Aim for **>80% coverage** on new code
- Critical paths should have **100% coverage**
- Use Jacoco reports to identify gaps

## Code Style

### Java Style Guide

- Follow [Oracle Java Code Conventions](https://www.oracle.com/java/technologies/javase/codeconventions-introduction.html)
- Use **4 spaces** for indentation (no tabs)
- **Max line length**: 120 characters
- **Braces**: Always use braces for control structures

### Static Analysis

We use automated tools to maintain code quality:

```bash
# Check code style
make lint

# Run static analysis
make analyze
```

## Questions?

- Check existing [issues](https://github.com/your-repo/issues)
- Join our community discussions
- Ask in PR comments or issues

Thank you for contributing! 🎉
