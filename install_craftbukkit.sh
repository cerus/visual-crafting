#!/bin/bash

# Author: Maximilian Dorn <github.com/cerus>
# Version: 1.0.0
# Repository: https://github.com/cerus/craftbukkit-install

FLAG_FORCE_INSTALL="false"
FLAG_REMOVE_DIR="false"
FLAG_DEBUG="false"

BUILDTOOLS_DOWNLOAD_URL="https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar"
WORK_DIR="./.buildtools_cache"
REQUIRED_PROGRAMS=("awk" "mvn" "grep" "wget" "xmllint") # libxml2-utils

# Adding a new Java version:
# 1) Go to https://adoptium.net/temurin/releases/?package=jdk&arch=x64&os=linux
# 2) Select the version
# 3) Download and also copy the download url to paste into JAVA_DOWNLOAD_LINKS
# 4) Get the path the archive will extract to and append it to JAVA_DIRS
# 5) Append the version to JAVA_VERS
JAVA_DOWNLOAD_LINKS=("https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u422-b05/OpenJDK8U-jdk_x64_linux_hotspot_8u422b05.tar.gz"
 "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.12%2B7/OpenJDK17U-jdk_x64_linux_hotspot_17.0.12_7.tar.gz"
  "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.4%2B7/OpenJDK21U-jdk_x64_linux_hotspot_21.0.4_7.tar.gz")
JAVA_DIRS=("./jdk8u422-b05" "./jdk-17.0.12+7" "./jdk-21.0.4+7")
JAVA_VERS=("8" "17" "21")

# STATE
CRAFTBUKKIT_VERSIONS=()
CURRENT_CRAFTBUKKIT=""
INSTALLED="0"
SKIPPED="0"
FAILED="0"
CURRENT_JAVA_IDX="0"

function error() {
    echo "/!\\ $1"
}

function info() {
    echo "[i] $1"
}

function debug() {
    if [ "$FLAG_DEBUG" == "0" ]; then
      echo "DEBUG: $1"
    fi
}

