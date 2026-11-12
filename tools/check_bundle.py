"""Validate offline resources and JavaScript before building the Android package."""
from pathlib import Path
from html.parser import HTMLParser
import re,json,base64,subprocess,tempfile,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
assets=root/'android/app/src/main/assets'
html=(assets/'index.html').read_text()
class Page(HTMLParser):
 def __init__(self):super().__init__();self.urls=[];self.scripts=[];self.current=None
 def handle_starttag(self,tag,attrs):
  attrs=dict(attrs)
  if tag=='script':self.current=[]
  if tag in ['script','link','img','audio','video','source']:
   for key in ['src','href']:
    if key in attrs:self.urls.append(attrs[key])
 def handle_data(self,data):
  if self.current is not None:self.current.append(data)
 def handle_endtag(self,tag):
  if tag=='script' and self.current is not None:self.scripts.append(''.join(self.current));self.current=None
page=Page();page.feed(html)
for url in page.urls:
 if url.startswith(('data:','blob:','#')):continue
 assert not url.startswith(('http:','https:','//')), 'Unexpected online resource: '+url
 assert (assets/url.split('?')[0]).is_file(), 'Missing packaged file: '+url
raw=re.search(r'window\.__PJS_RAW\s*=\s*(\{.*?\});\s*window\.__PJS_URLS',html,re.S)
assert raw, 'Embedded game assets not found'
resources=json.loads(raw.group(1))
assert len(resources)>20
for name,resource in resources.items():
 data=base64.b64decode(resource['b'],validate=True)
 assert data, 'Empty game asset: '+name
for script in page.scripts:
 if not script.strip():continue
 with tempfile.NamedTemporaryFile(suffix='.js',mode='w') as f:
  f.write(script);f.flush();subprocess.run(['node','--check',f.name],check=True)
manifest=ET.parse(root/'android/app/src/main/AndroidManifest.xml')
assert not manifest.findall('uses-permission'), 'The offline game should not request permissions'
print('Offline asset checks and embedded JavaScript syntax passed')
print('Embedded game resources:',len(resources))
