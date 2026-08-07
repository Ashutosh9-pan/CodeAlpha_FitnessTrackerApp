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

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import utils.ThemeManager;

public class SignupActivity extends AppCompatActivity {

    private static final String FIREBASE_DATABASE_URL =
            "https://fitnesstrackerapp-cb360-default-rtdb.asia-southeast1.firebasedatabase.app";

    private TextInputLayout layoutSignupName;
    private TextInputLayout layoutSignupEmail;
    private TextInputLayout layoutSignupPassword;
    private TextInputLayout layoutSignupConfirmPassword;

    private TextInputEditText etSignupName;
    private TextInputEditText etSignupEmail;
    private TextInputEditText etSignupPassword;
    private TextInputEditText etSignupConfirmPassword;

    private MaterialButton btnSignup;

    private TextView txtOpenLogin;

    private ProgressBar progressSignup;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference usersReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initialiseViews();
        initialiseFirebase();
        setupClickListeners();
        setupKeyboardAction();
    }

    private void initialiseViews() {
        layoutSignupName =
                findViewById(R.id.layoutSignupName);

        layoutSignupEmail =
                findViewById(R.id.layoutSignupEmail);

        layoutSignupPassword =
                findViewById(R.id.layoutSignupPassword);

        layoutSignupConfirmPassword =
                findViewById(R.id.layoutSignupConfirmPassword);

        etSignupName =
                findViewById(R.id.etSignupName);

        etSignupEmail =
                findViewById(R.id.etSignupEmail);

        etSignupPassword =
                findViewById(R.id.etSignupPassword);

        etSignupConfirmPassword =
                findViewById(R.id.etSignupConfirmPassword);

        btnSignup =
                findViewById(R.id.btnSignup);

        txtOpenLogin =
                findViewById(R.id.txtOpenLogin);

        progressSignup =
                findViewById(R.id.progressSignup);
    }

    private void initialiseFirebase() {
        firebaseAuth =
                FirebaseAuth.getInstance();

        FirebaseDatabase firebaseDatabase =
                FirebaseDatabase.getInstance(
                        FIREBASE_DATABASE_URL
                );

        usersReference =
                firebaseDatabase.getReference("users");
    }

    private void setupClickListeners() {
        btnSignup.setOnClickListener(
                view -> createAccount()
        );

        txtOpenLogin.setOnClickListener(
                view -> openLoginScreen()
        );
    }

    private void setupKeyboardAction() {
        etSignupConfirmPassword
                .setOnEditorActionListener(
                        (textView, actionId, event) -> {

                            if (actionId
                                    == EditorInfo.IME_ACTION_DONE) {

                                createAccount();
                                return true;
                            }

                            return false;
                        }
                );
    }

    private void createAccount() {
        clearErrors();

        String fullName =
                getText(etSignupName);

        String email =
                getText(etSignupEmail);

        String password =
                getText(etSignupPassword);

        String confirmPassword =
                getText(etSignupConfirmPassword);

        if (TextUtils.isEmpty(fullName)) {
            layoutSignupName.setError(
                    "Enter your full name"
            );

            etSignupName.requestFocus();
            return;
        }

        if (fullName.length() < 2) {
            layoutSignupName.setError(
                    "Name must contain at least 2 characters"
            );

            etSignupName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            layoutSignupEmail.setError(
                    "Enter your email address"
            );

            etSignupEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            layoutSignupEmail.setError(
                    "Enter a valid email address"
            );

            etSignupEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            layoutSignupPassword.setError(
                    "Create a password"
            );

            etSignupPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            layoutSignupPassword.setError(
                    "Password must contain at least 6 characters"
            );

            etSignupPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            layoutSignupConfirmPassword.setError(
                    "Confirm your password"
            );

            etSignupConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            layoutSignupConfirmPassword.setError(
                    "Passwords do not match"
            );

            etSignupConfirmPassword.requestFocus();
            return;
        }

        setLoadingState(true);

        firebaseAuth
                .createUserWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(task -> {

                    if (!task.isSuccessful()) {
                        setLoadingState(false);

                        String errorMessage =
                                task.getException() != null
                                        ? task.getException()
                                        .getMessage()
                                        : "Unable to create account";

                        Toast.makeText(
                                SignupActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    FirebaseUser currentUser =
                            firebaseAuth.getCurrentUser();

                    if (currentUser == null) {
                        setLoadingState(false);

                        Toast.makeText(
                                SignupActivity.this,
                                "Account created, but user information is unavailable",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    saveBasicUserProfile(
                            currentUser.getUid(),
                            fullName,
                            email
                    );
                });
    }

    private void saveBasicUserProfile(
            String userId,
            String fullName,
            String email
    ) {
        Map<String, Object> userData =
                new HashMap<>();

        userData.put("name", fullName);
        userData.put("email", email);
        userData.put("age", 0);
        userData.put("height", 0.0);
        userData.put("weight", 0.0);
        userData.put("goal", "");
        userData.put("bmi", 0.0);
        userData.put("bmiCategory", "");
        userData.put("emailVerificationRequired", true);
        userData.put(
                "createdAt",
                System.currentTimeMillis()
        );

        usersReference
                .child(userId)
                .setValue(userData)
                .addOnSuccessListener(unused -> {
                    FirebaseUser currentUser =
                            firebaseAuth.getCurrentUser();

                    if (currentUser != null) {
                        sendVerificationEmail(currentUser);
                    } else {
                        finishSignupWithoutEmail();
                    }
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(
                            SignupActivity.this,
                            "Account created, but profile could not be saved: "
                                    + exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                    FirebaseUser currentUser =
                            firebaseAuth.getCurrentUser();

                    if (currentUser != null) {
                        sendVerificationEmail(currentUser);
                    } else {
                        finishSignupWithoutEmail();
                    }
                });
    }

    private void sendVerificationEmail(
            FirebaseUser currentUser
    ) {
        currentUser
                .sendEmailVerification()
                .addOnCompleteListener(task -> {
                    firebaseAuth.signOut();
                    setLoadingState(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                SignupActivity.this,
                                "Verification email sent. Open the link, then sign in.",
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        String message =
                                task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Unable to send verification email";

                        Toast.makeText(
                                SignupActivity.this,
                                message + ". Use Resend Verification on the sign-in screen.",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    openLoginScreen();
                });
    }

    private void finishSignupWithoutEmail() {
        firebaseAuth.signOut();
        setLoadingState(false);

        Toast.makeText(
                this,
                "Account created. Sign in to resend the verification email.",
                Toast.LENGTH_LONG
        ).show();

        openLoginScreen();
    }

    private void openLoginScreen() {
        Intent intent =
                new Intent(
                        SignupActivity.this,
                        LoginActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        startActivity(intent);
        finish();

        overridePendingTransition(
                R.anim.slide_in_left,
                R.anim.slide_out_right
        );
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
        layoutSignupName.setError(null);
        layoutSignupEmail.setError(null);
        layoutSignupPassword.setError(null);
        layoutSignupConfirmPassword.setError(null);
    }

    private void setLoadingState(
            boolean isLoading
    ) {
        progressSignup.setVisibility(
                isLoading
                        ? View.VISIBLE
                        : View.GONE
        );

        btnSignup.setEnabled(!isLoading);
        txtOpenLogin.setEnabled(!isLoading);

        btnSignup.setText(
                isLoading
                        ? "Creating Account..."
                        : "Create Account"
        );
    }

    @Override
    public void onBackPressed() {
        openLoginScreen();
    }
}