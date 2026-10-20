#!/usr/bin/env bash
# Baixa o emulador e a imagem do Android antes do android-emulator-runner,
# tentando de novo quando o download do Google chega corrompido ("Error on ZipFile unknown archive").
# Com tudo já instalado, o runner não baixa nada e não falha por isso.
# O sdkmanager imprime avisos sem importância (outras listas do Google), então o que vale é o arquivo no disco.
set -u
api="$1"
sdk="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
yes | "$sdk" --licenses > /dev/null 2>&1
install() { # pacote, arquivo que prova que instalou
  for tentativa in 1 2 3 4; do
    "$sdk" --install "$1" --channel=0 > "/tmp/sdk-$tentativa.log" 2>&1
    if [ -e "$2" ]; then
      echo "$1 instalado"
      return 0
    fi
    echo "$1 falhou na tentativa $tentativa: $(grep -iE "error|exception" "/tmp/sdk-$tentativa.log" | grep -v google_xr | head -2)"
    sleep $((tentativa * 10))
  done
  return 1
}
install emulator "$ANDROID_HOME/emulator/emulator" || exit 1
install "system-images;android-$api;google_apis;x86_64" "$ANDROID_HOME/system-images/android-$api/google_apis/x86_64/system.img" || exit 1
