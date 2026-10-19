#!/usr/bin/env bash
# Tira as capturas da Play em cada idioma: troca o idioma do emulador inteiro
# (reinicia o Android), roda o teste PlayScreenshots e junta as imagens.
set -euo pipefail

out=app/build/play-screenshots
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

for locale in pt-BR en-US es-419; do
  echo "::group::Capturas em $locale"
  adb shell setprop persist.sys.locale "$locale"
  adb shell setprop ctl.restart zygote
  esperar_android
  echo "Idioma do aparelho: $(adb shell getprop persist.sys.locale | tr -d '\r')"
  adb shell wm dismiss-keyguard || true
  ./gradlew --no-daemon connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=app.babylock.PlayScreenshots \
    -Pandroid.testInstrumentationRunnerArguments.locale="$locale"
  echo "::endgroup::"
done

adb pull /data/local/tmp/babylock-shots/. "$out"
ls -l "$out"
