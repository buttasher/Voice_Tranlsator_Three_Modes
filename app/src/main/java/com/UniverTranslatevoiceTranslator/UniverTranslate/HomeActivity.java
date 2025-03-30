package com.UniverTranslatevoiceTranslator.UniverTranslate;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class HomeActivity extends AppCompatActivity {

    CardView cardView1, cardView2, cardView3;
    AdView adView;
    private InterstitialAd mInterstitialAd;
    private AdRequest adRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        cardView1 = findViewById(R.id.card_voiceTranslator);
        cardView2 = findViewById(R.id.card_cameraTranslate);
        cardView3 = findViewById(R.id.card_conversation);
        adView = findViewById(R.id.adView);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.WHITE); // White background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); // Dark icons
            }
        }

        // Initialize AdMob
        MobileAds.initialize(this);

        // Load banner ad
        adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        // Load interstitial ad
        loadInterstitialAd();

        // Set click listeners with interstitial ad logic
        cardView1.setOnClickListener(view -> showAdBeforeNavigation(VoiceTranslator.class));
        cardView2.setOnClickListener(view -> showAdBeforeNavigation(CameraTranslator.class));
        cardView3.setOnClickListener(view -> showAdBeforeNavigation(ConversationTranslator.class));
    }

    private void loadInterstitialAd() {
        InterstitialAd.load(this, getString(R.string.interstitial_id), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        mInterstitialAd = null; // Reset if loading fails
                    }
                });
    }

    private void showAdBeforeNavigation(Class<?> targetActivity) {
        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    // Load next ad
                    loadInterstitialAd();
                    // Navigate to next screen
                    startActivity(new Intent(HomeActivity.this, targetActivity));
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    // If ad fails, proceed directly
                    startActivity(new Intent(HomeActivity.this, targetActivity));
                }
            });
            mInterstitialAd.show(this);
        } else {
            // If ad is not loaded, proceed directly
            startActivity(new Intent(HomeActivity.this, targetActivity));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemID = item.getItemId();
        if (itemID==R.id.privacy)
        {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/document/d/14vbWdxVLGglg9gL30eBtLcwjQi4Y7RvXkLNeIEFJtZE"));
            startActivity(intent);
            return true;
        } else if (itemID==R.id.share) {
            shareApp();
            return true;
        }
        else if (itemID== R.id.rateUs){
            rateApp();
            return true;
        }
        else{
            return super.onOptionsItemSelected(item);
        }

    }

    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        // Get the app name from strings.xml
        String appName = getString(R.string.app_name);

        // Your app link (replace with actual Play Store link if available)
        String appLink = "https://play.google.com/store/apps/details?id=" + getPackageName();

        // Share message with dynamic app name
        String shareMessage = "Check out this amazing app: " + appName + "\n" + appLink;

        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
        startActivity(Intent.createChooser(shareIntent, "Share " + appName + " via"));
    }

    private void rateApp() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + getPackageName())));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }
}
