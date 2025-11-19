#!/bin/bash
# SplittyDupe Deployment Script for Google Cloud Platform
# Consolidated deployment with setup, deployment, and domain configuration
# Usage:
#   ./deploy.sh                    # Full deployment (recommended)
#   ./deploy.sh --setup            # Setup secrets only
#   ./deploy.sh --deploy           # Deploy services only
#   ./deploy.sh --domains          # Configure domains only
#   ./deploy.sh --backend          # Deploy backend only
#   ./deploy.sh --frontend         # Deploy frontend only
#   ./deploy.sh --fix-permissions  # Fix secret permissions
#   ./deploy.sh --quick            # Skip API enablement (faster)

set -e

# Configuration
PROJECT_ID="payment-splitter-476301"
REGION="us-central1"
DEPLOY_BACKEND=true
DEPLOY_FRONTEND=true
RUN_SETUP=false
RUN_DEPLOY=true
RUN_DOMAINS=false
FIX_PERMISSIONS=false
SKIP_API_CHECK=false

# Application Configuration (non-sensitive)
DATABASE_ID="transaction"
COLLECTION="receiptTable"
PROCESSOR_ID="fa4d82fd8ffd5ee2"
PROCESSOR_LOCATION="us"
APP_BASE_URL="https://www.jhaell.me"
API_URL="https://api.jhaell.me/api"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse arguments
while [[ $# -gt 0 ]]; do
  case $1 in
    --setup)
      RUN_SETUP=true
      RUN_DEPLOY=false
      RUN_DOMAINS=false
      shift
      ;;
    --deploy)
      RUN_SETUP=false
      RUN_DEPLOY=true
      RUN_DOMAINS=false
      shift
      ;;
    --domains)
      RUN_SETUP=false
      RUN_DEPLOY=false
      RUN_DOMAINS=true
      shift
      ;;
    --backend)
      DEPLOY_FRONTEND=false
      shift
      ;;
    --frontend)
      DEPLOY_BACKEND=false
      shift
      ;;
    --fix-permissions)
      FIX_PERMISSIONS=true
      RUN_SETUP=false
      RUN_DEPLOY=false
      RUN_DOMAINS=false
      shift
      ;;
    --quick)
      SKIP_API_CHECK=true
      shift
      ;;
    --help)
      echo "SplittyDupe Deployment Script"
      echo ""
      echo "Usage: $0 [OPTIONS]"
      echo ""
      echo "Options:"
      echo "  --setup              Setup secrets only"
      echo "  --deploy             Deploy services only (default)"
      echo "  --domains            Configure domain mappings only"
      echo "  --backend            Deploy backend only"
      echo "  --frontend           Deploy frontend only"
      echo "  --fix-permissions    Fix secret manager permissions"
      echo "  --quick              Skip API enablement check (faster)"
      echo "  --help               Show this help message"
      echo ""
      echo "Examples:"
      echo "  $0                   # Full deployment"
      echo "  $0 --setup           # Setup secrets first time"
      echo "  $0 --backend         # Deploy backend only"
      echo "  $0 --frontend --quick # Deploy frontend quickly"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      echo "Run '$0 --help' for usage information"
      exit 1
      ;;
  esac
done

# Function: Print header
print_header() {
  echo ""
  echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${BLUE}$1${NC}"
  echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo ""
}

# Function: Check authentication
check_auth() {
  if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | grep -q "@"; then
    echo -e "${RED}❌ Not authenticated with gcloud${NC}"
    echo "Please run: gcloud auth login"
    exit 1
  fi
}

# Function: Setup secrets
setup_secrets() {
  print_header "🔐 Setting up Google Cloud Secrets"

  echo "Enabling Secret Manager API..."
  gcloud services enable secretmanager.googleapis.com --quiet

  echo ""
  echo "Creating secrets..."

  # Check if secrets exist, create if not
  if ! gcloud secrets describe jwt-secret &>/dev/null; then
    echo -e "${YELLOW}⚠️  JWT secret not found. Generating new secure secret...${NC}"
    NEW_JWT_SECRET=$(openssl rand -base64 32)
    echo -n "$NEW_JWT_SECRET" | gcloud secrets create jwt-secret --data-file=- --quiet
    echo -e "${GREEN}✅ Created jwt-secret${NC}"
  else
    echo -e "${GREEN}✅ jwt-secret already exists${NC}"
  fi

  if ! gcloud secrets describe mail-username &>/dev/null; then
    echo -n "dragonsoul527@gmail.com" | gcloud secrets create mail-username --data-file=- --quiet
    echo -e "${GREEN}✅ Created mail-username${NC}"
  else
    echo -e "${GREEN}✅ mail-username already exists${NC}"
  fi

  if ! gcloud secrets describe mail-password &>/dev/null; then
    echo -n "jxvs enlu rdfw wuah" | gcloud secrets create mail-password --data-file=- --quiet
    echo -e "${GREEN}✅ Created mail-password${NC}"
  else
    echo -e "${GREEN}✅ mail-password already exists${NC}"
  fi

  echo ""
  echo "🔑 Granting Cloud Run access to secrets..."

  PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format="value(projectNumber)")
  COMPUTE_SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

  for secret in jwt-secret mail-username mail-password; do
    gcloud secrets add-iam-policy-binding $secret \
      --member="serviceAccount:${COMPUTE_SA}" \
      --role="roles/secretmanager.secretAccessor" \
      --quiet 2>&1 | grep -v "Updated IAM policy" || true
  done

  echo -e "${GREEN}✅ Secrets setup complete!${NC}"
  echo ""
  gcloud secrets list --format="table(name,createTime)"
}

