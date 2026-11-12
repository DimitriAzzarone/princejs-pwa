// Android WebView compatibility for the HTML's existing Italian narration.
// The only native capability is speech, on this APK's local origin.
(() => {
  if ('speechSynthesis' in window || !window.PrinceSpeech) return;
  let queue = [], current = null, paused = false, sequence = 0;
  const voice = {name:'Android Italiano',lang:'it-IT',voiceURI:'android-it',default:true,localService:true};
  class Utterance {
    constructor(text='') {
      this.text=String(text); this.lang='it-IT'; this.rate=1; this.pitch=1; this.volume=1;
      this.voice=null; this.onstart=null; this.onend=null; this.onerror=null;
    }
  }
  const notify=(item,type) => {
    const callback=item.utterance['on'+type];
    if(typeof callback==='function') callback({type,utterance:item.utterance});
  };
  function drain() {
    if(paused || current || !queue.length) return;
    current=queue.shift();
    const u=current.utterance;
    PrinceSpeech.speak(current.id,u.text,Number(u.rate)||1,Number(u.pitch)||1,Number(u.volume)||0);
  }
  const listeners=new Map();
  const synthesis = {
    addEventListener(type,listener) {
      if(!listeners.has(type))listeners.set(type,new Set());
      listeners.get(type).add(listener);
    },
    removeEventListener(type,listener) {listeners.get(type)?.delete(listener);},
    dispatchEvent(event) {
      for(const listener of listeners.get(event.type)||[]) listener.call(this,event);
      const handler=this['on'+event.type];if(typeof handler==='function')handler.call(this,event);
      return true;
    },
    getVoices:()=>[voice],
    speak(utterance) {queue.push({id:'pjs-'+(++sequence),utterance});drain();},
    cancel() {queue=[];current=null;paused=false;PrinceSpeech.stop();},
    pause() {if(!paused){paused=true;PrinceSpeech.stop();}},
    resume() {
      if(!paused)return;
      paused=false;
      // Android TTS resumes at the start of the current sentence.
      if(current){queue.unshift(current);current=null;}
      drain();
    },
    get pending(){return queue.length>0;},
    get speaking(){return current!==null;},
    get paused(){return paused;},
    onvoiceschanged:null
  };
  window.__princeSpeechEvent=(id,type)=>{
    if(!current || current.id!==id || paused)return;
    const item=current;
    if(type==='start'){notify(item,'start');return;}
    current=null;
    notify(item,type==='end'?'end':'error');
    drain();
  };
  window.SpeechSynthesisUtterance=Utterance;
  Object.defineProperty(window,'speechSynthesis',{value:synthesis});
  setTimeout(()=>synthesis.dispatchEvent({type:'voiceschanged'}),0);
})();
