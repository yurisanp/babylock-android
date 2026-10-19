"""Gera as artes da loja (ícone 512, banner e capturas) em pt-BR, en-US e es-419.

Uso (precisa de Python 3 e do Playwright para Node com Chromium):
    python3 docs/marca/fonte/build.py
    NODE_PATH=$(npm root -g) node docs/marca/fonte/shot.cjs "$(cat /tmp/babylock-marca/jobs.json)"

As páginas HTML intermediárias ficam em /tmp/babylock-marca (ou em $MARCA_TMP).
"""
import json
import os
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
from geo import lock, sparkle  # noqa: E402

RAIZ = os.path.abspath(os.path.join(AQUI, '..', '..', '..'))
FONTES = os.path.join(RAIZ, 'app', 'src', 'main', 'res', 'font')
MARCA = os.path.join(RAIZ, 'docs', 'marca')
FASTLANE = os.path.join(RAIZ, 'fastlane', 'metadata', 'android')
TMP = os.environ.get('MARCA_TMP', '/tmp/babylock-marca')
os.makedirs(TMP, exist_ok=True)

# ---------- textos ----------
T = {
 'pt-BR': dict(
  shots=[
   ('PROTEÇÃO DE TOQUE', 'Seu filho assiste.<br>O toque não faz nada.', 'O vídeo continua. Ninguém pausa, fecha ou liga para alguém.'),
   ('FÁCIL DE USAR', 'Um toque e<br>o celular está travado', 'Abra o YouTube, Netflix ou Disney+ direto pelo app.'),
   ('SEM PRESSA', 'Dá tempo de<br>abrir o vídeo', 'Você toca em Travar e tem 5 segundos para dar o play.'),
   ('SÓ VOCÊ DESTRAVA', 'Criança não<br>consegue sair', 'Segure o canto por 3 segundos e confirme com a digital ou o PIN.'),
   ('PRIVACIDADE', 'Sua privacidade<br>em primeiro lugar', 'O BabyLock só cuida do toque. O resto é seu.'),
  ],
  pill='Toques bloqueados',
  checks=[('Não lê sua tela', 'Só bloqueia os toques'), ('Não pede conta', 'Nem e-mail, nem cadastro'),
          ('Trava mesmo sem internet', 'O bloqueio não precisa de rede'), ('Anúncio só na sua tela', 'Nunca aparece no vídeo'), ('Pague uma vez, sem mensalidade', 'O Pro tira os anúncios')],
  locking='TRAVANDO', open_video='Abra o vídeo agora', auto='A tela trava sozinha.<br>O vídeo continua tocando.', cancel='Cancelar',
  confirm='Confirme que é você', sensor='Toque no sensor de digital', pin='Usar PIN',
  home={},
  feat=('Seu filho assiste.', 'Você relaxa.', 'Trava os toques da tela enquanto o vídeo continua tocando.'),
 ),
 'en-US': dict(
  shots=[
   ('TOUCH PROTECTION', 'Your child watches.<br>Touches do nothing.', 'The video keeps playing. No pausing, closing or calling anyone.'),
   ('EASY TO USE', 'One tap and<br>the phone is locked', 'Open YouTube, Netflix or Disney+ right from the app.'),
   ('NO RUSH', 'Time to<br>open the video', 'Tap Lock and you have 5 seconds to press play.'),
   ('ONLY YOU UNLOCK', 'Kids can’t<br>get out', 'Hold the corner for 3 seconds, then use your fingerprint or PIN.'),
   ('PRIVACY', 'Your privacy<br>comes first', 'BabyLock only handles touches. The rest is yours.'),
  ],
  pill='Touches blocked',
  checks=[('Doesn’t read your screen', 'It only blocks touches'), ('No account needed', 'No email, no sign-up'),
          ('Locks even offline', 'Locking needs no internet'), ('Ads only on your screen', 'Never on the video'), ('Pay once, no subscription', 'Pro removes the ads')],
  locking='LOCKING', open_video='Open the video now', auto='The screen locks by itself.<br>The video keeps playing.', cancel='Cancel',
  confirm='Confirm it’s you', sensor='Touch the fingerprint sensor', pin='Use PIN',
  home={'aria-label="Configurações"': 'aria-label="Settings"', 'Proteção completa': 'Full protection',
        'toque, barra e volume': 'touch, bar and volume', '>Travar<': '>Lock<', 'em 5 segundos': 'in 5 seconds',
        'Toque, abra o vídeo e entregue o celular.': 'Tap, open the video and hand over the phone.',
        'Abrir e travar': 'Open and lock', 'abre o app e trava sozinho': 'opens the app and locks itself',
        'Para destravar': 'To unlock',
        'Segure o canto de cima, à direita, por 3 s. Depois confirme com a digital ou o PIN.':
        'Hold the top right corner for 3 s. Then confirm with your fingerprint or PIN.'},
  feat=('Your child watches.', 'You relax.', 'Locks screen touches while the video keeps playing.'),
 ),
 'es-419': dict(
  shots=[
   ('PROTECCIÓN TÁCTIL', 'Tu hijo mira.<br>Los toques no hacen nada.', 'El video sigue. Nadie pausa, cierra ni llama a nadie.'),
   ('FÁCIL DE USAR', 'Un toque y el celular<br>queda bloqueado', 'Abre YouTube, Netflix o Disney+ directo desde la app.'),
   ('SIN PRISA', 'Tiempo para<br>abrir el video', 'Tocas Bloquear y tienes 5 segundos para darle play.'),
   ('SOLO TÚ DESBLOQUEAS', 'Los niños no<br>pueden salir', 'Mantén la esquina 3 segundos y confirma con la huella o el PIN.'),
   ('PRIVACIDAD', 'Tu privacidad<br>es lo primero', 'BabyLock solo se ocupa del toque. Lo demás es tuyo.'),
  ],
  pill='Toques bloqueados',
  checks=[('No lee tu pantalla', 'Solo bloquea los toques'), ('No pide cuenta', 'Ni correo, ni registro'),
          ('Bloquea incluso sin internet', 'El bloqueo no necesita red'), ('Anuncios solo en tu pantalla', 'Nunca sobre el video'), ('Paga una vez, sin mensualidad', 'Pro quita los anuncios')],
  locking='BLOQUEANDO', open_video='Abre el video ahora', auto='La pantalla se bloquea sola.<br>El video sigue.', cancel='Cancelar',
  confirm='Confirma que eres tú', sensor='Toca el sensor de huella', pin='Usar PIN',
  home={'aria-label="Configurações"': 'aria-label="Ajustes"', 'Proteção completa': 'Protección completa',
        'toque, barra e volume': 'toque, barra y volumen', '>Travar<': '>Bloquear<', 'em 5 segundos': 'en 5 segundos',
        'Toque, abra o vídeo e entregue o celular.': 'Toca, abre el video y entrega el celular.',
        'Abrir e travar': 'Abrir y bloquear', 'abre o app e trava sozinho': 'abre la app y se bloquea',
        'Para destravar': 'Para desbloquear',
        'Segure o canto de cima, à direita, por 3 s. Depois confirme com a digital ou o PIN.':
        'Mantén la esquina superior derecha 3 s. Luego confirma con la huella o el PIN.'},
  feat=('Tu hijo mira.', 'Tú te relajas.', 'Bloquea los toques de la pantalla mientras el video sigue.'),
 ),
}

