#!/bin/sh
# Run every check in docs/measurement.md inside a Linux container.
#
#   sh tools/linux-run.sh > media/captures/linux-run.txt
#
# Uses maven:3.9-eclipse-temurin-21 with python3 and zip added. The
# repository is mounted read-only and copied, so the tools write their
# outputs into the copy; the script then diffs them against the committed
# files. No Paper server or Minecraft client is started.

set -eu

REPO=$(cd "$(dirname "$0")/.." && pwd)
IMAGE=maven:3.9-eclipse-temurin-21

docker pull -q "$IMAGE" >/dev/null

docker run --rm -v "$REPO":/repo:ro "$IMAGE" sh -c '
set -u
section() { printf "\n=== %s\n" "$*"; }

apt-get -qq update >/dev/null 2>&1 && apt-get -qq install -y python3 zip unzip >/dev/null 2>&1
cp -r /repo /tmp/ba && cd /tmp/ba && rm -rf target

section "environment"
uname -srm
python3 --version
java -version 2>&1 | head -n1
mvn -version 2>/dev/null | head -n1

section "tools/catalogue.py"
python3 tools/catalogue.py
git diff --no-index --quiet /repo/docs/items-and-ammunition.md docs/items-and-ammunition.md \
    && echo "docs/items-and-ammunition.md unchanged" || echo "docs/items-and-ammunition.md differs"

section "tools/ballistics.py"
python3 tools/ballistics.py
for f in trajectory impact-model; do
    cmp -s /repo/media/$f.svg media/$f.svg && echo "media/$f.svg byte-identical" || echo "media/$f.svg differs"
done

section "tools/packcheck.py"
python3 tools/packcheck.py
echo "exit=$?"

section "mvn -B -q verify"
mvn -B -q verify >/tmp/mvn.log 2>&1
echo "exit=$?"
grep -h "Tests run" target/surefire-reports/*.txt
ls -l target/*.jar | awk "{print \$5, \$9}"

section "resource pack archive"
(cd resource-pack && zip -qr ../target/BKKArsenal-resource-pack-1.1.0.zip pack.mcmeta assets)
unzip -l target/BKKArsenal-resource-pack-1.1.0.zip | tail -n 1
stat -c "%s bytes" target/BKKArsenal-resource-pack-1.1.0.zip
'