function next_java() {
    CURRENT_JAVA_IDX="$((CURRENT_JAVA_IDX+1))"
    if [ $CURRENT_JAVA_IDX -ge ${#JAVA_DIRS[@]} ]; then
      error "Exhausted all Java versions. Can not continue."
      exit 255
    fi
}

function install_java() {
  info "Installing the following Java versions: ${JAVA_VERS[*]}"
  for i in $(seq 0 $((${#JAVA_DIRS[@]}-1))); do
    link="${JAVA_DOWNLOAD_LINKS[$i]}"
    path="${JAVA_DIRS[$i]}/bin/java"
    if [ -e "$path" ]; then
      info "Java ${JAVA_VERS[$i]} is already installed"
    else
      info "Downloading $link..."
      wget "$link" --quiet
      filename="$(ls ./*.tar.gz)"
      tar -xf "$filename" > /dev/null 2>&1
      rm "$filename"
    fi
  done
}

function collect_craftbukkit_versions() {
  while IFS=$'\n' read -r line
  do
    CRAFTBUKKIT_VERSIONS+=("$line")
  done <<< "$(xmllint --xpath "//*[local-name()='dependency'][*[local-name()='artifactId' and text()='craftbukkit']][*[local-name()='groupId' and text()='org.bukkit']]/*[local-name()='version']/text()" ./**/pom.xml 2>/dev/null)"
}

function install_craftbukkit() {
  # Check if already installed
  if mvn dependency:get -Dartifact=org.bukkit:craftbukkit:"$CURRENT_CRAFTBUKKIT" > /dev/null 2>&1; then
    if [ "$FLAG_FORCE_INSTALL" != "true" ]; then
      info "CraftBukkit $CURRENT_CRAFTBUKKIT already is installed, skipping"
      SKIPPED="$((SKIPPED+1))"
      return 0
    fi
  fi

  IFS='-' read -ra parts <<< "$CURRENT_CRAFTBUKKIT"
  mcver="${parts[0]}"

  info "Compiling $CURRENT_CRAFTBUKKIT (MC $mcver) with Java ${JAVA_VERS[$CURRENT_JAVA_IDX]}"
  mkdir -p "$CURRENT_CRAFTBUKKIT"
  cd "$CURRENT_CRAFTBUKKIT" || exit

  curjava="${JAVA_DIRS[$CURRENT_JAVA_IDX]}/bin/java"
  "../$curjava" -jar ../BuildTools.jar --rev "$mcver" > /dev/null 2>&1
  btres="$?"

  logdir="../logs/$mcver"
  rm -rf "$logdir"; mkdir -p "$logdir"
  cp BuildTools.log.txt "$logdir/buildtools.log"
  touch "$logdir/maven.log"

  if [ "$btres" != "0" ]; then
    cd ..
    if grep "java.io.FileNotFoundException" "$CURRENT_CRAFTBUKKIT/BuildTools.log.txt" > /dev/null 2>&1; then
      error "CraftBukkit $CURRENT_CRAFTBUKKIT not found on SpigotMC servers"
      return 1
    elif grep "requires Java versions" "$CURRENT_CRAFTBUKKIT/BuildTools.log.txt" > /dev/null 2>&1; then
      info "CraftBukkit $CURRENT_CRAFTBUKKIT requires another Java version. Switching and trying again."
      next_java
      install_craftbukkit
      return 0
    else
      FAILED="$((FAILED+1))"
      error "Failed to install $CURRENT_CRAFTBUKKIT. Check the log file."
      return 1
    fi
  fi

  curjava="${JAVA_DIRS[$CURRENT_JAVA_IDX]}"
  info "Installing $CURRENT_CRAFTBUKKIT"
  info "  Bukkit"
  cd Bukkit || echo; JAVA_HOME="../../$curjava" mvn install >> "$logdir/maven.log" 2>&1
  info "  Spigot / API"
  cd ../Spigot/Spigot-API || echo; JAVA_HOME="../../../$curjava" mvn install >> "$logdir/maven.log" 2>&1
  info "  CraftBukkit"
  cd ../../CraftBukkit || echo; JAVA_HOME="../../$curjava" mvn install >> "$logdir/maven.log" 2>&1
  info "  Spigot / Server"
  cd ../Spigot/Spigot-Server || echo; JAVA_HOME="../../../$curjava" mvn install >> "$logdir/maven.log" 2>&1
  cd ../../../
  INSTALLED="$((INSTALLED+1))"
}

# Params
[[ "$1" == *"f"* ]] && FLAG_FORCE_INSTALL="true"
[[ "$1" == *"r"* ]] && FLAG_REMOVE_DIR="true"
[[ "$1" == *"d"* ]] && FLAG_DEBUG="true"

# Check if all required programs are installed
for app in "${REQUIRED_PROGRAMS[@]}"; do
  if ! which "$app" > /dev/null 2>&1; then
    error "$app is not installed. This script requires $app."
    exit
  fi
done

# Collect CB versions
collect_craftbukkit_versions
if [ "${#CRAFTBUKKIT_VERSIONS[@]}" == "0" ]; then
  error "No CraftBukkit versions found in project. Exiting now."
  exit
fi

# Enter work dir
mkdir -p "$WORK_DIR"
cd $WORK_DIR || exit

# Install Java
install_java

# Download BuildTools
if [ ! -e "./BuildTools.jar" ]; then
  info "Downloading BuildTools from $BUILDTOOLS_DOWNLOAD_URL..."
  wget "$BUILDTOOLS_DOWNLOAD_URL" --quiet
fi
if [ ! -e "./BuildTools.jar" ]; then
  error "Failed to download BuildTools. Exiting now."
  exit
fi

# Install CB
for ver in "${CRAFTBUKKIT_VERSIONS[@]}"; do
  CURRENT_CRAFTBUKKIT="$ver"
  install_craftbukkit
done

info "Done."
info "Installed: $INSTALLED   Skipped: $SKIPPED   Failed: $FAILED"

# Cleanup
if [ "$FLAG_REMOVE_DIR" == "true" ]; then
  cd ..
  rm -rf "$WORK_DIR"
else
  for dir in $(ls | grep R0.1-SNAPSHOT); do
    rm -rf "$dir"
  done
  rm -rf BuildTools.jar
fi