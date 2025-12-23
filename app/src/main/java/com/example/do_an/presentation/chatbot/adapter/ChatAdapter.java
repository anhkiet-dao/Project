package com.example.do_an.presentation.chatbot.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.example.do_an.domain.chatbot.model.Message;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private static final int TYPE_USER = Message.TYPE_USER;
    private static final int TYPE_BOT = Message.TYPE_BOT;

    private List<Message> items;

    // =========================================================
    // Constructor
    // =========================================================

    public ChatAdapter(List<Message> items) {
        this.items = items;
    }

    // =========================================================
    // RecyclerView Methods
    // =========================================================

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chatbot_message_user, parent, false);
            return new ViewHolder(view, viewType);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chatbot_message_bot, parent, false);
            return new ViewHolder(view, viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = items.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // =========================================================
    // ViewHolder
    // =========================================================

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        int viewType;

        ViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;

            if (viewType == TYPE_USER) {
                textMessage = itemView.findViewById(R.id.txtMessageUser);
            } else {
                textMessage = itemView.findViewById(R.id.txtMessageBot);
            }
        }

        void bind(Message message) {
            textMessage.setText(message.content);
        }
    }
}