package com.ashutosh.codealpha_fitnesstrackerapp.ai;

import androidx.annotation.NonNull;

import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiManager {

    public interface GeminiCallback {

        void onSuccess(String response);

        void onError(String errorMessage);
    }

    private final GenerativeModelFutures model;
    private final Executor executor;

    public GeminiManager() {

        GenerativeModel generativeModel =
                FirebaseAI
                        .getInstance(GenerativeBackend.googleAI())
                        .generativeModel("gemini-3.6-flash");

        model = GenerativeModelFutures.from(generativeModel);
        executor = Executors.newSingleThreadExecutor();
    }

    public void generateResponse(
            @NonNull String userMessage,
            @NonNull String userContext,
            @NonNull GeminiCallback callback
    ) {

        String completePrompt =
                "You are VitaFit AI Coach, a professional and supportive "
                        + "fitness assistant inside an Android fitness tracking app.\n\n"

                        + "IMPORTANT RULES:\n"
                        + "1. Personalize your reply using the supplied user profile "
                        + "and workout statistics.\n"
                        + "2. Never invent missing profile or workout information.\n"
                        + "3. Keep the response practical, clearly structured and "
                        + "easy to read on a mobile screen.\n"
                        + "4. Use short plain-text headings and the Unicode bullet "
                        + "character • for lists.\n"
                        + "5. Never use Markdown formatting or Markdown symbols such "
                        + "as #, *, **, _, backticks or ---.\n"
                        + "6. Do not diagnose diseases or prescribe medicines.\n"
                        + "7. If the user mentions serious pain, injury, fainting, "
                        + "breathing difficulty or another urgent symptom, advise "
                        + "them to contact a qualified healthcare professional.\n"
                        + "8. Avoid extreme diets, unsafe weight-loss methods and "
                        + "unrealistic promises.\n"
                        + "9. Mention that calorie and nutrition estimates are "
                        + "approximate when appropriate.\n"
                        + "10. For users under 18, keep guidance general, balanced "
                        + "and age-appropriate. Do not provide personalized "
                        + "weight-loss targets, calorie limits, restrictive diets "
                        + "or excessive exercise plans.\n\n"

                        + "USER FITNESS CONTEXT:\n"
                        + userContext
                        + "\n\n"

                        + "USER QUESTION:\n"
                        + userMessage
                        + "\n\n"

                        + "Answer as VitaFit AI Coach.";

        Content prompt =
                new Content.Builder()
                        .addText(completePrompt)
                        .build();

        ListenableFuture<GenerateContentResponse> responseFuture =
                model.generateContent(prompt);

        Futures.addCallback(
                responseFuture,
                new FutureCallback<GenerateContentResponse>() {

                    @Override
                    public void onSuccess(
                            GenerateContentResponse result
                    ) {

                        String responseText = result.getText();

                        if (responseText == null
                                || responseText.trim().isEmpty()) {

                            callback.onError(
                                    "The AI returned an empty response."
                            );
                            return;
                        }

                        callback.onSuccess(
                                cleanForMobileDisplay(responseText)
                        );
                    }

                    @Override
                    public void onFailure(
                            @NonNull Throwable throwable
                    ) {

                        String errorMessage = throwable.getMessage();

                        if (errorMessage == null
                                || errorMessage.trim().isEmpty()) {

                            errorMessage =
                                    "Unable to connect to VitaFit AI Coach.";
                        }

                        callback.onError(errorMessage);
                    }
                },
                executor
        );
    }

    /**
     * Converts occasional Markdown returned by the model into clean text that
     * displays professionally inside a normal Android TextView.
     */
    private String cleanForMobileDisplay(@NonNull String response) {
        String cleaned = response
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .replaceAll("(?m)^\\s*#{1,6}\\s*", "")
                .replaceAll("(?m)^\\s*[-*_]{3,}\\s*$", "")
                .replaceAll("(?m)^\\s*[-*+]\\s+", "• ")
                .replaceAll("\\n{3,}", "\\n\\n")
                .trim();

        return cleaned.isEmpty()
                ? "I couldn't prepare a response. Please try again."
                : cleaned;
    }
}