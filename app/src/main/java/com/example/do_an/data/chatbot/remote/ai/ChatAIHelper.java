package com.example.do_an.data.chatbot.remote.ai;

import org.json.JSONArray;
import org.json.JSONObject;

public class ChatAIHelper {

    private static final String GEMINI_PROMPT = "Bạn là một AI trợ lý thân thiện, nhiệt tình và gần gũi. "
            + "Hãy trả lời người dùng bằng tiếng Việt với giọng điệu tự nhiên, chân thành và mang tính chất chia sẻ, gợi mở. "
            + "Dùng từ ngữ đơn giản, dễ hiểu. "
            + "QUAN TRỌNG: Không được sử dụng bất kỳ ký tự hoặc cú pháp định dạng Markdown nào (như **in đậm** hoặc #heading).";

    private final JSONArray conversation = new JSONArray();

    public ChatAIHelper() {
        initSystemPrompt();
    }

    public void askAI(String userMessage, AICallback callback) {
        new Thread(() -> {
            try {
                addUserMessage(userMessage);

                String reply = GroqAPIHelper.askAI(conversation);

                if (isErrorReply(reply)) {
                    callback.onError("Xin lỗi, hiện tại tôi không thể trả lời 😥");
                    return;
                }

                String cleanReply = cleanMarkdown(reply);
                addBotMessage(cleanReply);

                callback.onSuccess(cleanReply);

            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("❌ Có lỗi xảy ra");
            }
        }).start();
    }

    private void initSystemPrompt() {
        try {
            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content", GEMINI_PROMPT);
            conversation.put(system);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addUserMessage(String message) throws Exception {
        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", message);
        conversation.put(user);
    }

    private void addBotMessage(String message) throws Exception {
        JSONObject bot = new JSONObject();
        bot.put("role", "assistant");
        bot.put("content", message);
        conversation.put(bot);
    }

    private boolean isErrorReply(String reply) {
        return reply == null || reply.startsWith("❌");
    }

    private String cleanMarkdown(String text) {
        return text.replace("**", "").replace("*", "");
    }

    // =========================================================
    // 3️⃣ Callback interface
    // =========================================================

    public interface AICallback {
        void onSuccess(String reply);

        void onError(String error);
    }
}
