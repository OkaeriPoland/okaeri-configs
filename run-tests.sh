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

# Configuration
# Modules that need to be installed (built without running tests)
INSTALL_MODULES=(
    "core:Core library"
    "core-test-commons:Shared test utilities and configs"
    "yaml-snakeyaml:YAML format implementation"
    "yaml-bukkit:Bukkit's YAML format implementation"
    "yaml-bungee:Bungee's YAML format implementation"
)

# Modules that need to have tests executed
TEST_MODULES=(
    "core-test:Core functionality tests"
    "yaml-snakeyaml:YAML format implementation tests"
    "yaml-bukkit:Bukkit's YAML format implementation tests"
    "yaml-bungee:Bungee's YAML format implementation tests"
)

# Start
print_section "Okaeri Configs - Test Suite Runner"
print_info "Starting build and test process..."

# Build and install modules
step=1
for module_info in "${INSTALL_MODULES[@]}"; do
    IFS=':' read -r module_name module_desc <<< "$module_info"
    
    print_section "Step $step: Building and Installing $module_name"
    print_info "$module_desc"
    
    cd "$module_name"
    mvn clean install -DskipTests
    if [ $? -eq 0 ]; then
        print_success "$module_name installed successfully"
    else
        print_error "Failed to install $module_name"
        exit 1
    fi
    cd ..
    
    ((step++))
done

# Run tests for test modules
for module_info in "${TEST_MODULES[@]}"; do
    IFS=':' read -r module_name module_desc <<< "$module_info"
    
    print_section "Step $step: Running $module_name Tests"
    print_info "$module_desc"
    
    cd "$module_name"
    mvn clean test
    if [ $? -eq 0 ]; then
        print_success "$module_name tests passed successfully"
    else
        print_error "$module_name tests failed"
        exit 1
    fi
    cd ..
    
    ((step++))
done

# Summary
print_section "Test Summary"
print_success "All tests completed successfully!"

print_info "Modules built and tested:"
for module_info in "${INSTALL_MODULES[@]}"; do
    IFS=':' read -r module_name module_desc <<< "$module_info"
    print_info "  ✓ $module_name (installed)"
done
for module_info in "${TEST_MODULES[@]}"; do
    IFS=':' read -r module_name module_desc <<< "$module_info"
    print_info "  ✓ $module_name (tests passed)"
done

# Test result locations
print_info ""
print_info "For detailed test results, check:"
for module_info in "${TEST_MODULES[@]}"; do
    IFS=':' read -r module_name module_desc <<< "$module_info"
    print_info "  - $module_name/target/surefire-reports/"
done

echo ""
print_success "Test run completed successfully! 🎉"
