package com.dimitriazzarone.princeofpersia;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.JavascriptInterface;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import org.json.JSONObject;
import java.util.Locale;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import java.io.ByteArrayInputStream;
import java.util.Collections;

/** Local-only container for the original PrinceJS PWA. Only a local speech bridge; no file or command access. */
public final class MainActivity extends Activity {
    private static final String HOST = "appassets.androidplatform.net";
    private static final String HOME = "https://" + HOST + "/assets/index.html";
    private WebView web;
    private TextToSpeech tts;
    private boolean voiceReady;
    private boolean voiceFailed;
    private String waitingId, waitingText;
    private float waitingRate, waitingPitch, waitingVolume;
    private FrameLayout root;
    private View fullscreen;
    private WebChromeClient.CustomViewCallback fullscreenCallback;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);
        web = new WebView(this);
        web.addJavascriptInterface(new SpeechBridge(), "PrinceSpeech");
        createVoice();
        web.setBackgroundColor(Color.BLACK);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setBuiltInZoomControls(false);
        WebViewAssetLoader assets = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("https".equals(uri.getScheme()) && HOST.equals(uri.getHost())) {
                    WebResourceResponse response = assets.shouldInterceptRequest(uri);
                    if (response != null) return response;
                }
                // Everything used by the game must be packaged in the APK.
                // Data/blob resources are handled by WebView without network access.
                return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found",
                        Collections.emptyMap(), new ByteArrayInputStream(new byte[0]));
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("https".equals(uri.getScheme()) && HOST.equals(uri.getHost())) return false;
                if (request.isForMainFrame() && request.hasGesture()
                        && ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                    catch (Exception unavailable) {
                        Toast.makeText(MainActivity.this, "Nessun browser disponibile", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullscreen != null) { callback.onCustomViewHidden(); return; }
                fullscreen = view;
                fullscreenCallback = callback;
                root.addView(view, new FrameLayout.LayoutParams(-1, -1));
                web.setVisibility(View.GONE);
                immersive();
            }
            @Override public void onHideCustomView() { hideFullscreen(); }
        });
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        web.loadUrl(HOME);
        immersive();
    }
    private void createVoice() {
        tts = new TextToSpeech(this, status -> runOnUiThread(() -> {
            int language = status == TextToSpeech.SUCCESS ? tts.setLanguage(Locale.ITALIAN)
                    : TextToSpeech.LANG_NOT_SUPPORTED;
            voiceReady = language != TextToSpeech.LANG_MISSING_DATA && language != TextToSpeech.LANG_NOT_SUPPORTED;
            voiceFailed = !voiceReady;
            if (voiceReady) {
                if (tts.getVoices() != null) for (android.speech.tts.Voice voice : tts.getVoices()) {
                    if ("it".equals(voice.getLocale().getLanguage()) && !voice.isNetworkConnectionRequired()) {
                        tts.setVoice(voice); break;
                    }
                }
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) { speechEvent(id, "start"); }
                    @Override public void onDone(String id) { speechEvent(id, "end"); }
                    @Override public void onError(String id) { speechEvent(id, "error"); }
                });
            }
            if (waitingId != null) {
                String id = waitingId, text = waitingText;
                waitingId = null; waitingText = null;
                speak(id, text, waitingRate, waitingPitch, waitingVolume);
            }
        }));
    }
    private void speechEvent(String id, String event) {
        runOnUiThread(() -> {
            if (web != null) web.evaluateJavascript("window.__princeSpeechEvent&&window.__princeSpeechEvent("
                    + JSONObject.quote(id) + "," + JSONObject.quote(event) + ");", null);
        });
    }
    private void speak(String id, String text, float rate, float pitch, float volume) {
        if (voiceFailed) { speechEvent(id, "error"); return; }
        if (!voiceReady) {
            waitingId=id; waitingText=text; waitingRate=rate; waitingPitch=pitch; waitingVolume=volume;
            return;
        }
        tts.setSpeechRate(Math.max(.3f, Math.min(2f, rate)));
        tts.setPitch(Math.max(.5f, Math.min(2f, pitch)));
        Bundle params = new Bundle();
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, Math.max(0, Math.min(1, volume)));
        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, id) == TextToSpeech.ERROR) speechEvent(id, "error");
    }
    private final class SpeechBridge {
        @JavascriptInterface public void speak(String id, String text, float rate, float pitch, float volume) {
            if (id == null || !id.matches("pjs-[0-9]+") || text == null || text.length() > 3500) return;
            runOnUiThread(() -> {
                if (web != null && web.getUrl() != null && web.getUrl().startsWith("https://"+HOST+"/assets/"))
                    MainActivity.this.speak(id, text, rate, pitch, volume);
            });
        }
        @JavascriptInterface public void stop() {
            runOnUiThread(() -> {
                waitingId=null; waitingText=null;
                if (tts != null) tts.stop();
            });
        }
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
    private void hideFullscreen() {
        if (fullscreen == null) return;
        root.removeView(fullscreen);
        fullscreen = null;
        web.setVisibility(View.VISIBLE);
        if (fullscreenCallback != null) fullscreenCallback.onCustomViewHidden();
        fullscreenCallback = null;
        immersive();
    }
    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) immersive();
    }
    @Override protected void onPause() {
        if (web != null) {
            web.evaluateJavascript("window.dispatchEvent(new Event('blur'));if(window.speechSynthesis)window.speechSynthesis.pause();", null);
            if (tts != null) tts.stop();
            web.onPause();
            web.pauseTimers();
        }
        super.onPause();
    }
    @Override protected void onResume() {
        super.onResume();
        if (web != null) {
            web.onResume(); web.resumeTimers();
            web.evaluateJavascript("if(window.speechSynthesis)window.speechSynthesis.resume();", null);
        }
        immersive();
    }
    @Override public void onBackPressed() {
        if (fullscreen != null) { hideFullscreen(); return; }
        new AlertDialog.Builder(this).setTitle("Uscire dal gioco?")
                .setMessage("Adattamento Android — Creato da Dimitri Azzarone. Le opzioni di salvataggio restano quelle del gioco.")
                .setPositiveButton("Esci", (dialog, which) -> finish())
                .setNegativeButton("Continua", null).show();
    }
    @Override protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); tts=null; }
        if (web != null) {
            root.removeView(web);
            web.stopLoading();
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
