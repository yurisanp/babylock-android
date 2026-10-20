#!/usr/bin/env bash
# Baixa o emulador e a imagem do Android antes do android-emulator-runner,
# tentando de novo quando o download do Google chega corrompido ("Error on ZipFile unknown archive").
# Com tudo já instalado, o runner não baixa nada e não falha por isso.
set -u
api="$1"
sdk="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
yes | "$sdk" --licenses > /dev/null
for pkg in emulator "system-images;android-$api;google_apis;x86_64"; do
  for tentativa in 1 2 3 4; do
    saida=$("$sdk" --install "$pkg" --channel=0 2>&1)
    if ! grep -qiE "error occurred|unknown archive|failed to" <<< "$saida"; then
      echo "$pkg instalado"
      break
    fi
    echo "$pkg falhou na tentativa $tentativa: $(grep -iE "error occurred|unknown archive|failed to" <<< "$saida" | head -1)"
    [ "$tentativa" = 4 ] && exit 1
    sleep $((tentativa * 10))
  done
done
