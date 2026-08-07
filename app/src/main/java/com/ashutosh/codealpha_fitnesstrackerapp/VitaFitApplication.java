package com.ashutosh.codealpha_fitnesstrackerapp;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.AppCheckProviderFactory;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

import java.lang.reflect.Method;

public class VitaFitApplication extends Application {

    private static final String TAG = "VitaFitApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        FirebaseAppCheck firebaseAppCheck =
                FirebaseAppCheck.getInstance();

        if (BuildConfig.DEBUG) {
            installDebugAppCheck(firebaseAppCheck);
        } else {
            // Signed release build
            firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
            );

            Log.d(TAG, "Play Integrity App Check installed");
        }
    }

    private void installDebugAppCheck(FirebaseAppCheck firebaseAppCheck) {
        try {
            /*
             * Debug provider is loaded using reflection.
             * This prevents release-build compilation errors because
             * firebase-appcheck-debug is only available in debug builds.
             */
            Class<?> providerClass = Class.forName(
                    "com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory"
            );

            Method getInstanceMethod =
                    providerClass.getMethod("getInstance");

            Object providerObject =
                    getInstanceMethod.invoke(null);

            if (providerObject instanceof AppCheckProviderFactory) {
                firebaseAppCheck.installAppCheckProviderFactory(
                        (AppCheckProviderFactory) providerObject
                );

                Log.d(TAG, "Debug App Check provider installed");
            } else {
                installPlayIntegrity(firebaseAppCheck);
            }

        } catch (Exception exception) {
            Log.e(
                    TAG,
                    "Debug App Check provider could not be installed",
                    exception
            );

            installPlayIntegrity(firebaseAppCheck);
        }
    }

    private void installPlayIntegrity(FirebaseAppCheck firebaseAppCheck) {
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
        );

        Log.d(TAG, "Play Integrity App Check installed as fallback");
    }
}