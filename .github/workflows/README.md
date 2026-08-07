# GitHub Workflows

This directory contains all CI/CD workflows for True Combat Manager.

## Overview

### Workflows

| Workflow | File | Description | Triggers |
|----------|------|-------------|----------|
| **CI Pipeline** | `ci-pipeline.yml` | Main continuous integration pipeline | Push to main/develop, PRs |
| **Release Manager** | `release.yml` | Automated release process | Git tags (v*), manual dispatch |
| **PR Workflow** | `pr-workflow.yml` | Pull request validation and testing | PRs, manual dispatch |
| **Maven CI** | `maven.yml` | Legacy Maven build (kept for compatibility) | Push, PRs, tags |

## Workflow Details

### 1. CI Pipeline (`ci-pipeline.yml`)

**Purpose**: Core continuous integration workflow that runs on every push and PR.

**Jobs**:
- **Validate**: Project validation and code formatting checks
- **Test**: Unit tests with result publishing
- **Build**: Build artifacts (JAR, sources, javadoc)
- **Security Scan**: OWASP dependency check (PRs only)

**Features**:
- Parallel job execution
- Automatic cancellation of obsolete runs
- Test result visualization
- Artifact retention (30 days)

### 2. Release Manager (`release.yml`)

**Purpose**: Production-grade release automation with validation and deployment.

**Jobs**:
- **Validate Release**: Version validation, POM consistency check
- **Build Release**: Create release artifacts with checksums
- **Create Release**: Generate GitHub release with changelog
- **Publish Maven**: Deploy to Maven repository (stable releases only)

**Triggers**:
- Git tag push (e.g., `v1.3.0`)
- Manual workflow dispatch

**Features**:
- Automatic pre-release detection (alpha/beta/rc/snapshot)
- SHA256/MD5 checksum generation
- Changelog generation from PR labels
- Maven repository deployment

### 3. PR Workflow (`pr-workflow.yml`)

**Purpose**: Comprehensive pull request validation and quality gates.

**Jobs**:
- **PR Validation**: Title convention, description check
- **Code Quality**: PMD, SpotBugs static analysis
- **Test Matrix**: Unit and integration tests
- **Performance Check**: JMH benchmarks (when labeled)
- **Docker Test**: Container build verification (when labeled)
- **Quality Gate**: Final status aggregation

**Features**:
- Conventional commit validation
- Conditional test execution
- Coverage reporting
- Performance regression detection

## Usage

### Running Workflows Manually

Some workflows support manual triggering via `workflow_dispatch`:

```bash
# Via GitHub UI:
# Actions -> Select Workflow -> Run Workflow

# Via GitHub CLI:
gh workflow run release.yml --field version=v1.4.0
gh workflow run pr-workflow.yml --field run_integration_tests=true
```

### Required Secrets

The following secrets must be configured in repository settings:

| Secret | Description | Required For |
|--------|-------------|--------------|
| `GITHUB_TOKEN` | Auto-provided by GitHub | All workflows |
| `MAVEN_USERNAME` | Maven repository username | Maven deployment |
| `MAVEN_PASSWORD` | Maven repository password | Maven deployment |

### Adding New Workflows

1. Create new `.yml` file in `.github/workflows/`
2. Follow naming convention: `<purpose>.yml`
3. Include proper permissions block
4. Add concurrency control if needed
5. Test with `workflow_dispatch` trigger first

## Best Practices

### Workflow Optimization

- Use `actions/checkout@v4` with appropriate `fetch-depth`
- Enable Maven caching: `cache: maven`
- Use `-B` flag for batch mode (non-interactive)
- Set `fail-fast: false` for test matrices
- Implement proper job dependencies with `needs:`

### Security

- Use minimal required permissions
- Never expose secrets in logs
- Validate inputs in `workflow_dispatch`
- Use `continue-on-error` judiciously

### Maintenance

- Keep action versions up to date
- Remove unused workflows
- Document workflow changes
- Test workflow changes in feature branches

## Troubleshooting

### Common Issues

**Workflow not triggering:**
- Check branch filters in `on:` block
- Verify file is in correct location
- Ensure YAML syntax is valid

**Job failing:**
- Check step logs for error messages
- Verify secret availability
- Test locally with same Maven commands

**Artifact upload failing:**
- Ensure files exist before upload
- Check file paths are correct
- Verify retention policy

### Debugging

Enable debug logging:
```bash
# Set repository secret: ACTIONS_RUNNER_DEBUG = true
# Or add to workflow:
env:
  ACTIONS_STEP_DEBUG: true
```

## Contributing

When modifying workflows:
1. Update this README
2. Test changes thoroughly
3. Document new features
4. Consider backward compatibility

---

For more information, see:
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Workflow Syntax Reference](https://docs.github.com/en/actions/reference/workflow-syntax-for-github-actions)
