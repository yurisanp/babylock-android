# Publicar o BabyLock na Google Play

Tudo o que dá para preparar sem a conta de desenvolvedor já está no repositório. Este guia lista o que falta, na ordem.

## O que já está pronto

| Item | Onde |
| --- | --- |
| Pacote da Play (.aab) assinado pelo CI | `.github/workflows/release.yml` (tag `v*` ou botão "Run workflow") |
| Envio automático para o teste interno | mesmo workflow, quando existir o segredo `PLAY_SERVICE_ACCOUNT_JSON` |
| Ficha da loja em português, inglês e espanhol | `fastlane/metadata/android/{pt-BR,en-US,es-419}/` |
| Novidades da versão | `distribution/whatsnew/` |
| Ícone 512, banner 1024x500 e capturas com título | `docs/marca/` (PR do visual da marca) |
| Capturas reais do app nos 3 idiomas | CI, job "Capturas para a Play", artefato `capturas-play` |
| Política de privacidade (3 idiomas) | `docs/privacidade/index.html` → GitHub Pages |
| Divulgação da acessibilidade dentro do app | Início > Acessibilidade > aviso antes das configurações |
| App em 3 idiomas, com escolha de idioma no Android 13+ | `app/src/main/res/values*/strings.xml` |

## O que precisa das suas mãos

1. **Conta de desenvolvedor** (US$ 25, uma vez): https://play.google.com/console/signup. Escolha conta pessoal e faça a verificação de identidade.
2. **Contato na política de privacidade**: troque `[E-MAIL DE CONTATO]` (e as versões em inglês e espanhol) em `docs/privacidade/index.html` por um e-mail público do app.
3. **Ligar o GitHub Pages**: Settings > Pages > Source: **GitHub Actions**. Depois rode o workflow "Site (política de privacidade)". O endereço fica `https://yurisanp.github.io/babylock-android/privacidade/`.
4. **Chave de envio (keystore)**: no seu computador, com Java instalado:
   ```bash
   keytool -genkeypair -v -keystore babylock-upload.jks -keyalg RSA -keysize 4096 \
     -validity 10000 -alias babylock-upload
   base64 -w0 babylock-upload.jks > babylock-upload.b64   # no Mac: base64 -i babylock-upload.jks
   ```
   Guarde o `.jks` e as senhas num lugar seguro (sem eles não dá para atualizar o app). Em GitHub > Settings > Secrets and variables > Actions, crie:
   `BABYLOCK_KEYSTORE_BASE64` (conteúdo do `.b64`), `BABYLOCK_KEYSTORE_PASSWORD`, `BABYLOCK_KEY_ALIAS` (`babylock-upload`), `BABYLOCK_KEY_PASSWORD`.
5. **Gerar o primeiro pacote**: GitHub > Actions > "Versão para a Play" > Run workflow. Baixe o artefato `babylock-play-N` (tem o `app-release.aab`).
6. **Criar o app no Play Console**: nome "BabyLock", idioma padrão **Português (Brasil)**, app gratuito. Aceite a **Assinatura de apps do Google Play**.
7. **Primeiro envio manual**: Teste > Teste interno > Criar versão > envie o `.aab`. (A Play não deixa a API enviar antes do primeiro envio manual.)
8. **Preencher a ficha e os formulários** com as respostas abaixo.
9. **Teste fechado obrigatório**: contas pessoais novas precisam de **12 testadores por 14 dias seguidos** num teste fechado antes de pedir produção. Chame amigas e familiares com Android (é só um e-mail do Google de cada).
10. **(Opcional) Envio automático**: no Google Cloud, crie uma conta de serviço, gere a chave JSON, convide o e-mail dela no Play Console (Usuários e permissões, permissão de "Lançar em faixas de teste") e salve o JSON no segredo `PLAY_SERVICE_ACCOUNT_JSON`. A partir daí cada tag `v*` sobe sozinha para o teste interno.

## Respostas para os formulários do Play Console

