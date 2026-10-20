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
| Anúncios AdMob (banner não personalizado só na tela inicial, consentimento UMP) | `app/src/main/java/app/babylock/ads/` |
| BabyLock Pro: compra única que tira os anúncios (Play Billing) | `app/src/main/java/app/babylock/billing/Pro.kt` |
| Contato na política: yurisantp@gmail.com | `docs/privacidade/index.html` |

## O que precisa das suas mãos

1. **Conta de desenvolvedor** (US$ 25, uma vez): https://play.google.com/console/signup. Escolha conta pessoal e faça a verificação de identidade.
2. **Perfil de pagamentos**: no Play Console, Configuração > Perfil de pagamentos. Sem ele não dá para vender o Pro (o app continua gratuito).
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
6. **Criar o app no Play Console**: nome "BabyLock", idioma padrão **Português (Brasil)**, app gratuito. Aceite a **Assinatura de apps do Google Play**. E-mail de contato da ficha: `yurisantp@gmail.com`. Nome do pacote: `com.santech.babylock` (já é o do app; não dá para mudar depois do primeiro envio).
7. **Primeiro envio manual**: Teste > Teste interno > Criar versão > envie o `.aab`. (A Play não deixa a API enviar antes do primeiro envio manual.)
8. **Preencher a ficha e os formulários** com as respostas abaixo.
9. **Teste fechado obrigatório**: contas pessoais novas precisam de **12 testadores por 14 dias seguidos** num teste fechado antes de pedir produção. Chame amigas e familiares com Android (é só um e-mail do Google de cada).
10. **AdMob**: crie a conta em https://admob.google.com, adicione o app (depois que ele estiver na Play), crie um bloco **Banner** e salve os dois IDs nos segredos `ADMOB_APP_ID` (`ca-app-pub-…~…`) e `ADMOB_BANNER_ID` (`ca-app-pub-…/…`). Sem eles o release usa os IDs de teste e não ganha nada. Detalhes no roteiro da loja.
11. **BabyLock Pro**: depois do primeiro envio (o pacote já tem a biblioteca de pagamentos), vá em Monetizar > Produtos > **Produtos no app** > Criar produto. ID do produto: **`babylock_pro`** (exatamente assim, não dá para mudar depois). Nome: "BabyLock Pro, sem anúncios". Descrição: "Pague uma vez e tire os anúncios para sempre." Preço: **R$ 14,99** (deixe a Play converter para os outros países). Ative o produto. Para testar sem pagar, adicione seu e-mail em Configuração > Teste de licença: a compra aparece como "Pedido de teste".
12. **(Opcional) Envio automático**: no Google Cloud, crie uma conta de serviço, gere a chave JSON, convide o e-mail dela no Play Console (Usuários e permissões, permissão de "Lançar em faixas de teste") e salve o JSON no segredo `PLAY_SERVICE_ACCOUNT_JSON`. A partir daí cada tag `v*` sobe sozinha para o teste interno.

## Respostas para os formulários do Play Console

### Ficha da loja
- Textos: copie de `fastlane/metadata/android/<idioma>/` (título, descrição curta, descrição completa). Adicione as traduções em Inglês (Estados Unidos) e Espanhol (Latinoamérica).
- Ícone, gráfico de destaque e as 5 capturas de cada idioma: `fastlane/metadata/android/<idioma>/images/` (`icon.png`, `featureGraphic.png`, `phoneScreenshots/1..5.png`).
- Categoria: **Ferramentas** (ou **Criação de filhos**, se aparecer). Tags: controle dos pais, bloqueio de tela.
- Política de privacidade: o endereço do GitHub Pages do passo 3.

### Público-alvo e conteúdo
- Faixa etária do público-alvo: **18 anos ou mais**. O app é usado por mães e pais; não marque faixas infantis (isso levaria ao programa Famílias, que não permite o uso de acessibilidade).
- O app pode atrair crianças sem querer? **Não**: as telas e a loja falam com o adulto.
- Anúncios: **Sim, o app contém anúncios** (AdMob, só na tela inicial do adulto; somem com o BabyLock Pro).
- Compras no app: **Sim** (BabyLock Pro, compra única de R$ 14,99).
- Classificação de conteúdo (IARC): categoria **Utilitário/Ferramenta**, responda "Não" para violência, sexo, drogas, apostas, compartilhamento de localização e interação entre usuários. Resultado esperado: Livre.
- Acesso ao app: **todas as funções estão disponíveis sem login**.
- App de notícias / saúde / governo / financeiro: **Não**.

### Segurança dos dados
Os dados abaixo são coletados pelo SDK do Google AdMob (o app em si não envia nada).
- O app coleta ou compartilha dados? **Sim**.
- Os dados são criptografados em trânsito? **Sim**.
- A pessoa pode pedir para apagar os dados? **Não** (o app não guarda dados em servidor; o identificador de publicidade é zerado nas configurações do Google).
- Tipos de dados, todos **coletados e compartilhados**, **processamento obrigatório**, finalidade **Publicidade ou marketing** e **Análise** (e **Prevenção de fraudes, segurança e compliance**):
  - Local: **Local aproximado** (pelo IP).
  - Atividade no app: **Interações com o app** (visualizações e cliques nos anúncios).
  - Informações e desempenho do app: **Registros de falhas** e **Diagnóstico**.
  - Identificadores do dispositivo ou outros: **ID do dispositivo ou outros IDs** (identificador de publicidade).
- O app não coleta nome, e-mail, contatos, fotos, arquivos, mensagens nem conteúdo da tela.
- Compras: o pagamento do Pro é feito pela Google Play. Informações financeiras **não** são coletadas pelo app (a Play cuida disso e ela não entra no formulário).

### Declaração de permissões
**Serviço de acessibilidade (AccessibilityService)**
- Usa a API de acessibilidade? **Sim**. É uma ferramenta de acessibilidade (`isAccessibilityTool`)? **Não**.
- Texto da justificativa (cole em português e, se pedir em inglês, use a versão abaixo):
  > O BabyLock bloqueia toques enquanto uma criança assiste vídeos no celular dos pais. O serviço de acessibilidade é usado apenas enquanto o bloqueio está ligado para: (1) mostrar uma camada de tela cheia sobre o vídeo que absorve toques e gestos, inclusive sobre as barras do sistema; (2) segurar os botões de volume; (3) fechar a barra de notificações se a criança puxá-la. Para saber se a barra de notificações abriu, o serviço consulta só a lista de janelas (tipo e foco); ele nunca lê textos nem o conteúdo de outros apps e nada disso sai do celular. Antes de abrir as configurações, o app mostra uma divulgação clara e pede o consentimento do adulto.

  > BabyLock blocks touches while a child watches videos on a parent's phone. The accessibility service is used only while the lock is on, to: (1) show a full-screen layer over the video that absorbs touches and gestures, including over the system bars; (2) hold the volume buttons; (3) close the notification shade if the child pulls it down. To know whether the notification shade opened, the service only checks the list of windows (type and focus); it never reads text or other apps' content, and none of it leaves the phone. Before opening settings, the app shows a clear disclosure and asks for the adult's consent.
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
