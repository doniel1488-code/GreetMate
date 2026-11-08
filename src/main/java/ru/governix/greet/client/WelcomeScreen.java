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
    private String notice = "Добавь игрока, выбери его и впиши своё сообщение.";
    private int targetScroll;
    private int messageScroll;
    private int px, py, pw, ph, lx, ly, lw, rx, rw, paneH;
    private int searchY, nickY, messageY, targetY, messageListY;

    public WelcomeScreen(Screen parent) {
        super(Text.literal("GreetMate"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        pw = Math.min(720, width - 24);
        ph = Math.min(430, height - 24);
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

        int messageW = Math.max(110, rw - 100);
        messageInput = new TextFieldWidget(textRenderer, rx + 12, messageY, messageW, 20, Text.literal("Сообщение"));
        messageInput.setMaxLength(256);
        messageInput.setPlaceholder(Text.literal("Напиши, как обычно пишешь..."));
        messageInput.setDrawsBackground(false);
        messageInput.setEditableColor(0xFFF0F4FF);
        addDrawableChild(messageInput);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xC9080B14);
        panel(ctx, px, py, pw, ph, 0xFF0F1422, 0xFF303C56);
        panel(ctx, lx, ly, lw, paneH, 0xFF141B2B, 0xFF26334A);
        panel(ctx, rx, ly, rw, paneH, 0xFF141B2B, 0xFF26334A);
        ctx.fill(px + 1, py + 1, px + pw - 1, py + 4, 0xFF7485FF);

        ctx.drawTextWithShadow(textRenderer, "GREETMATE", px + 20, py + 16, 0xFFF2F5FF);
        ctx.drawTextWithShadow(textRenderer, "ТВОИ ПРИВЕТСТВИЯ ДЛЯ ИГРОКОВ ИЗ TAB", px + 20, py + 37, 0xFF8F9CB5);
        button(ctx, px + pw - 132, py + 18, 112, 28,
                GreetConfig.isEnabled() ? "ВКЛЮЧЕНО" : "ВЫКЛЮЧЕНО", GreetConfig.isEnabled(), mx, my);

        ctx.drawTextWithShadow(textRenderer, "ИГРОКИ", lx + 12, ly + 12, 0xFFE7ECFA);
        ctx.drawTextWithShadow(textRenderer, GreetConfig.getTargets().size() + " ников", lx + lw - 64, ly + 12, 0xFF8190AA);
        inputFrame(ctx, lx + 11, searchY - 1, lw - 22, 22, search.isFocused());

        List<String> names = filteredTargets();
        int visible = Math.max(1, Math.min(7, (nickY - targetY - 6) / 25));
        targetScroll = clamp(targetScroll, names.size(), visible);
        for (int i = 0; i < visible && targetScroll + i < names.size(); i++) {
            String name = names.get(targetScroll + i);
            int y = targetY + i * 25;
            boolean chosen = name.equalsIgnoreCase(selectedNick);
            boolean over = inside(mx, my, lx + 8, y - 3, lw - 16, 23);
            if (chosen || over) ctx.fill(lx + 8, y - 3, lx + lw - 8, y + 20, chosen ? 0xFF28375A : 0xFF1D2940);
            if (chosen) ctx.fill(lx + 8, y - 3, lx + 11, y + 20, 0xFF7C8CFF);
            ctx.drawTextWithShadow(textRenderer, name, lx + 17, y + 3, 0xFFF0F3FC);
            boolean online = WelcomeTracker.isOnline(name);
            ctx.drawTextWithShadow(textRenderer, online ? "●" : "○", lx + lw - 29, y + 3,
                    online ? 0xFF5CE0A0 : 0xFF65718A);
        }
        if (names.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, search.getText().isBlank() ? "Пока никого нет" : "Ничего не найдено",
                    lx + 14, targetY + 8, 0xFF78859D);
        }

        inputFrame(ctx, lx + 11, nickY - 1, nickInput.getWidth() + 2, 22, nickInput.isFocused());
        button(ctx, lx + 19 + nickInput.getWidth(), nickY, 38, 20, "+", true, mx, my);
        button(ctx, lx + 61 + nickInput.getWidth(), nickY, lw - nickInput.getWidth() - 81, 20, "УДАЛИТЬ", false, mx, my);

        ctx.drawTextWithShadow(textRenderer, selectedNick == null ? "СООБЩЕНИЯ" : "ДЛЯ " + selectedNick,
                rx + 12, ly + 12, 0xFFE7ECFA);
        ctx.drawTextWithShadow(textRenderer, selectedNick == null ? "Сначала выбери ник слева" :
                        "Вариантов: " + GreetConfig.getGreetings(selectedNick).size() + "  •  случайный выбор",
                rx + 12, ly + 31, 0xFF8190AA);
        ctx.drawTextWithShadow(textRenderer, "{player} — необязательная подстановка ника",
                rx + 12, ly + 48, 0xFF74829B);

        List<String> messages = selectedNick == null ? List.of() : GreetConfig.getGreetings(selectedNick);
        int msgVisible = Math.max(1, Math.min(7, (messageY - messageListY - 6) / 25));
        messageScroll = clamp(messageScroll, messages.size(), msgVisible);
        for (int i = 0; i < msgVisible && messageScroll + i < messages.size(); i++) {
            int index = messageScroll + i;
            int y = messageListY + i * 25;
            boolean over = inside(mx, my, rx + 8, y - 3, rw - 16, 23);
            ctx.fill(rx + 8, y - 3, rx + rw - 8, y + 20, over ? 0xFF1D2940 : 0xFF192235);
            ctx.drawTextWithShadow(textRenderer,
                    textRenderer.trimToWidth(messages.get(index), rw - 58), rx + 16, y + 3, 0xFFDDE4F4);
            ctx.drawTextWithShadow(textRenderer, "×", rx + rw - 27, y + 2, 0xFFFF8795);
        }
        if (selectedNick != null && messages.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "Для этого ника пока нет сообщений.", rx + 16, messageListY + 8, 0xFF78859D);
            ctx.drawTextWithShadow(textRenderer, "Придумай своё и добавь снизу.", rx + 16, messageListY + 22, 0xFF78859D);
        }
        if (selectedNick == null) {
            ctx.drawTextWithShadow(textRenderer, "У каждого игрока свой список фраз.", rx + 16, messageListY + 8, 0xFF78859D);
        }

        inputFrame(ctx, rx + 11, messageY - 1, messageInput.getWidth() + 2, 22, messageInput.isFocused());
        button(ctx, rx + 19 + messageInput.getWidth(), messageY, rw - messageInput.getWidth() - 31, 20,
                "ДОБАВИТЬ", selectedNick != null, mx, my);

        ctx.drawTextWithShadow(textRenderer, "G — меню  •  TAB — статус  •  настройки сохраняются",
                px + 20, py + ph - 22, 0xFF74829B);
        ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(notice, 245),
                px + pw - 260, py + ph - 22, 0xFF9AA8C0);
        super.render(ctx, mx, my, delta);
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
        int color = enabled ? (over ? 0xFF5269D7 : 0xFF3B50B0) : (over ? 0xFF35435D : 0xFF253149);
        ctx.fill(x, y, x + w, y + h, color);
        ctx.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + (h - 8) / 2, 0xFFF5F7FF);
    }

    private boolean inside(double x, double y, int left, int top, int w, int h) {
        return x >= left && x < left + w && y >= top && y < top + h;
    }

    private int clamp(int value, int count, int visible) {
        return Math.max(0, Math.min(value, Math.max(0, count - visible)));
    }

    private void addNick() {
        String nick = nickInput.getText().trim();
        if (GreetConfig.addTarget(nick)) {
            selectedNick = nick;
            nickInput.setText("");
            messageInput.setText("");
            targetScroll = 0;
            messageScroll = 0;
            notice = "Ник добавлен. Впиши для него фразу.";
        } else {
            notice = "Ник неверный или уже добавлен.";
        }
    }

    private void addMessage() {
        if (selectedNick == null) {
            notice = "Сначала выбери ник.";
            return;
        }
        if (GreetConfig.addGreeting(selectedNick, messageInput.getText())) {
            messageInput.setText("");
            messageScroll = Math.max(0, GreetConfig.getGreetings(selectedNick).size() - 7);
            notice = "Сообщение сохранено для " + selectedNick;
        } else {
            notice = "Пустое, повторное сообщение или лимит 32.";
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;

        if (inside(mouseX, mouseY, px + pw - 132, py + 18, 112, 28)) {
            GreetConfig.setEnabled(!GreetConfig.isEnabled());
            notice = GreetConfig.isEnabled() ? "Автоприветствия включены." : "Автоприветствия выключены.";
            return true;
        }

        List<String> names = filteredTargets();
        int visible = Math.max(1, Math.min(7, (nickY - targetY - 6) / 25));
        for (int i = 0; i < visible && targetScroll + i < names.size(); i++) {
            int y = targetY + i * 25;
            if (inside(mouseX, mouseY, lx + 8, y - 3, lw - 16, 23)) {
                selectedNick = names.get(targetScroll + i);
                messageInput.setText("");
                messageScroll = 0;
                notice = "Редактируешь фразы для " + selectedNick;
                return true;
            }
        }

        int addX = lx + 19 + nickInput.getWidth();
        if (inside(mouseX, mouseY, addX, nickY, 38, 20)) {
            addNick();
            return true;
        }
        int removeX = lx + 61 + nickInput.getWidth();
        if (inside(mouseX, mouseY, removeX, nickY, lw - nickInput.getWidth() - 81, 20)) {
            if (selectedNick != null && GreetConfig.removeTarget(selectedNick)) {
                notice = "Игрок и его фразы удалены.";
                selectedNick = null;
                messageInput.setText("");
            } else notice = "Сначала выбери игрока.";
            return true;
        }

        if (selectedNick != null) {
            List<String> messages = GreetConfig.getGreetings(selectedNick);
            int msgVisible = Math.max(1, Math.min(7, (messageY - messageListY - 6) / 25));
            for (int i = 0; i < msgVisible && messageScroll + i < messages.size(); i++) {
                int y = messageListY + i * 25;
                if (inside(mouseX, mouseY, rx + rw - 39, y - 3, 31, 23)) {
                    GreetConfig.removeGreeting(selectedNick, messageScroll + i);
                    notice = "Фраза удалена.";
                    return true;
                }
            }
            int msgAddX = rx + 19 + messageInput.getWidth();
            if (inside(mouseX, mouseY, msgAddX, messageY, rw - messageInput.getWidth() - 31, 20)) {
                addMessage();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (inside(mouseX, mouseY, lx, ly, lw, paneH)) {
            targetScroll = Math.max(0, targetScroll + (vertical < 0 ? 1 : -1));
            return true;
        }
        if (inside(mouseX, mouseY, rx, ly, rw, paneH)) {
            messageScroll = Math.max(0, messageScroll + (vertical < 0 ? 1 : -1));
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
                addMessage();
                return true;
            }
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