# Function: Fix secret permissions
fix_permissions() {
  print_header "🔑 Fixing Secret Manager Permissions"

  PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format="value(projectNumber)")
  COMPUTE_SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

  echo "Service Account: $COMPUTE_SA"
  echo ""

  for secret in jwt-secret mail-username mail-password; do
    echo "  ✓ $secret"
    gcloud secrets add-iam-policy-binding $secret \
      --member="serviceAccount:${COMPUTE_SA}" \
      --role="roles/secretmanager.secretAccessor" 2>&1 | grep -v "Updated IAM policy" || true
  done

  echo ""
  echo -e "${GREEN}✅ Permissions fixed!${NC}"
}

# Function: Deploy services
deploy_services() {
  print_header "🚀 Deploying SplittyDupe to GCP"

  # Get commit SHA
  if git rev-parse --is-inside-work-tree > /dev/null 2>&1; then
    COMMIT_SHA=$(git rev-parse --short HEAD)
  else
    COMMIT_SHA=$(date +%Y%m%d-%H%M%S)
  fi

  echo "Project:  $PROJECT_ID"
  echo "Region:   $REGION"
  echo "Build:    $COMMIT_SHA"

  if [ "$DEPLOY_BACKEND" = true ] && [ "$DEPLOY_FRONTEND" = true ]; then
    echo "Target:   Backend + Frontend"
  elif [ "$DEPLOY_BACKEND" = true ]; then
    echo "Target:   Backend only"
  else
    echo "Target:   Frontend only"
  fi
  echo ""

  # Enable required APIs (skip if --quick)
  if [ "$SKIP_API_CHECK" = false ]; then
    echo "📦 Enabling required APIs..."
    gcloud services enable \
      cloudbuild.googleapis.com \
      run.googleapis.com \
      containerregistry.googleapis.com \
      secretmanager.googleapis.com \
      firestore.googleapis.com \
      documentai.googleapis.com \
      --quiet
    echo -e "${GREEN}✅ APIs enabled${NC}"
    echo ""
  fi

  # Deploy based on target
  if [ "$DEPLOY_BACKEND" = true ] && [ "$DEPLOY_FRONTEND" = false ]; then
    # Backend only
    echo "🔨 Building and deploying backend..."
    gcloud builds submit --tag="gcr.io/$PROJECT_ID/splittydupe-backend:$COMMIT_SHA" .
    gcloud run deploy splittydupe-backend \
      --image="gcr.io/$PROJECT_ID/splittydupe-backend:$COMMIT_SHA" \
      --region="$REGION" \
      --platform=managed \
      --allow-unauthenticated \
      --port=8080 \
      --memory=1Gi \
      --cpu=1 \
      --min-instances=0 \
      --max-instances=10 \
      --timeout=300 \
      --set-env-vars="GCP_PROJECT_ID=$PROJECT_ID,TRANSACTION_DATABASE_ID=$DATABASE_ID,TRANSACTION_COLLECTION=$COLLECTION,PROCESSOR_ID=$PROCESSOR_ID,PROCESSOR_LOCATION=$PROCESSOR_LOCATION,APP_BASE_URL=$APP_BASE_URL" \
      --set-secrets="JWT_SECRET=jwt-secret:latest,MAIL_USERNAME=mail-username:latest,MAIL_PASSWORD=mail-password:latest"
  elif [ "$DEPLOY_FRONTEND" = true ] && [ "$DEPLOY_BACKEND" = false ]; then
    # Frontend only
    echo "🔨 Building and deploying frontend..."
    cd react-app
    gcloud builds submit --tag="gcr.io/$PROJECT_ID/splittydupe-frontend:$COMMIT_SHA" \
      --build-arg="VITE_API_URL=$API_URL" .
    gcloud run deploy splittydupe-frontend \
      --image="gcr.io/$PROJECT_ID/splittydupe-frontend:$COMMIT_SHA" \
      --region="$REGION" \
      --platform=managed \
      --allow-unauthenticated \
      --port=8080 \
      --memory=512Mi \
      --cpu=1 \
      --min-instances=0 \
      --max-instances=5
    cd ..
  else
    # Full deployment using cloudbuild.yaml
    echo "🔨 Starting Cloud Build (both services)..."
    gcloud builds submit \
      --config=cloudbuild.yaml \
      --substitutions="_REGION=$REGION,COMMIT_SHA=$COMMIT_SHA" \
      .
  fi

  echo ""
  echo -e "${GREEN}✅ Deployment completed!${NC}"
  echo ""

  # Display service URLs
  echo "📋 Service URLs:"
  if [ "$DEPLOY_BACKEND" = true ]; then
    BACKEND_URL=$(gcloud run services describe splittydupe-backend --region=$REGION --format='value(status.url)' 2>/dev/null || echo "Not deployed")
    echo "Backend:  $BACKEND_URL"
  fi
  if [ "$DEPLOY_FRONTEND" = true ]; then
    FRONTEND_URL=$(gcloud run services describe splittydupe-frontend --region=$REGION --format='value(status.url)' 2>/dev/null || echo "Not deployed")
    echo "Frontend: $FRONTEND_URL"
  fi
  echo ""
  echo "🌐 Custom domains:"
  echo "Backend:  https://api.jhaell.me"
  echo "Frontend: https://www.jhaell.me"
}

