#!/bin/bash
mvn clean package
cd plugin/target

sources=("github" "modrinth" "spigotmc" "builtbybit")
for src in ${sources[@]}; do
  echo "$src..."
  cp visual-crafting.jar visual-crafting-$src.jar
  echo "$src" >> metadata
  zip -ur visual-crafting-$src.jar metadata
  rm metadata
done
echo "Done"
