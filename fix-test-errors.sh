#!/bin/bash

# Fix Test Compilation Script
# This script helps identify and fix remaining test compilation issues

echo "=== Fixing Test Compilation Issues ==="
echo

# Check for Java installation
if ! command -v java >/dev/null 2>&1; then
    echo "Java is not installed or not in PATH"
    # Try to install OpenJDK
    echo "Attempting to install OpenJDK..."
    if command -v apt-get >/dev/null 2>&1; then
        sudo apt-get update && sudo apt-get install -y openjdk-17-jdk
        export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
    fi
fi

# Check for Maven
if ! command -v mvn >/dev/null 2>&1; then
    echo "Maven is not installed or not in PATH"
    if command -v apt-get >/dev/null 2>&1; then
        sudo apt-get install -y maven
    fi
fi

# Set JAVA_HOME if not already set
if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")
    echo "Set JAVA_HOME to: $JAVA_HOME"
fi

# Try to compile tests
echo "Attempting to compile tests..."
if mvn test-compile 2>&1 | tee test-compile.log; then
    echo "✅ Test compilation successful!"
else
    echo "❌ Test compilation failed. Checking errors..."
    
    # Look for common error patterns
    if grep -q "package.*does not exist" test-compile.log; then
        echo "Found missing package imports"
        grep "package.*does not exist" test-compile.log
    fi
    
    if grep -q "cannot find symbol" test-compile.log; then
        echo "Found missing symbols/methods"
        grep "cannot find symbol" test-compile.log
    fi
    
    if grep -q "constructor.*cannot be applied" test-compile.log; then
        echo "Found constructor issues"
        grep "constructor.*cannot be applied" test-compile.log
    fi
fi

echo "Done!"
