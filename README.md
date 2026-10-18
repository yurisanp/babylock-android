# BabyLock (Android)

Protótipo do BabyLock: trava os toques na tela enquanto a criança assiste YouTube, Netflix etc., e só destrava com o adulto (digital, rosto, senha do celular ou PIN).

Plano completo: [BabyLock: pesquisa e plano](https://claude.ai/code/artifact/b57a2f4b-eeb5-40fc-9402-dda7d61e9789)

## Como funciona

| Peça | Arquivo | O que faz |
| --- | --- | --- |
| Motor principal | `lock/LockAccessibilityService.kt` | Overlay de acessibilidade em tela cheia (inclui barras do sistema), bloqueia volume e fecha a barra de notificações. Não lê a tela. |
| Motor básico | `lock/OverlayLockService.kt` | Overlay comum ("sobrepor a outros apps") num serviço em primeiro plano. Usado se a acessibilidade estiver desligada. |
| Camada de bloqueio | `lock/LockOverlayView.kt` | Engole todos os toques. Só escapa o gesto de segurar o canto superior direito com um dedo por N segundos. |
| Controle | `lock/LockController.kt` | Escolhe o motor, agenda o bloqueio (5 s para abrir o vídeo), trava, destrava, volta a travar. |
| Desbloqueio | `unlock/UnlockActivity.kt` | BiometricPrompt (biometria forte ou credencial do aparelho), PIN próprio ou conta de multiplicar. 20 s sem confirmar, Voltar ou Home = trava de novo. 5 erros = espera 30 s. |
| Atalho | `lock/LockTileService.kt` | Bloco nas Configurações rápidas. |
| Tela inicial | `ui/HomeScreen.kt` | Estado das permissões, divulgação de acessibilidade (exigida pelo Google Play), "Abrir e travar" para apps de vídeo, configurações, dicas por fabricante. |

## Compilar

O GitHub Actions compila a cada push e publica o APK de teste em **Actions > (última execução) > Artifacts > babylock-debug-apk**.

Localmente: abra a pasta no Android Studio (Ladybug ou mais novo) ou rode `./gradlew assembleDebug`. Requer JDK 17+ e Android SDK 36.

## Testes automatizados

Rodam no GitHub Actions a cada push e pull request. O resumo de cada execução mostra quantos testes passaram, e os relatórios ficam em **Artifacts**.

| Camada | Onde | O que cobre |
| --- | --- | --- |
| Unidade | `app/src/test/.../UnlockGateTest`, `PrefsTest` | Regras do PIN e da conta, limite de 5 erros + espera de 30 s, PIN guardado só com hash e sal |
| Comportamento (Robolectric) | `LockOverlayViewTest`, `LockControllerTest`, `DeviceTipsTest` | Gesto do canto (tempo, soltar antes, segundo dedo, arrastar), escolha do motor, contagem regressiva, trava/destrava, dicas por marca |
| Telas (Compose + Robolectric) | `HomeScreenTest`, `UnlockScreenTest`, `UnlockActivityTest` | Estados de permissão, divulgação da acessibilidade antes de abrir as configurações, PIN, desbloqueio certo/errado, cancelar e voltar retravam |
| Ponta a ponta (emulador API 30 e 34) | `app/src/androidTest/.../LockEndToEndTest` | Liga o serviço de acessibilidade de verdade e confere: toques não chegam ao "app de vídeo", volume travado, barra de notificações fecha, gesto + PIN destrava, cancelar e PIN errado mantêm travado, toques rápidos no canto não destravam |
| Lint e cobertura | Android Lint, Kover | Relatório HTML de lint e de cobertura |

Rodar localmente:

```bash
./gradlew testDebugUnitTest koverHtmlReportDebug   # testes na JVM + cobertura
./gradlew connectedDebugAndroidTest                # com um emulador ou celular conectado
```

O "app de vídeo" dos testes é a `TouchProbeActivity`, que existe só no build debug (`app/src/debug`).

## Testar no celular

1. Instale o APK (permita "instalar apps desconhecidos").
2. Android 13+: como o APK não veio da Play Store, o Android bloqueia a acessibilidade ("configuração restrita"). Vá em Configurações > Apps > BabyLock > menu ⋮ > **Permitir configurações restritas**, depois ligue a acessibilidade.
3. Abra o BabyLock, toque em **Abrir e travar > YouTube** e coloque um vídeo em até 5 s.
4. Para destravar: segure o canto superior direito por 3 s.

## Roteiro de teste por aparelho

Para cada aparelho (Samsung, Xiaomi, Motorola, Pixel) e cada app (YouTube, Netflix):

- [ ] Toques no vídeo são ignorados e o vídeo continua tocando
- [ ] Puxar a barra de notificações: fecha sozinha (modo completo)
- [ ] Volume + e − não mudam o som (modo completo)
- [ ] Gesto de voltar, Home e Recentes pela borda: anotar se escapam
- [ ] Bater com vários dedos no canto não desbloqueia
- [ ] Desbloqueio com digital/rosto funciona; cancelar volta a travar
- [ ] Apagar e acender a tela: continua travado
- [ ] Bloqueio dura 30 min sem o sistema matar o serviço

## Limitações conhecidas do protótipo

- O botão liga/desliga não pode ser bloqueado por nenhum app.
- Em navegação por gestos, alguns aparelhos tratam as bordas antes do overlay. O Modo Fortaleza (fixação de tela) resolve; o guia dentro do app vem na próxima etapa.
- No Android 11 ou anterior, fechar a barra usa "voltar", que pode afetar o app de vídeo em casos raros.
- Ainda sem anúncios, compra Pro, timer e Não perturbe: entram na Fase 2 do plano.
