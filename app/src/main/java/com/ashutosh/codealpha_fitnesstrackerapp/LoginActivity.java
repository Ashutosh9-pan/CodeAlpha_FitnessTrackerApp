package com.ashutosh.codealpha_fitnesstrackerapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import utils.ThemeManager;

public class LoginActivity extends AppCompatActivity {

    private static final String FIREBASE_DATABASE_URL =
            "https://fitnesstrackerapp-cb360-default-rtdb.asia-southeast1.firebasedatabase.app";

    private TextInputLayout layoutLoginEmail;
    private TextInputLayout layoutLoginPassword;

    private TextInputEditText etLoginEmail;
    private TextInputEditText etLoginPassword;

    private MaterialButton btnLogin;

    private TextView txtForgotPassword;
    private TextView txtResendVerification;
    private TextView txtOpenSignup;

    private ProgressBar progressLogin;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference usersReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initialiseViews();

        initialiseFirebase();

        setupClickListeners();
        setupKeyboardAction();
    }

    private void initialiseViews() {
        layoutLoginEmail =
                findViewById(R.id.layoutLoginEmail);

        layoutLoginPassword =
                findViewById(R.id.layoutLoginPassword);

        etLoginEmail =
                findViewById(R.id.etLoginEmail);

        etLoginPassword =
                findViewById(R.id.etLoginPassword);

        btnLogin =
                findViewById(R.id.btnLogin);

        txtForgotPassword =
                findViewById(R.id.txtForgotPassword);

        txtResendVerification =
                findViewById(R.id.txtResendVerification);

        txtOpenSignup =
                findViewById(R.id.txtOpenSignup);

        progressLogin =
                findViewById(R.id.progressLogin);
    }

    private void initialiseFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();

        usersReference = FirebaseDatabase
                .getInstance(FIREBASE_DATABASE_URL)
                .getReference("users");
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(
                view -> loginUser()
        );

        txtOpenSignup.setOnClickListener(
                view -> openSignupScreen()
        );

        txtForgotPassword.setOnClickListener(
                view -> sendPasswordResetEmail()
        );

        txtResendVerification.setOnClickListener(
                view -> resendVerificationEmail()
        );
    }

    private void setupKeyboardAction() {
        etLoginPassword.setOnEditorActionListener(
                (textView, actionId, event) -> {

                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        loginUser();
                        return true;
                    }

                    return false;
                }
        );
    }

    private void loginUser() {
        clearErrors();

        String email =
                getText(etLoginEmail);

        String password =
                getText(etLoginPassword);

        if (TextUtils.isEmpty(email)) {
            layoutLoginEmail.setError(
                    "Enter your email address"
            );

            etLoginEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            layoutLoginEmail.setError(
                    "Enter a valid email address"
            );

            etLoginEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            layoutLoginPassword.setError(
                    "Enter your password"
            );

            etLoginPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            layoutLoginPassword.setError(
                    "Password must contain at least 6 characters"
            );

            etLoginPassword.requestFocus();
            return;
        }

        setLoadingState(true);

        firebaseAuth
                .signInWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        FirebaseUser currentUser =
                                firebaseAuth.getCurrentUser();

                        if (currentUser == null) {
                            setLoadingState(false);
                            showMessage("Unable to load your account");
                            return;
                        }

                        checkEmailVerification(currentUser);

                    } else {
                        setLoadingState(false);

                        String errorMessage =
                                task.getException() != null
                                        ? task.getException()
                                        .getMessage()
                                        : "Unable to sign in";

                        Toast.makeText(
                                LoginActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void checkEmailVerification(
            FirebaseUser currentUser
    ) {
        currentUser.reload().addOnCompleteListener(reloadTask -> {
            FirebaseUser refreshedUser =
                    firebaseAuth.getCurrentUser();

            if (refreshedUser == null) {
                setLoadingState(false);
                showMessage("Unable to refresh your account");
                return;
            }

            usersReference
                    .child(refreshedUser.getUid())
                    .child("emailVerificationRequired")
                    .addListenerForSingleValueEvent(
                            new ValueEventListener() {
                                @Override
                                public void onDataChange(
                                        @NonNull DataSnapshot snapshot
                                ) {
                                    Boolean required =
                                            snapshot.getValue(Boolean.class);

                                    boolean verificationRequired =
                                            Boolean.TRUE.equals(required);

                                    if (verificationRequired
                                            && !refreshedUser.isEmailVerified()) {
                                        firebaseAuth.signOut();
                                        setLoadingState(false);
                                        txtResendVerification.setVisibility(
                                                View.VISIBLE
                                        );
                                        showMessage(
                                                "Verify your email before signing in. Check Inbox or Spam."
                                        );
                                        return;
                                    }

                                    setLoadingState(false);
                                    Toast.makeText(
                                            LoginActivity.this,
                                            "Welcome back!",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                    openMainScreen();
                                }

                                @Override
                                public void onCancelled(
                                        @NonNull DatabaseError error
                                ) {
                                    handleVerificationCheckFailure(
                                            refreshedUser
                                    );
                                }
                            }
                    );
        });
    }

    private void handleVerificationCheckFailure(
            FirebaseUser currentUser
    ) {
        setLoadingState(false);

        if (currentUser.isEmailVerified()) {
            openMainScreen();
            return;
        }

        firebaseAuth.signOut();
        txtResendVerification.setVisibility(View.VISIBLE);
        showMessage(
                "Unable to confirm email verification. Check your connection and try again."
        );
    }

    private void resendVerificationEmail() {
        clearErrors();

        String email = getText(etLoginEmail);
        String password = getText(etLoginPassword);

        if (TextUtils.isEmpty(email)
                || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutLoginEmail.setError("Enter your valid email address");
            etLoginEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            layoutLoginPassword.setError("Enter your account password");
            etLoginPassword.requestFocus();
            return;
        }

        setLoadingState(true);

        firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(signInTask -> {
                    if (!signInTask.isSuccessful()) {
                        setLoadingState(false);
                        showMessage("Unable to sign in and resend verification email");
                        return;
                    }

                    FirebaseUser currentUser =
                            firebaseAuth.getCurrentUser();

                    if (currentUser == null) {
                        setLoadingState(false);
                        showMessage("Unable to load your account");
                        return;
                    }

                    if (currentUser.isEmailVerified()) {
                        setLoadingState(false);
                        txtResendVerification.setVisibility(View.GONE);
                        showMessage("Email is already verified. You can sign in.");
                        firebaseAuth.signOut();
                        return;
                    }

                    currentUser
                            .sendEmailVerification()
                            .addOnCompleteListener(sendTask -> {
                                firebaseAuth.signOut();
                                setLoadingState(false);

                                if (sendTask.isSuccessful()) {
                                    showMessage(
                                            "Verification email sent. Check Inbox or Spam."
                                    );
                                } else {
                                    String message =
                                            sendTask.getException() != null
                                                    ? sendTask.getException().getMessage()
                                                    : "Unable to resend verification email";
                                    showMessage(message);
                                }
                            });
                });
    }

    private void sendPasswordResetEmail() {
        clearErrors();

        String email =
                getText(etLoginEmail);

        if (TextUtils.isEmpty(email)) {
            layoutLoginEmail.setError(
                    "Enter your email first"
            );

            etLoginEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            layoutLoginEmail.setError(
                    "Enter a valid email address"
            );

            etLoginEmail.requestFocus();
            return;
        }

        txtForgotPassword.setEnabled(false);

        firebaseAuth
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    txtForgotPassword.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                LoginActivity.this,
                                "Password reset email sent",
                                Toast.LENGTH_LONG
                        ).show();

                    } else {
                        String errorMessage =
                                task.getException() != null
                                        ? task.getException()
                                        .getMessage()
                                        : "Unable to send reset email";

                        Toast.makeText(
                                LoginActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void openSignupScreen() {
        Intent intent =
                new Intent(
                        LoginActivity.this,
                        SignupActivity.class
                );

        startActivity(intent);

        overridePendingTransition(
                R.anim.slide_in_right,
                R.anim.slide_out_left
        );
    }

    private void openMainScreen() {
        Intent intent =
                new Intent(
                        LoginActivity.this,
                        MainActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private String getText(
            TextInputEditText editText
    ) {
        if (editText.getText() == null) {
            return "";
        }

        return editText
                .getText()
                .toString()
                .trim();
    }

    private void clearErrors() {
        layoutLoginEmail.setError(null);
        layoutLoginPassword.setError(null);
    }

    private void showMessage(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private void setLoadingState(
            boolean isLoading
    ) {
        progressLogin.setVisibility(
                isLoading
                        ? View.VISIBLE
                        : View.GONE
        );

        btnLogin.setEnabled(!isLoading);
        txtOpenSignup.setEnabled(!isLoading);
        txtForgotPassword.setEnabled(!isLoading);
        txtResendVerification.setEnabled(!isLoading);

        btnLogin.setText(
                isLoading
                        ? "Signing In..."
                        : "Sign In"
        );
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser =
                firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            checkEmailVerification(currentUser);
        }
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}