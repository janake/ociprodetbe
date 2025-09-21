#!/bin/bash

# Deployment script for OCI Prodet Backend
# Usage: ./deploy.sh <environment> <image_tag>

set -e

ENVIRONMENT=${1:-staging}
IMAGE_TAG=${2:-latest}
IMAGE_NAME="ghcr.io/janake/ociprodetbe:${IMAGE_TAG}"

echo "Deploying OCI Prodet Backend to ${ENVIRONMENT} environment..."
echo "Image: ${IMAGE_NAME}"

case ${ENVIRONMENT} in
  "staging")
    echo "Deploying to staging environment..."
    # Example deployment commands for staging
    # docker-compose -f docker-compose.staging.yml up -d
    # or kubectl apply -f k8s/staging/
    ;;
  "production")
    echo "Deploying to production environment..."
    # Example deployment commands for production
    # docker-compose -f docker-compose.prod.yml up -d
    # or kubectl apply -f k8s/production/
    ;;
  *)
    echo "Unknown environment: ${ENVIRONMENT}"
    echo "Supported environments: staging, production"
    exit 1
    ;;
esac

echo "Deployment completed successfully!"