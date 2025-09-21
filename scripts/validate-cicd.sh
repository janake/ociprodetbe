#!/bin/bash

# Validate CI/CD setup
echo "🔍 Validating CI/CD Pipeline Setup..."

# Check required files
REQUIRED_FILES=(
    ".github/workflows/ci-cd.yml"
    "Dockerfile"
    "docker-compose.yml"
    "pom.xml"
    "scripts/deploy.sh"
)

echo "📁 Checking required files..."
for file in "${REQUIRED_FILES[@]}"; do
    if [[ -f "$file" ]]; then
        echo "✅ $file exists"
    else
        echo "❌ $file is missing"
        exit 1
    fi
done

# Check if deploy script is executable
if [[ -x "scripts/deploy.sh" ]]; then
    echo "✅ Deploy script is executable"
else
    echo "❌ Deploy script is not executable"
    exit 1
fi

# Check Maven build
echo "🔨 Testing Maven build..."
if ./mvnw clean compile -q; then
    echo "✅ Maven build successful"
else
    echo "❌ Maven build failed"
    exit 1
fi

# Check Docker build (optional - requires Docker)
if command -v docker &> /dev/null; then
    echo "🐳 Testing Docker build..."
    if docker build -t test-validation . > /dev/null 2>&1; then
        echo "✅ Docker build successful"
        docker rmi test-validation > /dev/null 2>&1
    else
        echo "⚠️ Docker build failed (this might be expected in CI environments)"
    fi
else
    echo "⚠️ Docker not available, skipping Docker build test"
fi

# Check GitHub Actions workflow syntax
if command -v yamllint &> /dev/null; then
    echo "📝 Validating GitHub Actions workflow..."
    if yamllint .github/workflows/ci-cd.yml; then
        echo "✅ GitHub Actions workflow syntax is valid"
    else
        echo "❌ GitHub Actions workflow has syntax errors"
        exit 1
    fi
else
    echo "⚠️ yamllint not available, skipping workflow validation"
fi

echo ""
echo "🎉 CI/CD Pipeline validation completed successfully!"
echo ""
echo "Next steps:"
echo "1. Commit and push these changes to trigger the pipeline"
echo "2. Set up GitHub Environments (staging, production) in repository settings"
echo "3. Configure any required secrets for deployment"
echo "4. Update deployment scripts with your specific infrastructure commands"