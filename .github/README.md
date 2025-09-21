# GitHub Configuration

This directory contains GitHub-specific configuration files for the TokenHa project.

## 📁 Directory Contents

### Workflows (`.github/workflows/`)

- **`ci.yml`** - Continuous Integration workflow
  - Runs tests on Java 17 and 21
  - Builds JAR artifacts
  - Triggered by pushes and pull requests

- **`release.yml`** - Release workflow
  - Deploys to GitHub Packages and Maven Central
  - Creates GitHub releases
  - Triggered by version tags (v*.*.*) or manual dispatch

- **`manual-deploy.yml`** - Manual deployment workflow
  - Emergency deployments to Maven Central
  - Manual trigger only

- **`update-version.yml`** - Version management workflow
  - Updates version in pom.xml and README.md
  - Can create pull requests for version updates
  - Manual trigger only

### Configuration Files

- **`dependabot.yml`** - Dependabot configuration
  - Automatic dependency updates
  - Weekly schedule for Maven and GitHub Actions dependencies

- **`GITHUB_ACTIONS_SETUP.md`** - Setup guide
  - Detailed instructions for configuring secrets
  - Workflow usage documentation
  - Troubleshooting guide

## 🚀 Quick Start

1. **Set up required secrets** (see [GITHUB_ACTIONS_SETUP.md](GITHUB_ACTIONS_SETUP.md))
2. **Push changes** to trigger CI workflow
3. **Create version tags** to trigger release workflow
4. **Use manual workflows** for emergency deployments

## 🔗 Useful Links

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Maven Central Publishing Guide](https://central.sonatype.org/publish/publish-guide/)
- [GitHub Packages Maven Guide](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
