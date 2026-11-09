package ru.governix.greet.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.governix.greet.config.GreetConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WelcomeScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget search;
    private TextFieldWidget nickInput;
    private TextFieldWidget messageInput;

    private String selectedNick;
    private String notice = "Выбери игрока слева, чтобы настроить его приветствия.";
    private int editingGreetingIndex = -1;
    private int targetScroll;
    private int messageScroll;

    private int px, py, pw, ph, lx, ly, lw, rx, rw, paneH;
    private int searchY, nickY, messageY, targetY, messageListY;
    private int messageActionX, messageCancelX, messageFieldWidth;

    public WelcomeScreen(Screen parent) {
        super(Text.literal("GreetMate"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        pw = Math.min(760, width - 24);
        ph = Math.min(450, height - 24);
        px = (width - pw) / 2;
        py = (height - ph) / 2;

        lx = px + 16;
        ly = py + 76;
        lw = Math.min(250, (pw - 44) / 2);
        rx = lx + lw + 12;
        rw = px + pw - 16 - rx;
        paneH = ph - 116;

        searchY = ly + 36;
        nickY = ly + paneH - 32;
        messageY = ly + paneH - 32;
        targetY = searchY + 31;
        messageListY = ly + 82;

        search = new TextFieldWidget(textRenderer, lx + 12, searchY, lw - 24, 20, Text.literal("Search"));
        search.setMaxLength(16);
        search.setPlaceholder(Text.literal("Поиск по нику..."));
        search.setDrawsBackground(false);
        search.setEditableColor(0xFFF0F4FF);
        addDrawableChild(search);

        int nickW = Math.max(85, lw - 150);
        nickInput = new TextFieldWidget(textRenderer, lx + 12, nickY, nickW, 20, Text.literal("Ник"));
        nickInput.setMaxLength(16);
        nickInput.setPlaceholder(Text.literal("Ник игрока"));
        nickInput.setDrawsBackground(false);
        nickInput.setEditableColor(0xFFF0F4FF);
        addDrawableChild(nickInput);

        messageFieldWidth = Math.max(120, rw - 164);
        messageInput = new TextFieldWidget(textRenderer, rx + 12, messageY,
                messageFieldWidth, 20, Text.literal("Сообщение"));
        messageInput.setMaxLength(256);
        messageInput.setPlaceholder(Text.literal("Напиши своё сообщение..."));
        messageInput.setDrawsBackground(false);
        messageInput.setEditableColor(0xFFF0F4FF);
        addDrawableChild(messageInput);

        messageActionX = rx + 20 + messageFieldWidth;
        messageCancelX = messageActionX + 92;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xC9080B14);
        panel(ctx, px, py, pw, ph, 0xFF0F1422, 0xFF303C56);
        panel(ctx, lx, ly, lw, paneH, 0xFF141B2B, 0xFF26334A);
        panel(ctx, rx, ly, rw, paneH, 0xFF141B2B, 0xFF26334A);
        ctx.fill(px + 1, py + 1, px + pw - 1, py + 4, 0xFF7485FF);

        drawTextCenteredY(ctx, "GreetMate", px + 20, py + 12, 18, 0xFFF2F5FF);
        drawTextCenteredY(ctx, "Персональные приветствия и быстрый выбор", px + 20, py + 32, 18, 0xFF8F9CB5);
        button(ctx, px + pw - 132, py + 18, 112, 28,
                GreetConfig.isAutoGreetingEnabled() ? "АВТО: ВКЛ" : "АВТО: ВЫКЛ",
                GreetConfig.isAutoGreetingEnabled(), mx, my);

        drawTextCenteredY(ctx, "Игроки", lx + 12, ly + 7, 18, 0xFFE7ECFA);
        drawTextCenteredY(ctx, GreetConfig.getTargets().size() + " ников",
                lx + lw - 64, ly + 7, 18, 0xFF8190AA);
        inputFrame(ctx, lx + 11, searchY - 1, lw - 22, 22, search.isFocused());

        List<String> names = filteredTargets();
        int visibleTargets = Math.max(1, Math.min(7, (nickY - targetY - 6) / 25));
        targetScroll = clamp(targetScroll, names.size(), visibleTargets);

        for (int i = 0; i < visibleTargets && targetScroll + i < names.size(); i++) {
            String name = names.get(targetScroll + i);
            int rowY = targetY + i * 25;
            boolean chosen = name.equalsIgnoreCase(selectedNick);
            boolean over = inside(mx, my, lx + 8, rowY - 3, lw - 16, 23);

            if (chosen || over) {
                ctx.fill(lx + 8, rowY - 3, lx + lw - 8, rowY + 20,
                        chosen ? 0xFF28375A : 0xFF1D2940);
            }
            if (chosen) ctx.fill(lx + 8, rowY - 3, lx + 11, rowY + 20, 0xFF7C8CFF);

            drawTextCenteredY(ctx, textRenderer.trimToWidth(name, lw - 56),
                    lx + 17, rowY - 3, 23, 0xFFF0F3FC);

            boolean online = WelcomeTracker.isOnline(name);
            drawTextCenteredY(ctx, online ? "●" : "○", lx + lw - 29,
                    rowY - 3, 23, online ? 0xFF5CE0A0 : 0xFF65718A);
        }

        if (names.isEmpty()) {
            drawTextCenteredY(ctx, search.getText().isBlank() ? "Список пуст" : "Ничего не найдено",
                    lx + 14, targetY, 23, 0xFF78859D);
        }

        inputFrame(ctx, lx + 11, nickY - 1, nickInput.getWidth() + 2, 22, nickInput.isFocused());
        button(ctx, lx + 19 + nickInput.getWidth(), nickY, 38, 20, "+", true, mx, my);
        button(ctx, lx + 61 + nickInput.getWidth(), nickY,
                lw - nickInput.getWidth() - 81, 20, "УДАЛИТЬ", selectedNick != null, mx, my);

        drawTextCenteredY(ctx, selectedNick == null ? "Сообщения" : "Для " + selectedNick,
                rx + 12, ly + 7, 18, 0xFFE7ECFA);
        drawTextCenteredY(ctx, selectedNick == null ? "Сначала выбери ник слева" :
                        "Вариантов: " + GreetConfig.getGreetings(selectedNick).size() + " · случайный выбор",
                rx + 12, ly + 25, 18, 0xFF8190AA);
        drawTextCenteredY(ctx, "{player} — подстановка ника (необязательно)",
                rx + 12, ly + 43, 18, 0xFF74829B);

        List<String> messages = selectedNick == null ? List.of() : GreetConfig.getGreetings(selectedNick);
        int visibleMessages = Math.max(1, Math.min(7, (messageY - messageListY - 6) / 25));
        messageScroll = clamp(messageScroll, messages.size(), visibleMessages);

        for (int i = 0; i < visibleMessages && messageScroll + i < messages.size(); i++) {
            int index = messageScroll + i;
            int rowY = messageListY + i * 25;
            boolean selectedForEdit = index == editingGreetingIndex;
            boolean over = inside(mx, my, rx + 8, rowY - 3, rw - 16, 23);

            ctx.fill(rx + 8, rowY - 3, rx + rw - 8, rowY + 20,
                    selectedForEdit ? 0xFF303F65 : over ? 0xFF1D2940 : 0xFF192235);
            if (selectedForEdit) {
                ctx.fill(rx + 8, rowY - 3, rx + 11, rowY + 20, 0xFF8A9AFF);
            }

            drawTextCenteredY(ctx, textRenderer.trimToWidth(messages.get(index), rw - 70),
                    rx + 17, rowY - 3, 23, 0xFFDDE4F4);
            drawTextCenteredY(ctx, "×", rx + rw - 28, rowY - 3, 23, 0xFFFF8795);
        }

        if (selectedNick != null && messages.isEmpty()) {
            drawTextCenteredY(ctx, "Для этого ника пока нет сообщений.",
                    rx + 16, messageListY + 2, 20, 0xFF78859D);
            drawTextCenteredY(ctx, "Добавь своё сообщение снизу.",
                    rx + 16, messageListY + 22, 20, 0xFF78859D);
        } else if (selectedNick == null) {
            drawTextCenteredY(ctx, "У каждого игрока свой список фраз.",
                    rx + 16, messageListY + 2, 20, 0xFF78859D);
        }

        inputFrame(ctx, rx + 11, messageY - 1, messageFieldWidth + 2, 22, messageInput.isFocused());
        button(ctx, messageActionX, messageY, 86, 20,
                editingGreetingIndex >= 0 ? "СОХРАНИТЬ" : "ДОБАВИТЬ",
                selectedNick != null, mx, my);
        button(ctx, messageCancelX, messageY, 32, 20, "×",
                editingGreetingIndex >= 0, mx, my);

        drawTextCenteredY(ctx, "G — настройки · H — колесо приветствий",
                px + 20, py + ph - 29, 18, 0xFF74829B);
        drawTextCenteredY(ctx, textRenderer.trimToWidth(notice, 255),
                px + pw - 270, py + ph - 29, 18, 0xFF9AA8C0);

        super.render(ctx, mx, my, delta);
    }

    private void drawTextCenteredY(DrawContext ctx, String value, int x, int top, int height, int color) {
        int textY = top + Math.max(0, (height - textRenderer.fontHeight) / 2);
        ctx.drawTextWithShadow(textRenderer, value, x, textY, color);
    }

    private List<String> filteredTargets() {
        String filter = search.getText().trim().toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String name : GreetConfig.getTargets()) {
            if (filter.isEmpty() || name.toLowerCase(Locale.ROOT).contains(filter)) result.add(name);
        }
        return result;
    }

    private void panel(DrawContext ctx, int x, int y, int w, int h, int fill, int border) {
        ctx.fill(x, y, x + w, y + h, border);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private void inputFrame(DrawContext ctx, int x, int y, int w, int h, boolean focused) {
        ctx.fill(x, y, x + w, y + h, focused ? 0xFF6579D9 : 0xFF303D57);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF0D1320);
    }

    private void button(DrawContext ctx, int x, int y, int w, int h, String label,
                        boolean enabled, int mx, int my) {
        boolean over = inside(mx, my, x, y, w, h);
        int color = enabled
                ? (over ? 0xFF5269D7 : 0xFF3B50B0)
                : (over ? 0xFF35435D : 0xFF253149);
        ctx.fill(x, y, x + w, y + h, color);
        int textY = y + Math.max(0, (h - textRenderer.fontHeight) / 2);
        ctx.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, textY, 0xFFF5F7FF);
    }

    private boolean inside(double x, double y, int left, int top, int w, int h) {
        return x >= left && x < left + w && y >= top && y < top + h;
    }

    private int clamp(int value, int count, int visible) {
        return Math.max(0, Math.min(value, Math.max(0, count - visible)));
    }

    private void clearGreetingEditor() {
        editingGreetingIndex = -1;
        messageInput.setText("");
        notice = "Режим редактирования закрыт.";
    }

    private void addNick() {
        String nick = nickInput.getText().trim();
        if (GreetConfig.addTarget(nick)) {
            selectedNick = nick;
            nickInput.setText("");
            messageInput.setText("");
            editingGreetingIndex = -1;
            targetScroll = 0;
            messageScroll = 0;
            notice = "Ник добавлен. Впиши для него фразу.";
        } else {
            notice = "Ник неверный или уже добавлен.";
        }
    }

    private void saveMessage() {
        if (selectedNick == null) {
            notice = "Сначала выбери ник.";
            return;
        }

        String value = messageInput.getText();
        if (editingGreetingIndex >= 0) {
            if (GreetConfig.updateGreeting(selectedNick, editingGreetingIndex, value)) {
                editingGreetingIndex = -1;
                messageInput.setText("");
                notice = "Изменения сохранены.";
            } else {
                notice = "Не удалось сохранить: проверь текст или повтор сообщения.";
            }
        } else if (GreetConfig.addGreeting(selectedNick, value)) {
            messageInput.setText("");
            messageScroll = Math.max(0, GreetConfig.getGreetings(selectedNick).size() - 7);
            notice = "Сообщение добавлено.";
        } else {
            notice = "Пустое, повторное сообщение или лимит 32.";
        }
    }

    private void selectGreetingForEdit(int index) {
        List<String> messages = GreetConfig.getGreetings(selectedNick);
        if (index < 0 || index >= messages.size()) return;
        editingGreetingIndex = index;
        messageInput.setText(messages.get(index));
        messageInput.setCursorToEnd(false);
        messageInput.setFocused(true);
        notice = "Измени текст снизу и нажми «Сохранить».";
    }

    private void removeGreeting(int index) {
        if (!GreetConfig.removeGreeting(selectedNick, index)) return;

        if (editingGreetingIndex == index) {
            editingGreetingIndex = -1;
            messageInput.setText("");
        } else if (editingGreetingIndex > index) {
            editingGreetingIndex--;
        }
        notice = "Фраза удалена.";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        if (inside(mouseX, mouseY, px + pw - 132, py + 18, 112, 28)) {
            GreetConfig.setAutoGreetingEnabled(!GreetConfig.isAutoGreetingEnabled());
            notice = GreetConfig.isAutoGreetingEnabled()
                    ? "Автоприветствия включены." : "Автоприветствия выключены.";
            return true;
        }

        List<String> names = filteredTargets();
        int visibleTargets = Math.max(1, Math.min(7, (nickY - targetY - 6) / 25));
        for (int i = 0; i < visibleTargets && targetScroll + i < names.size(); i++) {
            int rowY = targetY + i * 25;
            if (inside(mouseX, mouseY, lx + 8, rowY - 3, lw - 16, 23)) {
                selectedNick = names.get(targetScroll + i);
                messageInput.setText("");
                editingGreetingIndex = -1;
                messageScroll = 0;
                notice = "Выбран игрок " + selectedNick + ".";
                return true;
            }
        }

        int addNickX = lx + 19 + nickInput.getWidth();
        if (inside(mouseX, mouseY, addNickX, nickY, 38, 20)) {
            addNick();
            return true;
        }

        int removeNickX = lx + 61 + nickInput.getWidth();
        if (inside(mouseX, mouseY, removeNickX, nickY,
                lw - nickInput.getWidth() - 81, 20)) {
            if (selectedNick != null && GreetConfig.removeTarget(selectedNick)) {
                notice = "Игрок и его фразы удалены.";
                selectedNick = null;
                editingGreetingIndex = -1;
                messageInput.setText("");
            } else {
                notice = "Сначала выбери игрока.";
            }
            return true;
        }

        if (selectedNick != null) {
            List<String> messages = GreetConfig.getGreetings(selectedNick);
            int visibleMessages = Math.max(1, Math.min(7, (messageY - messageListY - 6) / 25));
            for (int i = 0; i < visibleMessages && messageScroll + i < messages.size(); i++) {
                int index = messageScroll + i;
                int rowY = messageListY + i * 25;
                if (!inside(mouseX, mouseY, rx + 8, rowY - 3, rw - 16, 23)) continue;

                if (inside(mouseX, mouseY, rx + rw - 40, rowY - 3, 32, 23)) {
                    removeGreeting(index);
                } else {
                    selectGreetingForEdit(index);
                }
                return true;
            }

            if (inside(mouseX, mouseY, messageActionX, messageY, 86, 20)) {
                saveMessage();
                return true;
            }
            if (inside(mouseX, mouseY, messageCancelX, messageY, 32, 20)
                    && editingGreetingIndex >= 0) {
                clearGreetingEditor();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (inside(mouseX, mouseY, lx, ly, lw, paneH)) {
            targetScroll = clamp(targetScroll + (vertical < 0 ? 1 : -1),
                    filteredTargets().size(), 7);
            return true;
        }
        if (inside(mouseX, mouseY, rx, ly, rw, paneH)) {
            int messageCount = selectedNick == null ? 0 : GreetConfig.getGreetings(selectedNick).size();
            messageScroll = clamp(messageScroll + (vertical < 0 ? 1 : -1), messageCount, 7);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (nickInput.isFocused()) {
                addNick();
                return true;
            }
            if (messageInput.isFocused()) {
                saveMessage();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && editingGreetingIndex >= 0) {
            clearGreetingEditor();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