# ---------- símbolo ----------
G = lock(w=44, h=48)
SP1, SP2 = sparkle(74.5, 32, 5.6), sparkle(32.5, 39, 3.2)
DEFS = ('<radialGradient id="gN" cx="35%" cy="25%" r="90%"><stop offset="0" stop-color="#3A4785"/>'
        '<stop offset=".55" stop-color="#232C5A"/><stop offset="1" stop-color="#121833"/></radialGradient>')


def mark_g(body='#F6EFE3', shackle='#9C8CD9', sp='#5BC8A8', sp2='#F6EFE3', halo=True):
    h = '<circle cx="54" cy="56" r="31" fill="#9C8CD9" opacity=".16"/>' if halo else ''
    return (f'{h}<path d="{G["shackle"]}" fill="none" stroke="{shackle}" stroke-width="{G["stroke"]:.2f}" stroke-linecap="round"/>'
            f'<path d="{G["body"]} {G["moon"]}" fill="{body}" fill-rule="evenodd"/>'
            f'<path d="{SP1}" fill="{sp}"/><path d="{SP2}" fill="{sp2}"/>')


def icon_svg(size=None):
    sz = f'width="{size}" height="{size}"' if size else ''
    return (f'<svg xmlns="http://www.w3.org/2000/svg" {sz} viewBox="18 18 72 72"><defs>{DEFS}</defs>'
            f'<rect width="108" height="108" fill="url(#gN)"/>{mark_g()}</svg>')


