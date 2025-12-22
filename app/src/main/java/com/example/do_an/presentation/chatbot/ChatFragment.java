package com.example.do_an.presentation.chatbot;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.data.chatbot.remote.ai.GroqAPIHelper;
import com.example.do_an.R;
import com.example.do_an.domain.chatbot.model.Message;
import com.example.do_an.presentation.reading.reader.ReadFragment;
import com.example.do_an.presentation.chatbot.adapter.ChatAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerViewChat;
    private EditText edtMessage;
    private ImageButton btnSend;

    // Sử dụng List<Message> mới
    private List<Message> messages;
    private ChatAdapter adapter;
    private LinearLayoutManager layoutManager;

    private ReadFragment.NavigationListener navigationListener;
    private JSONArray conversation = new JSONArray();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ReadFragment.NavigationListener) {
            navigationListener = (ReadFragment.NavigationListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.chatbot_fragment_chat, container, false);

        recyclerViewChat = view.findViewById(R.id.recyclerViewChat);
        edtMessage = view.findViewById(R.id.edtMessage);
        btnSend = view.findViewById(R.id.btnSend);

        messages = new ArrayList<>();
        adapter = new ChatAdapter(messages);

        layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);

        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(adapter);

        try {
            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content", "Bạn là một AI trợ lý thân thiện, nhiệt tình và gần gũi. Hãy trả lời người dùng bằng tiếng Việt với giọng điệu tự nhiên, chân thành và mang tính chất chia sẻ, gợi mở. Dùng từ ngữ đơn giản, dễ hiểu. QUAN TRỌNG: Không được sử dụng bất kỳ ký tự hoặc cú pháp định dạng Markdown nào (như **in đậm** hoặc #heading).");
            conversation.put(system);
        } catch (Exception e) {
            e.printStackTrace();
        }

        messages.add(new Message(Message.TYPE_BOT, "Xin chào! Tôi là AI trợ lý 🤖"));
        adapter.notifyDataSetChanged();

        btnSend.setOnClickListener(v -> sendMessage());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (navigationListener != null) {
            navigationListener.setBottomNavVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (navigationListener != null) {
            navigationListener.setBottomNavVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationListener = null;
    }

    private void sendMessage() {
        String userMsg = edtMessage.getText().toString().trim();
        if (userMsg.isEmpty()) return;

        btnSend.setEnabled(false);

        messages.add(new Message(Message.TYPE_USER, userMsg));
        adapter.notifyItemInserted(messages.size() - 1);
        edtMessage.setText("");

        recyclerViewChat.smoothScrollToPosition(messages.size() - 1);

        messages.add(new Message(Message.TYPE_BOT, "Đang trả lời..."));
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerViewChat.smoothScrollToPosition(messages.size() - 1);

        final int botIndex = messages.size() - 1;

        new Thread(() -> {
            try {
                JSONObject user = new JSONObject();
                user.put("role", "user");
                user.put("content", userMsg);
                conversation.put(user);

                String reply = GroqAPIHelper.askAI(conversation);

                if (reply == null || reply.startsWith("❌")) {
                    reply = "Xin lỗi, hiện tại tôi không thể trả lời 😥";
                }

                String cleanReply = reply
                        .replace("**", "")
                        .replace("*", "");

                JSONObject bot = new JSONObject();
                bot.put("role", "assistant");
                bot.put("content", cleanReply);
                conversation.put(bot);

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        messages.get(botIndex).content = cleanReply;
                        adapter.notifyItemChanged(botIndex);

                        recyclerViewChat.smoothScrollToPosition(messages.size() - 1);
                        btnSend.setEnabled(true);
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        messages.get(botIndex).content = "❌ Có lỗi xảy ra";
                        adapter.notifyItemChanged(botIndex);
                        btnSend.setEnabled(true);
                    });
                }
            }
        }).start();
    }
}