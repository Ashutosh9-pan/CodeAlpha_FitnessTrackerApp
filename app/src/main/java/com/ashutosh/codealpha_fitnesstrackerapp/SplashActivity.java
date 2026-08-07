package com.ashutosh.codealpha_fitnesstrackerapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.animation.OvershootInterpolator;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.splashscreen.SplashScreen;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.AppCheckProviderFactory;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    private static final long SPLASH_DELAY = 1200;

    private static final String FIREBASE_DATABASE_URL =
            "https://fitnesstrackerapp-cb360-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        initializeFirebaseAppCheck();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        setupWindowInsets();
        initializeViews();
    }

    private void initializeFirebaseAppCheck() {
        FirebaseApp.initializeApp(this);

        FirebaseAppCheck firebaseAppCheck =
                FirebaseAppCheck.getInstance();

        if (BuildConfig.DEBUG) {
            installDebugAppCheckProvider(firebaseAppCheck);
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
            );
        }
    }

    private void installDebugAppCheckProvider(
            FirebaseAppCheck firebaseAppCheck
    ) {
        try {
            Class<?> factoryClass = Class.forName(
                    "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory"
            );

            Object factory = factoryClass
                    .getMethod("getInstance")
                    .invoke(null);

            if (!(factory instanceof AppCheckProviderFactory)) {
                throw new IllegalStateException(
                        "Invalid debug App Check provider"
                );
            }

            firebaseAppCheck.installAppCheckProviderFactory(
                    (AppCheckProviderFactory) factory
            );

        } catch (ReflectiveOperationException
                 | IllegalStateException exception) {
            Log.e(
                    TAG,
                    "Unable to initialize debug App Check; using Play Integrity",
                    exception
            );

            firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
            );
        }
    }

    private void setupWindowInsets() {
        View main = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(
                main,
                (view, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }

    private void initializeViews() {
        ImageView logo =
                findViewById(R.id.imgLogo);

        TextView title =
                findViewById(R.id.txtAppName);

        TextView tagline =
                findViewById(R.id.txtTagline);

        animateSplashContent(
                logo,
                title,
                tagline
        );

        new Handler(
                Looper.getMainLooper()
        ).postDelayed(
                this::openNextScreen,
                SPLASH_DELAY
        );
    }

    private void openNextScreen() {
        FirebaseUser currentUser =
                FirebaseAuth
                        .getInstance()
                        .getCurrentUser();

        if (currentUser == null) {
            openScreen(LoginActivity.class);
            return;
        }

        currentUser.reload().addOnCompleteListener(task ->
                checkVerificationRequirement(currentUser)
        );
    }

    private void checkVerificationRequirement(
            FirebaseUser currentUser
    ) {
        FirebaseUser refreshedUser = FirebaseAuth
                .getInstance()
                .getCurrentUser();

        if (refreshedUser == null) {
            openScreen(LoginActivity.class);
            return;
        }

        DatabaseReference verificationReference =
                FirebaseDatabase
                        .getInstance(FIREBASE_DATABASE_URL)
                        .getReference("users")
                        .child(refreshedUser.getUid())
                        .child("emailVerificationRequired");

        verificationReference.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {
                        Boolean required =
                                snapshot.getValue(Boolean.class);

                        if (Boolean.TRUE.equals(required)
                                && !refreshedUser.isEmailVerified()) {
                            FirebaseAuth.getInstance().signOut();
                            openScreen(LoginActivity.class);
                            return;
                        }

                        openScreen(MainActivity.class);
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError error
                    ) {
                        if (refreshedUser.isEmailVerified()) {
                            openScreen(MainActivity.class);
                        } else {
                            FirebaseAuth.getInstance().signOut();
                            openScreen(LoginActivity.class);
                        }
                    }
                }
        );
    }

    private void openScreen(Class<?> activityClass) {
        Intent intent = new Intent(
                SplashActivity.this,
                activityClass
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        overridePendingTransition(
                R.anim.slide_in_right,
                R.anim.slide_out_left
        );

        finish();
    }

    private void animateSplashContent(
            ImageView logo,
            TextView title,
            TextView tagline
    ) {

        // Initial State
        logo.setAlpha(0f);
        logo.setScaleX(0.5f);
        logo.setScaleY(0.5f);
        logo.setRotation(-15f);

        title.setAlpha(0f);
        title.setTranslationY(40f);

        tagline.setAlpha(0f);
        tagline.setTranslationY(40f);

        // Logo Animation
        logo.animate()
                .alpha(1f)
                .rotation(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();

        // App Name Animation
        title.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setStartDelay(250)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Tagline Animation
        tagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(700)
                .setStartDelay(450)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }
}