def mark_svg(size=None, **kw):
    sz = f'width="{size}" height="{size}"' if size else ''
    return f'<svg xmlns="http://www.w3.org/2000/svg" {sz} viewBox="28 26 52 54">{mark_g(halo=False, **kw)}</svg>'


def wordmark(dark=True, scale=1):
    c1, c2 = ('#F6EFE3', '#B9ADF0') if dark else ('#121833', '#6E5FC0')
    m = mark_svg(int(64 * scale), body=c1 if dark else '#2E3A6E', sp2='#F6EFE3' if dark else '#9C8CD9')
    return (f'<div style="display:flex;align-items:center;gap:{14 * scale}px">{m}'
            f'<span class="d" style="font-size:{56 * scale}px;color:{c1};line-height:1">Baby<span style="color:{c2}">Lock</span></span></div>')


FONTS = open(os.path.join(AQUI, 'fonts.css')).read().replace('../../../app/src/main/res/font/', 'file://' + FONTES + '/')
HEAD = ('<!doctype html><meta charset="utf-8"><style>' + FONTS +
        'body{margin:0}*{box-sizing:border-box}.d{font-family:"Bricolage Grotesque";font-weight:700;letter-spacing:-.02em}'
        '.b{font-family:Figtree}*,*::before,*::after{animation-play-state:paused!important;animation-delay:-10s!important;transition:none!important}</style>')


def page(name, body):
    path = os.path.join(TMP, name + '.html')
    open(path, 'w').write(HEAD + body)
    return path


# ---------- telas (390x844) ----------
def video_scene(dim=0):
    return f'''<div style="position:absolute;inset:0;background:linear-gradient(#8FD3F4,#CDEFE6 70%);overflow:hidden">
<div style="position:absolute;left:46px;top:120px;width:120px;height:120px;border-radius:50%;background:#FFD66B;box-shadow:0 0 0 22px rgba(255,214,107,.25)"></div>
<div style="position:absolute;left:200px;top:180px;width:150px;height:46px;border-radius:30px;background:#fff;opacity:.9"></div>
<div style="position:absolute;left:228px;top:160px;width:70px;height:56px;border-radius:30px;background:#fff;opacity:.9"></div>
<div style="position:absolute;left:-80px;top:520px;width:420px;height:420px;border-radius:50%;background:#6CC79F"></div>
<div style="position:absolute;left:170px;top:470px;width:420px;height:420px;border-radius:50%;background:#4FB08A"></div>
<div style="position:absolute;left:120px;top:400px;width:150px;height:170px;border-radius:75px 75px 60px 60px;background:#F7A9A0"></div>
<div style="position:absolute;left:140px;top:340px;width:34px;height:90px;border-radius:17px;background:#F7A9A0;transform:rotate(-12deg)"></div>
<div style="position:absolute;left:214px;top:340px;width:34px;height:90px;border-radius:17px;background:#F7A9A0;transform:rotate(12deg)"></div>
<div style="position:absolute;left:162px;top:462px;width:14px;height:18px;border-radius:7px;background:#3B2A33"></div>
<div style="position:absolute;left:214px;top:462px;width:14px;height:18px;border-radius:7px;background:#3B2A33"></div>
<div style="position:absolute;left:180px;top:494px;width:30px;height:16px;border-radius:0 0 16px 16px;background:#B5525B"></div>
<div style="position:absolute;left:146px;top:488px;width:22px;height:12px;border-radius:50%;background:#F48E8E"></div>
<div style="position:absolute;left:222px;top:488px;width:22px;height:12px;border-radius:50%;background:#F48E8E"></div>
<div style="position:absolute;left:0;right:0;bottom:0;height:120px;background:linear-gradient(transparent,rgba(0,0,0,.35))"></div>
<div style="position:absolute;left:20px;right:20px;bottom:40px;height:5px;border-radius:3px;background:rgba(255,255,255,.35)"><div style="width:42%;height:100%;border-radius:3px;background:#fff"></div></div>
<div style="position:absolute;inset:0;background:rgba(18,24,51,{dim})"></div></div>'''


