#!/usr/bin/env bash
# Records the variable-font sizzle reel (SizzleReel.kt) on the View player and joins the scenes,
# with crossfades, into sizzle-reel.mp4 and sizzle-reel.gif in the output directory.
#
#   remotecompose/fontvariation/sizzle-reel.sh [out-dir]
#
# SIZZLE_FPS and SIZZLE_SECONDS (per scene) default to 30 and 4.
set -euo pipefail

root="$(cd "$(dirname "$0")/../.." && pwd)"
out="$(mkdir -p "${1:-$root/remotecompose/fontvariation/build/sizzle}" && cd "${1:-$root/remotecompose/fontvariation/build/sizzle}" && pwd)"
fps="${SIZZLE_FPS:-30}"
seconds="${SIZZLE_SECONDS:-4}"
fade=0.5

if [[ "${SIZZLE_SKIP_RECORD:-}" != 1 ]]; then
  rm -rf "$out"/[0-9]-*
  (cd "$root" && SIZZLE_OUT="$out" SIZZLE_FPS="$fps" SIZZLE_SECONDS="$seconds" \
    build-brief ./gradlew :remotecompose:fontvariation:testDebugUnitTest \
    --tests '*SizzleReelRecorder*' --rerun)
fi

scenes=("$out"/[0-9]-*/)
inputs=()
for s in "${scenes[@]}"; do inputs+=(-framerate "$fps" -i "${s}frame_%04d.png"); done

# Chain the scenes with crossfades; each fade starts `fade` seconds before its scene ends.
filter=""
prev="[0:v]"
offset=0
for ((i = 1; i < ${#scenes[@]}; i++)); do
  offset=$(echo "$offset + $seconds - $fade" | bc)
  filter+="${prev}[$i:v]xfade=transition=circleopen:duration=$fade:offset=$offset[v$i];"
  prev="[v$i]"
done
filter+="${prev}format=yuv420p[out]"

ffmpeg -loglevel error -y "${inputs[@]}" -filter_complex "$filter" -map '[out]' \
  -c:v libx264 -crf 18 -preset slow -movflags +faststart "$out/sizzle-reel.mp4"
ffmpeg -loglevel error -y -i "$out/sizzle-reel.mp4" \
  -vf "fps=20,scale=320:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=128[p];[b][p]paletteuse=dither=sierra2_4a" \
  "$out/sizzle-reel.gif"
echo "$out/sizzle-reel.mp4"
echo "$out/sizzle-reel.gif"
