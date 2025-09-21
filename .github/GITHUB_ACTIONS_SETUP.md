# GitHub Actions Setup Guide

This guide explains how to set up the GitHub Actions workflows for automated testing, building, and publishing of the TokenHa library.

## 🔧 Prerequisites

Before the workflows can function properly, you need to configure several secrets in your GitHub repository settings.

### Required Secrets

Navigate to your repository → Settings → Secrets and variables → Actions, and add the following secrets:

#### For Maven Central Deployment

1. **`OSSRH_USERNAME`** - Your Sonatype OSSRH username
   - Sign up at: https://issues.sonatype.org/
   - Create a JIRA ticket to request access to your groupId

2. **`OSSRH_PASSWORD`** - Your Sonatype OSSRH password (or token)
   - Use your JIRA password or create a user token

3. **`GPG_PRIVATE_KEY`** - Your GPG private key for signing artifacts
   ```bash
   # Generate a new GPG key
   gpg --gen-key
   
   # Export private key (replace with your key ID)
   gpg --armor --export-secret-keys YOUR_KEY_ID
   ```

4. **`GPG_PASSPHRASE`** - Passphrase for your GPG key

#### Automatic Secrets (No setup required)

- **`GITHUB_TOKEN`** - Automatically provided by GitHub for GitHub Packages

## 📋 Workflow Overview

### 1. CI Workflow (`ci.yml`)

**Triggers:** Push to `main`/`develop` branches, Pull Requests
**Purpose:** Continuous integration testing

**Features:**
- ✅ Tests on multiple Java versions (17, 21)
- ✅ Caches Maven dependencies for faster builds
- ✅ Generates test reports
- ✅ Builds JAR artifacts
- ✅ Uploads artifacts for download

### 2. Release Workflow (`release.yml`)

**Triggers:** 
- Git tags matching `v*.*.*` (e.g., `v1.0.0`)
- Manual workflow dispatch

**Purpose:** Automated release to both GitHub Packages and Maven Central

**Features:**
- ✅ Validates version format
- ✅ Runs comprehensive tests
- ✅ Deploys to GitHub Packages
- ✅ Deploys to Maven Central (tag releases only)
- ✅ Creates GitHub releases with assets
- ✅ Notifies on success/failure

### 3. Manual Deploy Workflow (`manual-deploy.yml`)

**Triggers:** Manual workflow dispatch only
**Purpose:** Emergency deployments or testing Maven Central deployment

**Features:**
- ✅ Manual version specification
- ✅ Optional test skipping
- ✅ Direct Maven Central deployment
- ✅ Artifact upload for verification

## 🚀 Usage Instructions

### Automatic Release (Recommended)

1. **Prepare your release:**
   ```bash
   # Update version in pom.xml if needed
   ./mvnw versions:set -DnewVersion=1.0.0
   
   # Commit changes
   git add pom.xml
   git commit -m "Prepare release 1.0.0"
   git push
   ```

2. **Create and push a version tag:**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

3. **The workflow will automatically:**
   - Run tests
   - Deploy to GitHub Packages
   - Deploy to Maven Central
   - Create a GitHub release

### Manual Release

1. **Go to Actions tab in your repository**
2. **Select "Release to GitHub Packages and Maven Central"**
3. **Click "Run workflow"**
4. **Enter the version number (e.g., 1.0.0)**
5. **Click "Run workflow"**

### Emergency Maven Central Deployment

1. **Go to Actions tab in your repository**
2. **Select "Manual Deploy to Maven Central"**
3. **Click "Run workflow"**
4. **Enter version and options**
5. **Click "Run workflow"**

## 📦 Published Artifacts

### GitHub Packages
- **URL**: `https://maven.pkg.github.com/tsutomunakamura/token-ha`
- **Dependency:**
  ```xml
  <dependency>
      <groupId>com.github.tsutomunakamura</groupId>
      <artifactId>token-ha</artifactId>
      <version>1.0.0</version>
  </dependency>
  ```

### Maven Central
- **URL**: `https://central.sonatype.com/artifact/com.github.tsutomunakamura/token-ha`
- **Dependency:** Same as above
- **Search**: https://search.maven.org/

## 🔍 Monitoring and Troubleshooting

### Check Workflow Status
- Go to the **Actions** tab in your repository
- Monitor running workflows and their logs
- Check artifact uploads in the workflow summary

### Common Issues

#### GPG Signing Issues
```bash
# Verify your GPG key
gpg --list-secret-keys

# Test signing locally
echo "test" | gpg --clearsign
```

#### Maven Central Issues
- Check Sonatype OSSRH status: https://status.sonatype.com/
- Verify your credentials at: https://s01.oss.sonatype.org/
- Review deployment logs in the workflow

#### Version Issues
- Ensure version follows semantic versioning (X.Y.Z)
- Check that the version doesn't already exist in Maven Central
- Verify pom.xml version matches the release tag

### Artifact Verification

After successful deployment, verify your artifacts:

1. **GitHub Packages:**
   ```bash
   # Configure authentication for GitHub Packages
   mvn dependency:get -Dartifact=com.github.tsutomunakamura:token-ha:1.0.0
   ```

2. **Maven Central:**
   ```bash
   # Should work without authentication
   mvn dependency:get -Dartifact=com.github.tsutomunakamura:token-ha:1.0.0
   ```

## 📈 Best Practices

### Version Management
- Use semantic versioning (MAJOR.MINOR.PATCH)
- Create release branches for major versions
- Tag releases consistently (v1.0.0, v1.0.1, etc.)

### Testing Strategy
- Ensure all tests pass before releasing
- Use the CI workflow to validate changes
- Test on multiple Java versions

### Release Process
1. Develop on feature branches
2. Merge to `develop` branch
3. Test thoroughly on `develop`
4. Merge to `main` when ready for release
5. Tag the release on `main`
6. Let GitHub Actions handle the deployment

## 🆘 Support

If you encounter issues with the workflows:

1. Check the workflow logs in the Actions tab
2. Verify all required secrets are configured
3. Test Maven commands locally first
4. Check Sonatype OSSRH status and requirements

For Maven Central specific issues, refer to:
- [Sonatype OSSRH Guide](https://central.sonatype.org/publish/publish-guide/)
- [Maven GPG Plugin Documentation](https://maven.apache.org/plugins/maven-gpg-plugin/)
