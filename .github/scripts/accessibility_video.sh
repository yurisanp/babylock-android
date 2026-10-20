#!/usr/bin/env bash
# Grava o vídeo da acessibilidade pedido pela Play, em inglês e em português:
# troca o idioma do emulador, liga o screenrecord e roda o roteiro AccessibilityVideo.
set -euo pipefail

out=app/build/accessibility-video
mkdir -p "$out"
adb root
sleep 3
adb wait-for-device

esperar_android() {
  sleep 15
  adb wait-for-device
  for _ in $(seq 1 90); do
    if [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ] \
      && adb shell pm path android > /dev/null 2>&1; then
      sleep 10
      return 0
    fi
    sleep 2
  done
  echo "O Android não voltou depois de trocar o idioma" >&2
  return 1
}

# Instala uma vez; o gradle depois só roda o teste.
./gradlew --no-daemon installDebug installDebugAndroidTest

for locale in en-US pt-BR; do
  echo "::group::Vídeo em $locale"
  adb shell setprop persist.sys.locale "$locale"
  adb shell setprop ctl.restart zygote
  esperar_android
  adb shell wm dismiss-keyguard || true
  adb shell input keyevent KEYCODE_HOME
  sleep 2

  # screenrecord para sozinho em 180 s; o roteiro leva perto de 90 s.
  adb shell "screenrecord --bit-rate 8000000 --time-limit 180 /sdcard/babylock-$locale.mp4" &
  rec=$!
  sleep 2
  status=0
  adb shell am instrument -w \
    -e class app.babylock.AccessibilityVideo \
    com.santech.babylock.test/androidx.test.runner.AndroidJUnitRunner | tee "$out/teste-$locale.txt" || status=$?
  sleep 2
  adb shell pkill -INT screenrecord || true
  wait "$rec" || true
  sleep 3
  adb pull "/sdcard/babylock-$locale.mp4" "$out/babylock-acessibilidade-$locale.mp4"
  echo "::endgroup::"
  if [ "$status" -ne 0 ] || ! grep -q "OK (1 test)" "$out/teste-$locale.txt"; then
    echo "O roteiro falhou em $locale" >&2
    exit 1
  fi
done
ls -l "$out"