# Function: Setup domains
setup_domains() {
  print_header "🌐 Configuring Custom Domains"

  echo "Mapping domains to Cloud Run services..."
  echo ""

  # Map backend to api.jhaell.me
  echo "1️⃣  Mapping api.jhaell.me → splittydupe-backend"
  gcloud beta run domain-mappings create \
    --service=splittydupe-backend \
    --domain=api.jhaell.me \
    --region=$REGION 2>&1 | grep -v "ERROR" || echo -e "   ${YELLOW}⚠️  Mapping may already exist${NC}"

  echo ""

  # Map frontend to jhaell.me
  echo "2️⃣  Mapping jhaell.me → splittydupe-frontend"
  gcloud beta run domain-mappings create \
    --service=splittydupe-frontend \
    --domain=jhaell.me \
    --region=$REGION 2>&1 | grep -v "ERROR" || echo -e "   ${YELLOW}⚠️  Mapping may already exist${NC}"

  echo ""

  # Map frontend to www.jhaell.me
  echo "3️⃣  Mapping www.jhaell.me → splittydupe-frontend"
  gcloud beta run domain-mappings create \
    --service=splittydupe-frontend \
    --domain=www.jhaell.me \
    --region=$REGION 2>&1 | grep -v "ERROR" || echo -e "   ${YELLOW}⚠️  Mapping may already exist${NC}"

  echo ""
  echo -e "${GREEN}✅ Domain mappings created!${NC}"
  echo ""
  echo "📋 DNS Configuration Required:"
  echo ""
  echo "Run these commands to get DNS records:"
  echo ""
  echo "  gcloud beta run domain-mappings describe --domain=api.jhaell.me --region=$REGION"
  echo "  gcloud beta run domain-mappings describe --domain=jhaell.me --region=$REGION"
  echo "  gcloud beta run domain-mappings describe --domain=www.jhaell.me --region=$REGION"
  echo ""
  echo "Then configure in NameCheap → Advanced DNS:"
  echo "  Type: A      Host: api    Value: <IP from api.jhaell.me>"
  echo "  Type: A      Host: @      Value: <IP from jhaell.me>"
  echo "  Type: CNAME  Host: www    Value: ghs.googlehosted.com"
  echo ""
  echo -e "${YELLOW}⏰ Wait 5-30 minutes for DNS propagation${NC}"
  echo -e "${YELLOW}🔒 SSL certificates will auto-provision (15-30 min after DNS)${NC}"
}

# Main execution
main() {
  print_header "🚀 SplittyDupe Deployment Manager"

  # Set the project
  gcloud config set project $PROJECT_ID --quiet

  # Check authentication
  check_auth

  # Execute based on flags
  if [ "$FIX_PERMISSIONS" = true ]; then
    fix_permissions
  elif [ "$RUN_SETUP" = true ]; then
    setup_secrets
    echo ""
    echo "Next steps:"
    echo "  1. Run: ./deploy.sh --deploy"
    echo "  2. Run: ./deploy.sh --domains"
  elif [ "$RUN_DOMAINS" = true ]; then
    setup_domains
  elif [ "$RUN_DEPLOY" = true ]; then
    deploy_services
    echo ""
    echo "Next steps:"
    echo "  1. Configure domains: ./deploy.sh --domains"
    echo "  2. View logs: gcloud run logs read splittydupe-backend --region=$REGION"
  fi

  echo ""
  echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${GREEN}✨ Done!${NC}"
  echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo ""
}

# Run main function
main
