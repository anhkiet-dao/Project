package com.example.do_an.chatbot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private static final int TYPE_USER = Message.TYPE_USER;
    private static final int TYPE_BOT = Message.TYPE_BOT;

    private final List<Message> messages;

    public ChatAdapter(List<Message> messages) {
        this.messages = messages;
    }

    // 1. Xác định loại View dựa trên Message.type
    @Override
    public int getItemViewType(int position) {
        return messages.get(position).type;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_USER) {
            // Dùng layout cho tin nhắn User (bong bóng xanh, căn phải)
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chatbot_message_user, parent, false);
            return new UserMessageViewHolder(view);
        } else { // TYPE_BOT
            // Dùng layout cho tin nhắn Bot (bong bóng xám, căn trái)
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chatbot_message_bot, parent, false);
            return new BotMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);

        if (holder.getItemViewType() == TYPE_USER) {
            ((UserMessageViewHolder) holder).txtMessageUser.setText(message.content);
        } else {
            ((BotMessageViewHolder) holder).txtMessageBot.setText(message.content);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static abstract class MessageViewHolder extends RecyclerView.ViewHolder {
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class UserMessageViewHolder extends MessageViewHolder {
        TextView txtMessageUser;

        public UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMessageUser = itemView.findViewById(R.id.txtMessageUser);
        }
    }

    public static class BotMessageViewHolder extends MessageViewHolder {
        TextView txtMessageBot;

        public BotMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMessageBot = itemView.findViewById(R.id.txtMessageBot);
        }
    }
}