def tap(x, y):
    return (f'<div style="position:absolute;left:{x - 30}px;top:{y - 30}px;width:60px;height:60px;border-radius:50%;'
            'border:3px solid rgba(255,255,255,.85);background:rgba(255,255,255,.18)">'
            '<svg width="54" height="54" viewBox="0 0 54 54" style="position:absolute;left:0;top:0">'
            '<path d="M19 19l16 16M35 19L19 35" stroke="#fff" stroke-width="4" stroke-linecap="round"/></svg></div>')


def s_locked(t):
    pill = (f'<div class="b" style="position:absolute;left:50%;top:58px;transform:translateX(-50%);display:flex;align-items:center;'
            'gap:8px;padding:9px 16px 9px 12px;border-radius:30px;background:rgba(14,42,34,.82);color:#BDEBDC;font-weight:600;'
            'font-size:15px;white-space:nowrap"><span style="width:10px;height:10px;border-radius:5px;background:#5BC8A8;'
            f'box-shadow:0 0 0 4px rgba(91,200,168,.3)"></span>{t["pill"]}</div>')
    return f'<div style="position:relative;width:390px;height:844px;overflow:hidden">{video_scene()}{pill}{tap(110, 300)}{tap(290, 640)}{tap(250, 380)}</div>'


def s_home(t):
    h = open(os.path.join(AQUI, 'inicio.html')).read()
    for a, b in t['home'].items():
        assert a in h, a
        h = h.replace(a, b)
    return h


def s_count(t):
    return f'''<div class="b" style="width:390px;height:844px;background:#121833;color:#F6EFE3;display:flex;flex-direction:column;align-items:center;padding:72px 28px 40px">
<span style="letter-spacing:.2em;font-size:13px;font-weight:600;color:#9DA3C4">{t["locking"]}</span>
<h1 class="d" style="margin:10px 0 0;font-size:32px;text-align:center">{t["open_video"]}</h1>
<div style="position:relative;width:250px;height:250px;margin-top:70px">
<svg width="250" height="250" viewBox="0 0 250 250"><circle cx="125" cy="125" r="112" fill="none" stroke="#2E3A6E" stroke-width="14"/><circle cx="125" cy="125" r="112" fill="none" stroke="#5BC8A8" stroke-width="14" stroke-linecap="round" stroke-dasharray="704" stroke-dashoffset="282" transform="rotate(-90 125 125)"/></svg>
<span class="d" style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;font-size:120px;line-height:1">3</span></div>
<p style="margin:56px 0 0;font-size:17px;color:#C9CCE0;text-align:center;line-height:1.45">{t["auto"]}</p>
<div style="flex:1"></div>
<div style="width:100%;height:56px;border-radius:28px;border:2px solid #2E3A6E;display:flex;align-items:center;justify-content:center;font-weight:600;font-size:17px">{t["cancel"]}</div></div>'''


FP = ('<svg width="54" height="54" viewBox="0 0 24 24" fill="none" stroke="#F6EFE3" stroke-width="1.7" stroke-linecap="round">'
      '<path d="M6.5 7.5a7 7 0 0 1 11 0"/><path d="M5 12a7 7 0 0 1 14 0c0 2-.3 3.6-.9 5"/>'
      '<path d="M8.5 19.5A10 10 0 0 0 9 12a3 3 0 0 1 6 0c0 3-.5 5.4-1.6 7.5"/><path d="M12 12c0 3.3-.6 6-1.9 8.4"/></svg>')


def s_unlock(t):
    return f'''<div style="position:relative;width:390px;height:844px;overflow:hidden">{video_scene(.45)}
<svg width="96" height="96" viewBox="0 0 96 96" style="position:absolute;right:-8px;top:-8px"><circle cx="48" cy="48" r="34" fill="none" stroke="rgba(255,255,255,.3)" stroke-width="7"/><circle cx="48" cy="48" r="34" fill="none" stroke="#5BC8A8" stroke-width="7" stroke-linecap="round" stroke-dasharray="214" transform="rotate(-90 48 48)"/><circle cx="48" cy="48" r="16" fill="#F6EFE3"/></svg>
<div class="b" style="position:absolute;left:0;right:0;bottom:0;height:470px;border-radius:32px 32px 0 0;background:#FBF7F0;display:flex;flex-direction:column;align-items:center;padding:16px 28px 0">
<span style="width:44px;height:5px;border-radius:3px;background:#D9D0C2"></span>
<h2 class="d" style="margin:28px 0 0;font-size:28px;color:#121833">{t["confirm"]}</h2>
<div style="margin-top:34px;width:150px;height:150px;border-radius:50%;background:#E6E0F7;display:flex;align-items:center;justify-content:center"><div style="width:110px;height:110px;border-radius:50%;background:#2E3A6E;display:flex;align-items:center;justify-content:center">{FP}</div></div>
<p style="margin:22px 0 0;font-size:16px;color:#4A4D5A">{t["sensor"]}</p>
<span style="margin-top:22px;font-weight:700;font-size:17px;color:#2E3A6E">{t["pin"]}</span></div></div>'''


