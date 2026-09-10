#!/bin/bash
# Build test script for EnsNightMarket

cd "C:\Users\Enes\Desktop\Projeler\Pluginler\EnsNightMarket"

echo "=== EnsNightMarket Build Test ==="
echo "Cleaning previous build..."
if (Test-Path "target") { Remove-Item -Recurse -Force target }

echo "Compiling..."
mvn clean compile -q

if ($LASTEXITCODE -eq 0) {
    echo "Build successful!"
    echo ""
    echo "Generated files:"
    Get-ChildItem -Path "target\classes" -Recurse -Name
} else {
    echo "Build failed! Check errors above."
    exit 1
}
