#! /bin/bash

JDK_VERSION="21.0.4"
MAVEN_VERSION="3.8.9"
JDK_DIR="$(pwd)/jdk"
MAVEN_DIR="$(pwd)/maven"

# Java 21.0.4 install (This download is specifically 21.0.4+7)
echo "Downloading Java $JDK_VERSION..."
wget -q -O jdk.tar.gz "https://aka.ms/download-jdk/microsoft-jdk-$JDK_VERSION-linux-x64.tar.gz"
mkdir -p "$JDK_DIR"
tar -xzf jdk.tar.gz -C "$JDK_DIR" --strip-components=1
rm jdk.tar.gz

# Maven 3.9.9 install
echo "Downloading Maven $MAVEN_VERSION..."
wget -q -O maven.tar.gz "https://dlcdn.apache.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz"
mkdir -p "$MAVEN_DIR"
tar -xzf maven.tar.gz -C "$MAVEN_DIR" --strip-components=1
rm maven.tar.gz

# Environment variables (temporary in bash)
export JAVA_HOME="$JDK_DIR"
export MAVEN_HOME="$MAVEN_DIR"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"

# Build using JavaPackager
echo "Building project..."
mvn package "-DjdkPath=$JAVA_HOME"

# Find tar.gz Linux build
LINUX_BUILD=$(find target -name "*.tar.gz")
LINUX_BUILD_NAME=$(basename "$LINUX_BUILD")
EXISTING_BUILD="$(pwd)/$LINUX_BUILD_NAME"

# Remove existing Linux build tar.gz if it exists
if [ -f "$EXISTING_BUILD" ]; then
    echo "Removing existing build: $EXISTING_BUILD"
    rm -f "$EXISTING_BUILD"
fi

# Move new Linux build to repo root
echo "Moving final build..."
mv "$LINUX_BUILD" .

# Remove Java, Maven, and unwanted build output
echo "Cleaning up..."
rm -rf "$JDK_DIR"
rm -rf "$MAVEN_DIR"
rm -rf target

echo "Finished!"
