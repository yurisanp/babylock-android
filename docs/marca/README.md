# Marca BabyLock

## Ideia
Um cadeado com uma lua no lugar do buraco da chave: proteção calma, hora de descanso.
O público são mães e pais com pouco tempo. A comunicação é curta, carinhosa e direta, sem termos técnicos.

**Frase principal:** Seu filho assiste. Você relaxa.

## Cores
| Nome | Hex | Uso |
|---|---|---|
| Noite | `#121833` | fundos escuros, texto sobre creme |
| Noite clara | `#2E3A6E` | botões, cadeado sobre fundo claro |
| Lilás | `#9C8CD9` | arco do cadeado, destaques |
| Creme | `#F6EFE3` | fundo claro, cadeado sobre fundo escuro |
| Menta | `#5BC8A8` | só para "protegido" e confirmações |

## Letras
- Títulos: **Bricolage Grotesque** Bold
- Textos: **Figtree** (Regular, Semibold, Bold)

As duas já estão no app, em `app/src/main/res/font` (licença OFL).

## Arquivos
| Arquivo | Para quê |
|---|---|
| `icone-512.png` | ícone da Play Store (512×512) |
| `banner-1024x500.png` | gráfico de destaque da Play Store |
| `tela-1.png` … `tela-5.png` | capturas para a Play Store em português (1080×1920) |
| `fastlane/metadata/android/<idioma>/images/` | ícone, banner e as 5 capturas em pt-BR, en-US e es-419 |
| `logo-fundo-escuro.png`, `logo-fundo-claro.png`, `logo-transparente.png` | logo com o nome |
| `icon.svg`, `marca-clara.svg`, `marca-escura.svg` | símbolo em vetor |

Para mudar um texto ou gerar de novo, edite `fonte/build.py` e siga as instruções no topo do arquivo.

O ícone do app usa o mesmo desenho, em `res/drawable/ic_launcher_*.xml`. Ele inclui uma versão monocromática para os ícones temáticos do Android 13+.

## Regras rápidas
- O verde menta só aparece para dizer "está protegido". Ele não é cor de decoração.
- Não deixe o cadeado sem a lua.
- Fale com a pessoa usando "você". Prefira frases de até 8 palavras.
