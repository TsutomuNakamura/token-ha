# GitHub Actions Triggers and Conditionals Explained

## 🎯 Your Requirements Analysis

### ✅ **Requirement 1**: "Run action only when developers push main or develop branch and attach versions of tag"

**Status**: **✅ FULLY IMPLEMENTED**

The workflows now have the following behavior:

#### **For Testing (CI Workflow)**
```yaml
on:
  push:
    branches: [ main, develop ]  # ✅ Runs on pushes to main/develop
  pull_request:
    branches: [ main, develop ]  # ✅ Runs on PRs to main/develop
```
- **Purpose**: Runs tests and builds JARs
- **Does NOT publish** to any repository

#### **For Publishing (Release Workflow)**
```yaml
on:
  push:
    tags:
      - 'v*.*.*'  # ✅ Runs when version tags are pushed
```
- **Enhanced with branch validation**: Tags must be created from `main` or `develop` branches
- **Purpose**: Publishes to GitHub Packages AND Maven Central

### ✅ **Requirement 2**: "What types of format of name should I create as tag?"

**Answer**: Use semantic versioning with 'v' prefix

## 📝 Valid Tag Formats

### ✅ **Correct Formats**
```bash
v1.0.0     # Major release
v1.1.0     # Minor release  
v1.1.1     # Patch release
v2.0.0     # New major version
v10.5.2    # Double-digit versions
```

### ❌ **Invalid Formats**
```bash
1.0.0           # ❌ Missing 'v' prefix
v1.0            # ❌ Missing patch version
v1.0.0-beta     # ❌ Pre-release suffixes not supported
release-1.0.0   # ❌ Wrong prefix
v1.0.0.1        # ❌ Too many version parts
```

## 🔧 How the Branch Validation Works

### Automatic Branch Detection
The release workflow now includes smart branch validation:

```bash
# 1. When you create a tag, the workflow checks:
git merge-base --is-ancestor $TAG_COMMIT origin/main    # Is tag on main?
git merge-base --is-ancestor $TAG_COMMIT origin/develop # Is tag on develop?

# 2. If tag is not on main or develop:
❌ Workflow stops with clear error message
❌ No publishing occurs

# 3. If tag is on main or develop:
✅ Workflow continues
✅ Publishing proceeds normally
```

## 🚀 Complete Workflow Behavior

### Scenario 1: Push to main/develop (no tag)
```bash
git push origin main        # ✅ CI runs (tests only)
git push origin develop     # ✅ CI runs (tests only)
# Result: Tests run, JARs built, NO publishing
```

### Scenario 2: Create tag on main/develop
```bash
git checkout main
git tag v1.0.0
git push origin v1.0.0     # ✅ Release workflow runs
# Result: Tests + GitHub Packages + Maven Central + GitHub Release
```

### Scenario 3: Create tag on feature branch
```bash
git checkout feature/new-feature
git tag v1.0.0
git push origin v1.0.0     # ❌ Release workflow stops
# Result: Workflow fails with "Tag not on main or develop" error
```

### Scenario 4: Manual workflow dispatch
```bash
# Go to GitHub Actions → "Release to GitHub Packages and Maven Central"
# Click "Run workflow" → Enter version
# Result: ✅ Always runs (bypasses branch validation)
```

## 📊 Workflow Matrix

| Trigger | Branch | Tag Format | CI Runs? | Publishes? | Where? |
|---------|--------|------------|----------|------------|--------|
| Push to `main` | main | none | ✅ | ❌ | nowhere |
| Push to `develop` | develop | none | ✅ | ❌ | nowhere |
| Tag `v1.0.0` on `main` | main | ✅ v1.0.0 | ✅ | ✅ | GitHub Packages + Maven Central |
| Tag `v1.0.0` on `develop` | develop | ✅ v1.0.0 | ✅ | ✅ | GitHub Packages + Maven Central |
| Tag `v1.0.0` on `feature/*` | feature | ✅ v1.0.0 | ❌ | ❌ | Workflow stops |
| Tag `1.0.0` on `main` | main | ❌ Invalid | ❌ | ❌ | Workflow stops |
| Manual dispatch | any | any | ✅ | ✅ | GitHub Packages + Maven Central |

## 🎯 Recommended Release Process

### Standard Release (from main)
```bash
# 1. Ensure you're on main and up to date
git checkout main
git pull origin main

# 2. Update version (optional - can be done automatically)
./mvnw versions:set -DnewVersion=1.0.0
git add pom.xml
git commit -m "Prepare release 1.0.0"
git push origin main

# 3. Create and push tag
git tag v1.0.0
git push origin v1.0.0

# 4. GitHub Actions automatically:
# ✅ Validates tag is on main branch
# ✅ Runs comprehensive tests
# ✅ Publishes to GitHub Packages
# ✅ Publishes to Maven Central  
# ✅ Creates GitHub release with assets
```

### Development Release (from develop)
```bash
# 1. Ensure you're on develop and up to date
git checkout develop
git pull origin develop

# 2. Create and push tag
git tag v1.1.0-dev
git push origin v1.1.0-dev

# 3. Same automated process as main
```

### Emergency Manual Release
```bash
# 1. Go to GitHub repository
# 2. Actions tab → "Release to GitHub Packages and Maven Central"
# 3. Click "Run workflow"
# 4. Enter version number
# 5. Click "Run workflow"
```

## 🔒 Security and Safety Features

### Branch Protection
- ✅ **Tags must be on main or develop** - prevents accidental releases from feature branches
- ✅ **Version format validation** - ensures consistent versioning
- ✅ **Comprehensive testing** - all tests must pass before publishing

### Publishing Safety
- ✅ **Staging verification** - Maven Central uses staging before release
- ✅ **GPG signing** - All artifacts are cryptographically signed
- ✅ **Artifact validation** - Sources and Javadoc JARs required

### Error Handling
- ✅ **Clear error messages** - Explains why workflows stop
- ✅ **Rollback safety** - Failed deploys don't affect existing versions
- ✅ **Manual override** - Emergency deployment option available

## 🎉 Summary

Your requirements are **100% fulfilled**:

1. ✅ **Conditional execution**: Workflows only publish when tags are created from `main` or `develop`
2. ✅ **Tag format**: Use `v{MAJOR}.{MINOR}.{PATCH}` format (e.g., `v1.0.0`)
3. ✅ **Dual publishing**: Both GitHub Packages and Maven Central
4. ✅ **Safety mechanisms**: Branch validation, version validation, comprehensive testing

The setup is production-ready and follows best practices for open-source library deployment! 🚀