def check(title, sub):
    return f'''<div style="display:flex;gap:16px;align-items:center;padding:18px;border-radius:24px;background:#1C2447">
<span style="width:44px;height:44px;flex-shrink:0;border-radius:14px;background:#0E2A22;display:flex;align-items:center;justify-content:center"><svg width="22" height="22" viewBox="0 0 22 22"><path d="M5 11.5l4 4 8-9" fill="none" stroke="#5BC8A8" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/></svg></span>
<span style="display:flex;flex-direction:column;gap:3px"><b style="font-size:18px">{title}</b><span style="font-size:15px;color:#9DA3C4">{sub}</span></span></div>'''


def s_priv(t):
    return (f'<div class="b" style="width:390px;height:844px;background:#121833;color:#F6EFE3;padding:62px 22px 0;display:flex;flex-direction:column;gap:12px">'
            f'<div style="align-self:center;margin-bottom:10px">{mark_svg(100)}</div>{"".join(check(*c) for c in t["checks"])}</div>')


THEMES = {'night': ('#121833', '#F6EFE3', '#9DA3C4', '#5BC8A8', '#0E2A22'),
          'cream': ('#F6EFE3', '#121833', '#4A4D5A', '#2E3A6E', '#E3DEF6'),
          'lilac': ('#9C8CD9', '#121833', '#2B2558', '#121833', '#C9BEF5')}


def shot(name, theme, tag, title, sub, screen):
    bg, fg, sb, tagfg, tagbg = THEMES[theme]
    deco = ''
    if theme == 'night':
        deco = (f'<svg width="540" height="960" style="position:absolute;inset:0"><path d="{sparkle(470, 90, 14)}" fill="#5BC8A8"/>'
                '<circle cx="34" cy="320" r="3" fill="#9C8CD9"/><circle cx="500" cy="300" r="2.5" fill="#F6EFE3" opacity=".6"/>'
                '<circle cx="40" cy="70" r="2" fill="#F6EFE3" opacity=".5"/></svg>')
    glow = ('<div style="position:absolute;left:50%;top:560px;width:620px;height:620px;margin-left:-310px;border-radius:50%;'
            'background:radial-gradient(rgba(156,140,217,.35),transparent 65%)"></div>')
    size = 44 if max(len(x) for x in title.split('<br>')) <= 22 else 38
    return page(name, f'''<div style="position:relative;width:540px;height:960px;overflow:hidden;background:{bg}">{glow}{deco}
<div style="position:relative;padding:58px 30px 0;text-align:center">
<span class="b" style="display:inline-block;padding:7px 16px;border-radius:20px;background:{tagbg};color:{tagfg};font-weight:700;font-size:15px;letter-spacing:.04em">{tag}</span>
<h1 class="d" style="margin:18px 0 0;font-size:{size}px;line-height:1.06;color:{fg}">{title}</h1>
<p class="b" style="margin:14px 0 0;font-size:19px;line-height:1.35;color:{sb};text-wrap:balance">{sub}</p></div>
<div style="position:absolute;left:50%;top:350px;margin-left:-175px;width:350px;height:740px;border-radius:52px;background:#0A0E20;padding:10px;box-shadow:0 40px 80px -20px rgba(10,14,32,.6)">
<div style="width:330px;height:720px;border-radius:43px;overflow:hidden;position:relative">
<div style="transform:scale({330 / 390});transform-origin:0 0;width:390px;height:844px">{screen}</div>
<div style="position:absolute;left:50%;top:12px;margin-left:-11px;width:22px;height:22px;border-radius:50%;background:#0A0E20"></div></div></div></div>''')


