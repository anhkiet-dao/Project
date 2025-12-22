package com.example.do_an.presentation.reading.chatbot;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.do_an.data.chatbot.remote.ai.GroqAPIHelper;
import com.example.do_an.R;

import org.json.JSONArray;
import org.json.JSONObject;

public class ChatbotFragment extends Fragment {

    private static final String ARG_OCR = "OCR_TEXT";

    private String ocrText;

    private EditText edtQuestion;
    private TextView txtAnswer;
    private ProgressBar progress;
    private ImageView btnSend, btnBack;

    public static ChatbotFragment newInstance(String ocrText) {
        ChatbotFragment f = new ChatbotFragment();
        Bundle b = new Bundle();
        b.putString(ARG_OCR, ocrText);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ocrText = getArguments().getString(ARG_OCR, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.chatbot_layout_chatbot, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        edtQuestion = view.findViewById(R.id.edtQuestion);
        txtAnswer = view.findViewById(R.id.txtAnswer);
        progress = view.findViewById(R.id.progressAI);
        btnSend = view.findViewById(R.id.btnSend);

        btnSend.setOnClickListener(v -> askAI());
    }

    private void askAI() {

        String question = edtQuestion.getText().toString().trim();
        if (question.isEmpty()) {
            Toast.makeText(getContext(), "Nhập câu hỏi", Toast.LENGTH_SHORT).show();
            return;
        }

        progress.setVisibility(View.VISIBLE);
        txtAnswer.setText("");

        try {
            JSONArray messages = new JSONArray();

            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content",
                            "Bạn là trợ lý trả lời câu hỏi dựa trên nội dung truyện. "
                                    + "Văn bản có thể sai chính tả do OCR, hãy suy luận ngữ cảnh.")
            );

            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content",
                            "Nội dung:\n" + ocrText
                                    + "\n\nCâu hỏi:\n" + question)
            );

            GroqAPIHelper.askAIAsync(messages, new GroqAPIHelper.GroqCallback() {
                @Override
                public void onSuccess(String answer) {
                    requireActivity().runOnUiThread(() -> {
                        progress.setVisibility(View.GONE);
                        txtAnswer.setText(answer);
                    });
                }

                @Override
                public void onError(String error) {
                    requireActivity().runOnUiThread(() -> {
                        progress.setVisibility(View.GONE);
                        txtAnswer.setText(error);
                    });
                }
            });

        } catch (Exception e) {
            progress.setVisibility(View.GONE);
            txtAnswer.setText("Lỗi: " + e.getMessage());
        }
    }
}