### Ficha da loja
- Textos: copie de `fastlane/metadata/android/<idioma>/` (título, descrição curta, descrição completa). Adicione as traduções em Inglês (Estados Unidos) e Espanhol (Latinoamérica).
- Ícone: `docs/marca/icone-512.png`. Gráfico de destaque: `docs/marca/banner-1024x500.png`.
- Capturas de celular: `docs/marca/tela-1..5.png` (português) e as do artefato `capturas-play` para inglês e espanhol.
- Categoria: **Ferramentas** (ou **Criação de filhos**, se aparecer). Tags: controle dos pais, bloqueio de tela.
- Política de privacidade: o endereço do GitHub Pages do passo 3.

### Público-alvo e conteúdo
- Faixa etária do público-alvo: **18 anos ou mais**. O app é usado por mães e pais; não marque faixas infantis (isso levaria ao programa Famílias, que não permite o uso de acessibilidade).
- O app pode atrair crianças sem querer? **Não**: as telas e a loja falam com o adulto.
- Anúncios: **Não** (ainda não há anúncios no app).
- Classificação de conteúdo (IARC): categoria **Utilitário/Ferramenta**, responda "Não" para violência, sexo, drogas, apostas, compartilhamento de localização e interação entre usuários. Resultado esperado: Livre.
- Acesso ao app: **todas as funções estão disponíveis sem login**.
- App de notícias / saúde / governo / financeiro: **Não**.

### Segurança dos dados
- O app coleta ou compartilha algum tipo de dado? **Não**.
- Os dados são criptografados em trânsito? Não se aplica (o app não tem internet).
- Exclusão de dados: não se aplica (nada é coletado).

### Declaração de permissões
**Serviço de acessibilidade (AccessibilityService)**
- Usa a API de acessibilidade? **Sim**. É uma ferramenta de acessibilidade (`isAccessibilityTool`)? **Não**.
- Texto da justificativa (cole em português e, se pedir em inglês, use a versão abaixo):
  > O BabyLock bloqueia toques enquanto uma criança assiste vídeos no celular dos pais. O serviço de acessibilidade é usado apenas enquanto o bloqueio está ligado para: (1) mostrar uma camada de tela cheia sobre o vídeo que absorve toques e gestos, inclusive sobre as barras do sistema; (2) segurar os botões de volume; (3) fechar a barra de notificações se a criança puxá-la. O serviço não lê o conteúdo da tela (canRetrieveWindowContent=false), não coleta dados e o app não tem acesso à internet. Antes de abrir as configurações, o app mostra uma divulgação clara e pede o consentimento do adulto.

  > BabyLock blocks touches while a child watches videos on a parent's phone. The accessibility service is used only while the lock is on, to: (1) show a full-screen layer over the video that absorbs touches and gestures, including over the system bars; (2) hold the volume buttons; (3) close the notification shade if the child pulls it down. The service does not read screen content (canRetrieveWindowContent=false), collects no data and the app has no internet access. Before opening settings, the app shows a clear disclosure and asks for the adult's consent.
- **Vídeo obrigatório** (até 1 minuto, pode ser no YouTube como "Não listado"). Grave a tela do celular mostrando, nesta ordem:
  1. O app aberto, toque em **Ligar** no cartão Acessibilidade.
  2. O aviso "Uso da acessibilidade" aparecendo e o toque em **Aceitar e ativar**.
  3. A tela do Android com o BabyLock sendo ligado.
  4. Voltar ao app, tocar em **Travar**, abrir um vídeo.
  5. Tocar na tela (nada acontece) e puxar a barra de notificações (ela fecha).
  6. Segurar o canto e destravar com a digital ou o PIN.

**Serviço em primeiro plano (FOREGROUND_SERVICE_SPECIAL_USE)**
- Usado só no modo básico, para manter a camada de bloqueio ativa enquanto o vídeo toca.
- Justificativa: "Mantém a camada que bloqueia toques ativa enquanto a criança assiste um vídeo, a pedido do adulto. Para quando o adulto destrava." Use o mesmo vídeo, mostrando o modo básico (sem acessibilidade) e a notificação "Bloqueio ativo".

## Antes de cada nova versão
1. Atualize `versionName` em `app/build.gradle.kts` (o `versionCode` é automático).
2. Escreva as novidades em `distribution/whatsnew/` (3 idiomas, até 500 caracteres).
3. Crie a tag: `git tag v1.0.1 && git push origin v1.0.1`.
