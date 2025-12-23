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

import com.example.do_an.R;
import com.example.do_an.domain.chatbot.model.Message;
import com.example.do_an.presentation.reading.reader.ReadFragment;
import com.example.do_an.presentation.chatbot.adapter.ChatAdapter;
import com.example.do_an.data.chatbot.remote.ai.ChatAIHelper;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerChat;
    private EditText editMessage;
    private ImageButton btnSend;

    private List<Message> messages;
    private ChatAdapter adapter;
    private LinearLayoutManager layoutManager;
    private ChatAIHelper aiHelper;

    private ReadFragment.NavigationListener navigationListener;

    // =========================================================
    // Lifecycle
    // =========================================================

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ReadFragment.NavigationListener) {
            navigationListener = (ReadFragment.NavigationListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.chatbot_fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        initDependencies();
        setupUi();
        bindActions();

        addWelcomeMessage();
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

    // =========================================================
    // 1️⃣ Setup phase
    // =========================================================

    private void bindViews(View view) {
        recyclerChat = view.findViewById(R.id.recyclerViewChat);
        editMessage = view.findViewById(R.id.edtMessage);
        btnSend = view.findViewById(R.id.btnSend);
    }

    private void initDependencies() {
        messages = new ArrayList<>();
        adapter = new ChatAdapter(messages);
        layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        aiHelper = new ChatAIHelper();
    }

    private void setupUi() {
        recyclerChat.setLayoutManager(layoutManager);
        recyclerChat.setAdapter(adapter);
    }

    private void bindActions() {
        btnSend.setOnClickListener(v -> onSendIntent());
    }

    // =========================================================
    // 2️⃣ UI helpers
    // =========================================================

    private void addWelcomeMessage() {
        messages.add(new Message(Message.TYPE_BOT, getString(R.string.chatbot_welcome_message)));
        adapter.notifyDataSetChanged();
    }

    private void addUserMessage(String text) {
        messages.add(new Message(Message.TYPE_USER, text));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        messages.add(new Message(Message.TYPE_BOT, text));
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
    }

    private void updateBotMessage(int index, String text) {
        messages.get(index).content = text;
        adapter.notifyItemChanged(index);
        scrollToBottom();
    }

    private void scrollToBottom() {
        recyclerChat.smoothScrollToPosition(messages.size() - 1);
    }

    private void showLoading(boolean show) {
        btnSend.setEnabled(!show);
    }

    // =========================================================
    // 3️⃣ Intent handlers
    // =========================================================

    private void onSendIntent() {
        String userMsg = editMessage.getText().toString().trim();
        if (userMsg.isEmpty())
            return;

        showLoading(true);
        editMessage.setText("");

        addUserMessage(userMsg);
        addBotMessage(getString(R.string.chatbot_typing_indicator));

        doAskAI(userMsg, messages.size() - 1);
    }

    // =========================================================
    // 4️⃣ Business actions
    // =========================================================

    private void doAskAI(String userMsg, int botIndex) {
        aiHelper.askAI(userMsg, new ChatAIHelper.AICallback() {
            @Override
            public void onSuccess(String reply) {
                onAISuccess(botIndex, reply);
            }

            @Override
            public void onError(String error) {
                onAIFailed(botIndex);
            }
        });
    }

    // =========================================================
    // 5️⃣ Result handlers
    // =========================================================

    private void onAISuccess(int botIndex, String reply) {
        if (!isAdded())
            return;

        requireActivity().runOnUiThread(() -> {
            updateBotMessage(botIndex, reply);
            showLoading(false);
        });
    }

    private void onAIFailed(int botIndex) {
        if (!isAdded())
            return;

        requireActivity().runOnUiThread(() -> {
            updateBotMessage(botIndex, getString(R.string.chatbot_error_message));
            showLoading(false);
        });
    }
}