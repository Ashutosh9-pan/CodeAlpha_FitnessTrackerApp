package com.ashutosh.codealpha_fitnesstrackerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> messageList;

    public ChatAdapter(List<ChatMessage> messageList) {
        this.messageList = messageList;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getMessageType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        LayoutInflater inflater =
                LayoutInflater.from(parent.getContext());

        if (viewType == ChatMessage.TYPE_USER) {

            View view =
                    inflater.inflate(
                            R.layout.chat_user,
                            parent,
                            false
                    );

            return new UserMessageViewHolder(view);

        } else {

            View view =
                    inflater.inflate(
                            R.layout.chat_ai,
                            parent,
                            false
                    );

            return new AIMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {

        ChatMessage chatMessage =
                messageList.get(position);

        if (holder instanceof UserMessageViewHolder) {

            ((UserMessageViewHolder) holder)
                    .txtUserMessage
                    .setText(chatMessage.getMessage());

        } else if (holder instanceof AIMessageViewHolder) {

            ((AIMessageViewHolder) holder)
                    .txtAIMessage
                    .setText(chatMessage.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class UserMessageViewHolder
            extends RecyclerView.ViewHolder {

        TextView txtUserMessage;

        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);

            txtUserMessage =
                    itemView.findViewById(
                            R.id.txtUserMessage
                    );
        }
    }

    static class AIMessageViewHolder
            extends RecyclerView.ViewHolder {

        TextView txtAIMessage;

        AIMessageViewHolder(@NonNull View itemView) {
            super(itemView);

            txtAIMessage =
                    itemView.findViewById(
                            R.id.txtAIMessage
                    );
        }
    }
}