def feature(name, t):
    a, b, c = t['feat']
    return page(name, f'''<div style="position:relative;width:1024px;height:500px;overflow:hidden;background:radial-gradient(120% 140% at 20% 10%,#3A4785,#232C5A 50%,#121833)">
<svg width="1024" height="500" style="position:absolute;inset:0"><path d="{sparkle(560, 70, 16)}" fill="#5BC8A8"/><path d="{sparkle(960, 430, 10)}" fill="#F6EFE3" opacity=".8"/><circle cx="90" cy="440" r="3" fill="#9C8CD9"/><circle cx="520" cy="430" r="2.5" fill="#F6EFE3" opacity=".6"/><circle cx="640" cy="40" r="2" fill="#F6EFE3" opacity=".5"/></svg>
<div style="position:absolute;left:72px;top:0;bottom:0;display:flex;flex-direction:column;justify-content:center;gap:22px;width:520px">
{wordmark(True, 1.1)}
<h1 class="d" style="margin:6px 0 0;font-size:50px;line-height:1.04;color:#F6EFE3">{a}<br><span style="color:#B9ADF0">{b}</span></h1>
<p class="b" style="margin:0;font-size:21px;line-height:1.35;color:#C9CCE0;max-width:470px">{c}</p></div>
<div style="position:absolute;left:700px;top:56px;width:270px;height:580px;border-radius:46px;background:#0A0E20;padding:9px;transform:rotate(6deg);box-shadow:0 40px 80px -10px rgba(0,0,0,.6)">
<div style="width:252px;height:562px;border-radius:38px;overflow:hidden;position:relative"><div style="transform:scale({252 / 390});transform-origin:0 0;width:390px;height:844px">{s_locked(t)}</div></div></div></div>''')


jobs = []
open(os.path.join(MARCA, 'icon.svg'), 'w').write(icon_svg())
open(os.path.join(MARCA, 'marca-clara.svg'), 'w').write(mark_svg(body='#F6EFE3'))
open(os.path.join(MARCA, 'marca-escura.svg'), 'w').write(mark_svg(body='#2E3A6E', sp2='#9C8CD9'))
jobs.append([page('icon512', f'<div style="width:512px;height:512px">{icon_svg(512)}</div>'), os.path.join(MARCA, 'icone-512.png'), 512, 512, 1])
for nome, dark, bg in [('logo-fundo-escuro', True, '#121833'), ('logo-fundo-claro', False, '#F6EFE3'), ('logo-transparente', False, 'transparent')]:
    p = page(nome, f'<div style="width:760px;height:260px;background:{bg};display:flex;align-items:center;justify-content:center">{wordmark(dark, 1.6)}</div>')
    jobs.append([p, os.path.join(MARCA, nome + '.png'), 760, 260, 2])

TEMAS = ['night', 'cream', 'lilac', 'cream', 'night']
TELAS = [s_locked, s_home, s_count, s_unlock, s_priv]
for lang, t in T.items():
    imgs = os.path.join(FASTLANE, lang, 'images')
    os.makedirs(os.path.join(imgs, 'phoneScreenshots'), exist_ok=True)
    jobs.append([feature(f'feature-{lang}', t), os.path.join(imgs, 'featureGraphic.png'), 1024, 500, 1])
    jobs.append([jobs[0][0], os.path.join(imgs, 'icon.png'), 512, 512, 1])
    for i, (tag, title, sub) in enumerate(t['shots']):
        p = shot(f'tela-{lang}-{i + 1}', TEMAS[i], tag, title, sub, TELAS[i](t))
        jobs.append([p, os.path.join(imgs, 'phoneScreenshots', f'{i + 1}.png'), 540, 960, 2])
        if lang == 'pt-BR':
            jobs.append([p, os.path.join(MARCA, f'tela-{i + 1}.png'), 540, 960, 2])
    if lang == 'pt-BR':
        jobs.append([os.path.join(TMP, f'feature-{lang}.html'), os.path.join(MARCA, 'banner-1024x500.png'), 1024, 500, 1])

open(os.path.join(TMP, 'jobs.json'), 'w').write(json.dumps(jobs))
print(f'{len(jobs)} imagens; rode shot.cjs com {TMP}/jobs.json')
