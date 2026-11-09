package ru.governix.greet.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.governix.greet.config.GreetConfig;

import java.util.ArrayList;
import java.util.List;

public final class WheelScreen extends Screen {
    private static final int ITEMS_PER_PAGE = 8;
    private static final int NODE_WIDTH = 112;
    private static final int NODE_HEIGHT = 40;

    private final Screen parent;
    private String selectedPlayer;
    private int page;
    private String notice = "";

    private int centerX;
    private int centerY;
    private int radius;
    private int itemCount;
    private int hovered = -1;

    public WheelScreen(Screen parent) {
        super(Text.literal("GreetMate — Quick Wheel"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        centerX = width / 2;
        centerY = height / 2;
        radius = Math.min(150, Math.max(92, Math.min(width, height) / 4));
    }

    private boolean choosingPlayers() {
        return selectedPlayer == null;
    }

    private List<String> currentItems() {
        if (choosingPlayers()) return WelcomeTracker.getOnlineTargets();
        return GreetConfig.getGreetings(selectedPlayer);
    }

    private int pageCount() {
        return Math.max(1, (currentItems().size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
    }

    private int pageStart() {
        return page * ITEMS_PER_PAGE;
    }

    private int currentItemCount() {
        return Math.min(ITEMS_PER_PAGE, Math.max(0, currentItems().size() - pageStart()));
    }

    private int nodeX(int index, int count) {
        double angle = -Math.PI / 2.0 + (Math.PI * 2.0 * index / count);
        return centerX + (int) Math.round(Math.cos(angle) * radius);
    }

    private int nodeY(int index, int count) {
        double angle = -Math.PI / 2.0 + (Math.PI * 2.0 * index / count);
        return centerY + (int) Math.round(Math.sin(angle) * radius);
    }

    private boolean inside(double x, double y, int cx, int cy, int w, int h) {
        return x >= cx - w / 2.0 && x <= cx + w / 2.0
                && y >= cy - h / 2.0 && y <= cy + h / 2.0;
    }

    private int hitNode(double mouseX, double mouseY) {
        int count = currentItemCount();
        for (int i = 0; i < count; i++) {
            if (inside(mouseX, mouseY, nodeX(i, count), nodeY(i, count), NODE_WIDTH, NODE_HEIGHT)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xC9080D18);

        drawCircle(ctx, centerX, centerY, radius + 82, 0xE90D1423);
        drawCircle(ctx, centerX, centerY, radius + 78, 0xFF334362);
        drawCircle(ctx, centerX, centerY, radius + 74, 0xF20F1727);
        drawCircle(ctx, centerX, centerY, radius + 4, 0xFF263652);
        drawCircle(ctx, centerX, centerY, radius, 0xFF111A2A);

        String heading = choosingPlayers() ? "Кому пишем?" : "Приветствие для " + selectedPlayer;
        ctx.drawCenteredTextWithShadow(textRenderer, heading, centerX, centerY - radius - 68, 0xFFF1F5FF);
        String subtitle = choosingPlayers()
                ? "Выбери игрока из тех, кто сейчас в TAB"
                : "Выбери свою сохранённую фразу";
        ctx.drawCenteredTextWithShadow(textRenderer, subtitle, centerX, centerY - radius - 50, 0xFF9BAAC2);

        List<String> items = currentItems();
        page = Math.max(0, Math.min(page, pageCount() - 1));
        itemCount = currentItemCount();
        hovered = hitNode(mouseX, mouseY);

        if (items.isEmpty()) {
            // Empty-state text is drawn below the center hub, after the hub itself.
        } else {
            for (int i = 0; i < itemCount; i++) {
                String value = items.get(pageStart() + i);
                int x = nodeX(i, itemCount);
                int y = nodeY(i, itemCount);
                boolean active = i == hovered;
                drawPill(ctx, x, y, NODE_WIDTH, NODE_HEIGHT,
                        active ? 0xFF536FE0 : 0xFF202D45,
                        active ? 0xFF9EADFF : 0xFF3A4966);
                String label = textRenderer.trimToWidth(value, NODE_WIDTH - 14);
                ctx.drawCenteredTextWithShadow(textRenderer, label, x, y - 10, 0xFFF3F6FF);
                if (choosingPlayers()) {
                    ctx.drawCenteredTextWithShadow(textRenderer, "В TAB", x, y + 2, 0xFF9FB4D7);
                } else {
                    ctx.drawCenteredTextWithShadow(textRenderer, "ОТПРАВИТЬ", x, y + 2, 0xFF9FB4D7);
                }
            }
        }

        drawCircle(ctx, centerX, centerY, 57, 0xFF7184EA);
        drawCircle(ctx, centerX, centerY, 53, 0xFF121B2B);
        if (choosingPlayers()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "GREET", centerX, centerY - 10, 0xFFF2F5FF);
            ctx.drawCenteredTextWithShadow(textRenderer, "MATE", centerX, centerY + 2, 0xFF8FA2FF);
        } else {
            ctx.drawCenteredTextWithShadow(textRenderer, textRenderer.trimToWidth(selectedPlayer, 90),
                    centerX, centerY - 4, 0xFFF2F5FF);
            ctx.drawCenteredTextWithShadow(textRenderer, "НАЗАД", centerX, centerY + 12, 0xFF9CAAC1);
        }

        if (items.isEmpty()) {
            drawEmptyState(ctx, choosingPlayers());
        }

        if (hovered >= 0 && !items.isEmpty() && !choosingPlayers()) {
            String fullText = items.get(pageStart() + hovered);
            ctx.drawCenteredTextWithShadow(textRenderer,
                    textRenderer.trimToWidth(fullText, Math.max(120, width - 80)),
                    centerX, centerY + radius + 42, 0xFFC7D2E8);
        } else if (!notice.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, notice, centerX, centerY + radius + 42, 0xFF9EAECA);
        }

        if (pageCount() > 1) {
            drawPill(ctx, centerX - 78, centerY + radius + 66, 48, 24, 0xFF202D45, 0xFF465779);
            drawPill(ctx, centerX + 78, centerY + radius + 66, 48, 24, 0xFF202D45, 0xFF465779);
            ctx.drawCenteredTextWithShadow(textRenderer, "‹", centerX - 78, centerY + radius + 58, 0xFFF2F5FF);
            ctx.drawCenteredTextWithShadow(textRenderer, "›", centerX + 78, centerY + radius + 58, 0xFFF2F5FF);
            ctx.drawCenteredTextWithShadow(textRenderer, (page + 1) + " / " + pageCount(),
                    centerX, centerY + radius + 59, 0xFF9EADC6);
        }

        ctx.drawCenteredTextWithShadow(textRenderer, "Мышь — выбрать  •  Колесо — страницы  •  ESC — назад",
                centerX, height - 22, 0xFF7F8DA5);
        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawEmptyState(DrawContext ctx, boolean choosingPlayers) {
        String[] lines = choosingPlayers
                ? new String[] {
                    "Нет отслеживаемых игроков онлайн",
                    "Добавь ник через меню G",
                    "и дождись его появления в TAB"
                }
                : new String[] {
                    "Для этого игрока пока нет фраз",
                    "Добавь свои фразы",
                    "в меню настроек G"
                };

        int maxTextWidth = Math.max(120, Math.min(width - 40, radius * 2 - 24));
        int firstY = centerY + 70;
        for (int i = 0; i < lines.length; i++) {
            String line = textRenderer.trimToWidth(lines[i], maxTextWidth);
            int color = i == 0 ? 0xFFB6C2D6 : 0xFF8998B2;
            ctx.drawCenteredTextWithShadow(textRenderer, line, centerX, firstY + i * 14, color);
        }
    }

    private void drawCircle(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int y = -r; y <= r; y++) {
            int half = (int) Math.sqrt((double) r * r - (double) y * y);
            ctx.fill(cx - half, cy + y, cx + half + 1, cy + y + 1, color);
        }
    }

    private void drawPill(DrawContext ctx, int cx, int cy, int w, int h, int fill, int border) {
        int halfW = w / 2;
        int halfH = h / 2;
        for (int y = -halfH; y <= halfH; y++) {
            int inset = Math.max(0, halfH - Math.abs(y) - 5);
            int left = cx - halfW + inset;
            int right = cx + halfW - inset;
            ctx.fill(left, cy + y, right, cy + y + 1, border);
        }
        for (int y = -halfH + 2; y <= halfH - 2; y++) {
            int inset = Math.max(0, halfH - Math.abs(y) - 5);
            int left = cx - halfW + inset + 2;
            int right = cx + halfW - inset - 2;
            ctx.fill(left, cy + y, right, cy + y + 1, fill);
        }
    }

    private void changePage(int delta) {
        page = Math.max(0, Math.min(pageCount() - 1, page + delta));
        notice = "";
    }

    private void chooseCurrent() {
        List<String> items = currentItems();
        if (hovered < 0 || hovered >= currentItemCount()) return;
        String selected = items.get(pageStart() + hovered);

        if (choosingPlayers()) {
            selectedPlayer = selected;
            page = 0;
            notice = "";
            hovered = -1;
            return;
        }

        if (WelcomeTracker.sendManualGreeting(selectedPlayer, selected)) {
            close();
        } else {
            notice = "Игрок больше не отображается в TAB";
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (!choosingPlayers()) {
                selectedPlayer = null;
                page = 0;
                notice = "";
            } else {
                close();
            }
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        if (!choosingPlayers() && Math.hypot(mouseX - centerX, mouseY - centerY) <= 58) {
            selectedPlayer = null;
            page = 0;
            notice = "";
            return true;
        }

        int navY = centerY + radius + 66;
        if (pageCount() > 1 && inside(mouseX, mouseY, centerX - 78, navY, 48, 24)) {
            changePage(-1);
            return true;
        }
        if (pageCount() > 1 && inside(mouseX, mouseY, centerX + 78, navY, 48, 24)) {
            changePage(1);
            return true;
        }

        hovered = hitNode(mouseX, mouseY);
        if (hovered >= 0) {
            chooseCurrent();
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (vertical != 0 && pageCount() > 1) {
            changePage(vertical < 0 ? 1 : -1);
            return true;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!choosingPlayers()) {
                selectedPlayer = null;
                page = 0;
                notice = "";
            } else {
                close();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            changePage(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            changePage(1);
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
