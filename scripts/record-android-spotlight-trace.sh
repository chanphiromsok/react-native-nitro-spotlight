#!/usr/bin/env bash
set -euo pipefail

DEVICE="${ANDROID_SERIAL:-}"
PACKAGE="${SPOTLIGHT_PACKAGE:-spotlight.example}"
DURATION_SECONDS="${1:-15}"
OUT_DIR="${SPOTLIGHT_TRACE_DIR:-$PWD}"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
REMOTE_CONFIG="/data/local/tmp/spotlight-perfetto-config.pbtxt"
REMOTE_TRACE="/data/misc/perfetto-traces/spotlight-$TIMESTAMP.pftrace"
LOCAL_TRACE="$OUT_DIR/spotlight-$TIMESTAMP.pftrace"

if [[ -z "$DEVICE" ]]; then
  DEVICE="$(adb devices | awk 'NR > 1 && $2 == "device" { print $1; exit }')"
fi

if [[ -z "$DEVICE" ]]; then
  echo "No Android device/emulator found. Start an emulator, then run this script again." >&2
  exit 1
fi

mkdir -p "$OUT_DIR"

CONFIG="buffers: {
  size_kb: 32768
  fill_policy: RING_BUFFER
}

data_sources: {
  config {
    name: \"linux.ftrace\"
    ftrace_config {
      ftrace_events: \"sched/sched_switch\"
      ftrace_events: \"sched/sched_wakeup\"
      ftrace_events: \"power/cpu_frequency\"
      ftrace_events: \"power/cpu_idle\"
      atrace_categories: \"gfx\"
      atrace_categories: \"input\"
      atrace_categories: \"view\"
      atrace_categories: \"wm\"
      atrace_categories: \"am\"
      atrace_categories: \"dalvik\"
      atrace_apps: \"$PACKAGE\"
    }
  }
}

duration_ms: $((DURATION_SECONDS * 1000))
"

printf '%s' "$CONFIG" > /tmp/spotlight-perfetto-config.pbtxt
adb -s "$DEVICE" push /tmp/spotlight-perfetto-config.pbtxt "$REMOTE_CONFIG" >/dev/null
adb -s "$DEVICE" shell monkey -p "$PACKAGE" 1 >/dev/null 2>&1 || true

echo "Recording Spotlight trace on $DEVICE for ${DURATION_SECONDS}s..."
echo "Now interact with the app: highlight intro -> feature -> clear target -> clear, repeat until recording ends."

adb -s "$DEVICE" shell "cat $REMOTE_CONFIG | perfetto --txt -c - -o $REMOTE_TRACE" >/tmp/spotlight-perfetto.log 2>&1 &
PERFETTO_PID=$!

for remaining in $(seq "$DURATION_SECONDS" -1 1); do
  printf '\rRecording... %2ss left' "$remaining"
  sleep 1
done
printf '\nWaiting for trace file...\n'
wait "$PERFETTO_PID"

adb -s "$DEVICE" pull "$REMOTE_TRACE" "$LOCAL_TRACE" >/dev/null

echo "Trace saved: $LOCAL_TRACE"
echo "Send this path/file to the assistant for analysis."
