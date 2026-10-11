package com.adnova.earning;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class MainActivity extends AppCompatActivity {

    // ⚠️ আপনার ওয়েবসাইট URL
    private static final String WEBSITE_URL = "https://adnova-earning.web.app/";
    
    // AdMob Ad Unit IDs
    private static final String BANNER_AD_UNIT = "ca-app-pub-7065482959653376/9710100873";
    private static final String INTERSTITIAL_AD_UNIT = "ca-app-pub-7065482959653376/5850657623";
    private static final String REWARDED_AD_UNIT = "ca-app-pub-7065482959653376/9820772358";

    private WebView webView;
    private FrameLayout adContainerView;
    private AdView adView;
    
    private InterstitialAd mInterstitialAd;
    private RewardedAd mRewardedAd;
    
    private String pendingAdType = "";
    private String pendingReward = "";

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, initializationStatus -> {
            loadInterstitialAd();
            loadRewardedAd();
        });

        adContainerView = findViewById(R.id.adContainerView);
        adView = new AdView(this);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(BANNER_AD_UNIT);
        adContainerView.addView(adView);
        adView.loadAd(new AdRequest.Builder().build());

        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(false);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setUserAgentString(webSettings.getUserAgentString() + " AdNovaApp/1.0");

        webView.addJavascriptInterface(new AdMobBridge(), "AdMobBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectAdMobSystem();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.contains("googleads") || url.contains("doubleclick") || 
                    url.contains("profitableratecpmnetwork") || url.contains("googlesyndication")) {
                    return true;
                }
                if (!url.contains("adnova-earning.web.app") && !url.startsWith("about:")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl(WEBSITE_URL);
    }

    private void injectAdMobSystem() {
        String js = 
            "(function() {" +
            "  console.log('AdNova App - AdMob Activating...');" +
            "  if (typeof firePopunder === 'function') { window.firePopunder = function(){}; }" +
            "  var originalOpen = window.open;" +
            "  window.open = function(u,n,f){" +
            "    if(u && (u.includes('profitableratecpmnetwork')||u.includes('popunder'))) return null;" +
            "    return originalOpen.call(window,u,n,f);" +
            "  };" +
            "  window.onAdButtonClick = function() {" +
            "    AdMobBridge.showRewardedAd('plan', selectedAdPlanId, selectedAdReward);" +
            "  };" +
            "  window.onSpinAdClick = function() {" +
            "    AdMobBridge.showInterstitialAd('spin');" +
            "  };" +
            "  window.onCoinsSpinAdClick = function() {" +
            "    AdMobBridge.showInterstitialAd('coins_spin');" +
            "  };" +
            "  console.log('AdMob Activated!');" +
            "})();";
        
        webView.evaluateJavascript(js, null);
    }

    private class AdMobBridge {
        
        @JavascriptInterface
        public void showRewardedAd(String adType, String planId, String reward) {
            pendingAdType = adType;
            pendingReward = reward;
            
            runOnUiThread(() -> {
                if (mRewardedAd != null) {
                    mRewardedAd.show(MainActivity.this, rewardItem -> {
                        String js = "if (typeof claimReward === 'function') { claimReward(); }";
                        webView.evaluateJavascript(js, null);
                        Toast.makeText(MainActivity.this, 
                            "Reward Claimed!", Toast.LENGTH_SHORT).show();
                        loadRewardedAd();
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Ad loading...", Toast.LENGTH_SHORT).show();
                    loadRewardedAd();
                }
            });
        }
        
        @JavascriptInterface
        public void showInterstitialAd(String adType) {
            pendingAdType = adType;
            
            runOnUiThread(() -> {
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(MainActivity.this);
                    
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            super.onAdDismissedFullScreenContent();
                            
                            if ("spin".equals(adType)) {
                                String js = "if (typeof onSpinAdComplete === 'function') { onSpinAdComplete(); }";
                                webView.evaluateJavascript(js, null);
                            } else if ("coins_spin".equals(adType)) {
                                String js = "if (typeof onCoinsSpinAdComplete === 'function') { onCoinsSpinAdComplete(); }";
                                webView.evaluateJavascript(js, null);
                            }
                            loadInterstitialAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            super.onAdFailedToShowFullScreenContent(adError);
                            String js = "if (typeof showSpinAdState === 'function') { showSpinAdState('early'); }";
                            webView.evaluateJavascript(js, null);
                            loadInterstitialAd();
                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Ad loading...", Toast.LENGTH_SHORT).show();
                    loadInterstitialAd();
                }
            });
        }
    }

    private void loadInterstitialAd() {
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT, new AdRequest.Builder().build(),
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                }
                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    mInterstitialAd = null;
                }
            });
    }

    private void loadRewardedAd() {
        RewardedAd.load(this, REWARDED_AD_UNIT, new AdRequest.Builder().build(),
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(RewardedAd rewardedAd) {
                    mRewardedAd = rewardedAd;
                }
                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    mRewardedAd = null;
                }
            });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
