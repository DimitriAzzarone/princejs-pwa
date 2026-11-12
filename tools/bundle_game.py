"""Bundle the original PWA locally; only web-only SW registration is removed."""
from pathlib import Path
import re,shutil,json,hashlib,base64
root=Path(__file__).resolve().parents[1]
assets=root/'android/app/src/main/assets'
assets.mkdir(parents=True,exist_ok=True)
original=(root/'index.html').read_text()
pattern=r'<script>\s*if \("serviceWorker" in navigator\) \{\s*window\.addEventListener\("load", \(\) => \{\s*navigator\.serviceWorker\.register\("\./service-worker\.js"\)\.catch\(\(\) => \{\}\);\s*\}\);\s*\}\s*</script>'
html,count=re.subn(pattern,'',original)
assert count==1, 'Review changed PWA service-worker registration before bundling'
assert 'navigator.serviceWorker.register' not in html
html=html.replace('<script', '<script src="android-speech.js"></script><script', 1)
shutil.copyfile(root/'android/app/src/main/game-support/android-speech.js',assets/'android-speech.js')
# Use the original title image already supplied inside the user's HTML.
raw=re.search(r'window\.__PJS_RAW\s*=\s*(\{.*?\});\s*window\.__PJS_URLS',original,re.S)
resources=json.loads(raw.group(1))
(assets/'prince-title-atlas.png').write_bytes(base64.b64decode(resources['assets/gfx/title.png']['b']))
shutil.copyfile(root/'android/app/src/main/game-support/prince-branding.css',assets/'prince-branding.css')
assert html.count('<div class="family-card"><h1>')==1
html=html.replace('<div class="family-card"><h1>', '<div class="family-card"><div class="android-prince-logo" role="img" aria-label="Prince of Persia"></div><p class="android-author"><span>Adattamento Android</span><br>Creato da Dimitri Azzarone</p><h1>',1)
html=html.replace('</head>','<link rel="stylesheet" href="prince-branding.css"></head>',1)
(assets/'index.html').write_text(html)
for name in ['icon-192.png','icon-512.png','apple-touch-icon.png','favicon-32.png']:
 shutil.copyfile(root/name,assets/name)
(assets/'manifest.webmanifest').write_text(json.dumps({
 'name':'Prince of Persia','short_name':'Prince','start_url':'./index.html',
 'display':'fullscreen','orientation':'landscape','background_color':'#000000',
 'theme_color':'#1b1f2b','icons':[{'src':'icon-192.png','sizes':'192x192','type':'image/png'},
 {'src':'icon-512.png','sizes':'512x512','type':'image/png'}]}))
print('Bundled original HTML, embedded game assets, icons and manifest')
print('Original HTML SHA-256:',hashlib.sha256((root/'index.html').read_bytes()).hexdigest())
