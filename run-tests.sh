#!/bin/bash

# Okaeri Configs - Test Runner Script
# This script builds and runs tests for the okaeri-configs project

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_section() {
    echo ""
    echo -e "${BLUE}================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================================${NC}"
    echo ""
}

# Trap errors
trap 'print_error "Script failed at line $LINENO"' ERR

# Start
print_section "Okaeri Configs - Test Suite Runner"
print_info "Starting build and test process..."

# Step 1: Build and install core module
print_section "Step 1: Building and Installing core"
print_info "This module contains the core library being tested"

cd core
mvn clean install -DskipTests
if [ $? -eq 0 ]; then
    print_success "core installed successfully"
else
    print_error "Failed to install core"
    exit 1
fi
cd ..

# Step 2: Build and install core-test-commons
print_section "Step 2: Building and Installing core-test-commons"
print_info "This module provides shared test utilities and configs"

cd core-test-commons
mvn clean install -DskipTests
if [ $? -eq 0 ]; then
    print_success "core-test-commons installed successfully"
else
    print_error "Failed to install core-test-commons"
    exit 1
fi
cd ..

# Step 3: Run core-test tests
print_section "Step 3: Running core-test Tests"
print_info "This module contains core functionality tests"

cd core-test
mvn clean test
if [ $? -eq 0 ]; then
    print_success "core-test tests passed successfully"
else
    print_error "core-test tests failed"
    exit 1
fi
cd ..

# Step 4: Summary
print_section "Test Summary"
print_success "All tests completed successfully!"
print_info "Modules built and tested:"
print_info "  ✓ core (installed)"
print_info "  ✓ core-test-commons (installed)"
print_info "  ✓ core-test (tests passed)"

# Optional: Print test statistics
print_info ""
print_info "For detailed test results, check:"
print_info "  - core-test/target/surefire-reports/"

echo ""
print_success "Test run completed successfully! 🎉"
