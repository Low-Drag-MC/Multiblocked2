#!/usr/bin/env bash
# ForgeGradle downloads its own artifacts (forge userdev/universal, mcp_config, its tools) by walking
# the project repositories itself and keeping whatever bytes come back. It only looks at the status
# code, so a mirror that answers 200 with an html page instead of 404 leaves a bogus file in the
# cache -- and that file is never re-validated, so every later build fails with
# "Invalid patcher dependency" / "zip END header not found" until it is deleted by hand.
#
# The repository setup keeps such mirrors away from those artifacts now, but a cache restored from an
# already poisoned run still carries the bad files, so drop anything that isn't what it claims to be.
set -euo pipefail

dir="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/forge_gradle/maven_downloader"
if [ ! -d "$dir" ]; then
  echo "no ForgeGradle download cache, nothing to scrub"
  exit 0
fi

removed=0
drop() {
  echo "  removing $1 ($2)"
  rm -f "$1" "$1.md5" "$1.sha1"
  removed=$((removed + 1))
}

while IFS= read -r f; do
  case "$(head -c 2 "$f" | od -An -tx1 | tr -d ' \n')" in
    504b) ;; # PK, a real zip
    *) drop "$f" "not a zip" ;;
  esac
done < <(find "$dir" -type f \( -name '*.jar' -o -name '*.zip' \))

while IFS= read -r f; do
  if head -c 512 "$f" | grep -qai '<!doctype\|<html'; then
    drop "$f" "html instead of xml"
  fi
done < <(find "$dir" -type f -name '*.xml')

echo "scrubbed $removed corrupted file(s) from $dir"
