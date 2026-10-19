package com.example.xptable.client;

import com.example.xptable.Net;
import com.example.xptable.Xp;
import com.example.xptable.XpTableMenu;
import com.example.xptable.XpTableMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

/**
 * My drawings: the green orb background, and three pages
 * (main: XP stored / Deposit / Withdraw / Claim, then the Deposit and Withdraw pages).
 */
public class XpTableScreen extends AbstractContainerScreen<XpTableMenu> {
    // Blocky pixel art (tools/make_gui.py), one per page so the orbs sit differently. 1 texel = 2 GUI pixels.
    // The inner green box sits at (30,36), 220x128.
    private static final ResourceLocation BG_MAIN = tex("bg_main"), BG_DEPOSIT = tex("bg_deposit"), BG_WITHDRAW = tex("bg_withdraw");
    private static final int BG_W = 288, BG_H = 216, TEX_W = 144, TEX_H = 108;
    private static final int BOX_X = 30, BOX_Y = 36, BOX_W = 220;

    static final int OLIVE = 0xFF37530E;          // colours from my drawing: outline
    static final int PANEL = 0xFFC9F5A0;           // glow, middle
    static final int PANEL_HOVER = 0xFFC9FFA0;     // glow, brightest
    static final int PANEL_OFF = 0xFFC9E5A0;       // glow, outer
    static final int TEXT_OFF = 0xFF5A9916;

    private enum Page { MAIN, DEPOSIT, WITHDRAW }
    private Page page = Page.MAIN;
    /** Levels or points, remembered while the game runs. */
    private static boolean levels = true;

    private EditBox custom;
    private OrbButton depositBtn, withdrawBtn, claimBtn, toggleBtn;
    private OrbButton[] amountBtns = new OrbButton[0];

    public XpTableScreen(XpTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = BG_W;
        imageHeight = BG_H;
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(XpTableMod.MODID, "textures/gui/" + name + ".png");
    }

    private int bx() { return leftPos + BOX_X; }
    private int by() { return topPos + BOX_Y; }

    @Override
    protected void init() {
        super.init();
        build();
    }

    private void show(Page p) {
        page = p;
        build();
    }

    private void build() {
        clearWidgets();
        custom = null;
        depositBtn = withdrawBtn = claimBtn = toggleBtn = null;
        amountBtns = new OrbButton[0];
        int x = bx(), y = by();
        switch (page) {
            case MAIN -> {
                depositBtn = addRenderableWidget(new OrbButton(x + 8, y + 44, 98, 22, Component.literal("Deposit"), b -> show(Page.DEPOSIT)));
                withdrawBtn = addRenderableWidget(new OrbButton(x + 114, y + 44, 98, 22, Component.literal("Withdraw"), b -> show(Page.WITHDRAW)));
                claimBtn = addRenderableWidget(new OrbButton(x + 70, y + 76, 80, 18, Component.literal("Claim"), b -> claimPressed()));
            }
            case DEPOSIT -> {
                addBack(x, y);
                amountBtns = amountRow(x, y + 18, "+", XpTableMenu.DEPOSIT);
                toggleBtn = addToggle(x, y + 44);
                custom = addCustom(x, y + 63);
            }
            case WITHDRAW -> {
                addBack(x, y);
                custom = addCustom(x, y + 40);
                toggleBtn = addToggle(x, y + 66);
                amountBtns = amountRow(x, y + 85, "-", XpTableMenu.WITHDRAW);
            }
        }
    }

    private void addBack(int x, int y) {
        addRenderableWidget(new OrbButton(x + 3, y + 3, 18, 13, Component.literal("<"), b -> show(Page.MAIN)));
    }

    private OrbButton[] amountRow(int x, int y, String sign, int action) {
        return new OrbButton[] {
                addRenderableWidget(new OrbButton(x + 8, y, 64, 20, Component.literal(sign + "1"), b -> send(action, 1))),
                addRenderableWidget(new OrbButton(x + 78, y, 64, 20, Component.literal(sign + "10"), b -> send(action, 10))),
                addRenderableWidget(new OrbButton(x + 148, y, 64, 20, Component.literal("All"), b -> send(action, XpTableMenu.ALL))),
        };
    }

    private OrbButton addToggle(int x, int y) {
        return addRenderableWidget(new OrbButton(x + 70, y, 80, 14, toggleLabel(), b -> {
            levels = !levels;
            b.setMessage(toggleLabel());
        }));
    }

    private static Component toggleLabel() {
        return Component.literal(levels ? "Levels" : "Points");
    }

    private EditBox addCustom(int x, int y) {
        EditBox e = new EditBox(font, x + 14, y + 6, 192, 10, Component.literal("Custom amount"));
        e.setBordered(false);
        e.setMaxLength(12);
        e.setFilter(s -> s.chars().allMatch(Character::isDigit));
        e.setTextColor(OLIVE);
        e.setHint(Component.literal("Custom amount, press Enter").withStyle(s -> s.withColor(TEXT_OFF & 0xFFFFFF)));
        return addRenderableWidget(e);
    }

    private void send(int action, long amount) {
        Net.toServer(new Net.Action(action, amount, levels));
    }

    private void claimPressed() {
        Net.State s = menu.state;
        if (!s.claimed()) Net.toServer(new Net.Action(XpTableMenu.CLAIM, 0, false));
        else if (s.canUnclaim()) Net.toServer(new Net.Action(XpTableMenu.UNCLAIM, 0, false));
    }

    private void submitCustom() {
        if (custom == null || custom.getValue().isEmpty()) return;
        long n;
        try { n = Long.parseLong(custom.getValue()); } catch (NumberFormatException ex) { return; }
        if (n <= 0) return;
        send(page == Page.DEPOSIT ? XpTableMenu.DEPOSIT : XpTableMenu.WITHDRAW, n);
        custom.setValue("");
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE && page != Page.MAIN) {
            show(Page.MAIN);
            return true;
        }
        if (custom != null && custom.isFocused()) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                submitCustom();
                return true;
            }
            if (key != GLFW.GLFW_KEY_ESCAPE) {
                custom.keyPressed(key, scan, mods);
                return true; // so typing "e" doesn't close the menu
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    protected void containerTick() {
        Net.State s = menu.state;
        boolean use = s.canUse();
        if (depositBtn != null) depositBtn.active = use;
        if (withdrawBtn != null) withdrawBtn.active = use;
        if (claimBtn != null) {
            claimBtn.setMessage(Component.literal(!s.claimed() ? "Claim" : s.canUnclaim() ? "Unclaim" : "Claimed"));
            claimBtn.active = !s.claimed() || s.canUnclaim();
        }
        for (OrbButton b : amountBtns) b.active = use;
        if (custom != null) {
            custom.setEditable(use);
            custom.tick();
        }
        // Someone claimed it while we were on a sub-page: back to the main page.
        if (!use && page != Page.MAIN) show(Page.MAIN);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        ResourceLocation bg = page == Page.MAIN ? BG_MAIN : page == Page.DEPOSIT ? BG_DEPOSIT : BG_WITHDRAW;
        g.blit(bg, leftPos, topPos, BG_W, BG_H, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
        int x = bx(), y = by();
        Net.State s = menu.state;
        switch (page) {
            case MAIN -> {
                panel(g, x + 20, y + 6, 180, 30);
                center(g, "XP Stored", x + BOX_W / 2, y + 10);
                center(g, amount(s.xp()), x + BOX_W / 2, y + 23);
                String who = !s.claimed() ? "Unclaimed: anyone can use it"
                        : s.isOwner() ? "Claimed by you (" + s.ownerName() + ")"
                        : "Claimed by " + s.ownerName();
                center(g, who, x + BOX_W / 2, y + 104);
            }
            case DEPOSIT -> {
                center(g, "Deposit", x + BOX_W / 2, y + 5);
                panel(g, x + 10, y + 63, 200, 20);
                totalLine(g, x + 10, y + 88, s);
                center(g, you(), x + BOX_W / 2, y + 113);
            }
            case WITHDRAW -> {
                center(g, "Withdraw", x + BOX_W / 2, y + 5);
                totalLine(g, x + 10, y + 18, s);
                panel(g, x + 10, y + 40, 200, 20);
                center(g, you(), x + BOX_W / 2, y + 112);
            }
        }
    }

    /** "Total XP" bar from the drawing, plus what moved since the menu opened. */
    private void totalLine(GuiGraphics g, int x, int y, Net.State s) {
        panel(g, x, y, 200, 18);
        g.drawString(font, "Total XP: " + amount(s.xp()), x + 5, y + 5, OLIVE, false);
        if (s.moved() != 0) {
            String m = (s.moved() > 0 ? "+" : "-") + String.format("%,d", Math.abs(s.moved()));
            g.drawString(font, m, x + 195 - font.width(m), y + 5, OLIVE, false);
        }
    }

    private String you() {
        var p = minecraft.player;
        if (p == null) return "";
        long pts = Xp.totalFor(p.experienceLevel) + Math.round(p.experienceProgress * Xp.needed(p.experienceLevel));
        return String.format("You: Level %,d (%,d points)", p.experienceLevel, pts);
    }

    private static String amount(long pts) {
        return String.format("Lv %,d  (%,d pts)", Xp.levelFor(pts), pts);
    }

    private void center(GuiGraphics g, String text, int cx, int y) {
        g.drawString(font, text, cx - font.width(text) / 2, y, OLIVE, false);
    }

    static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, OLIVE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
    }

    // The default draws "XP Table" and "Inventory" in grey; the pages draw their own text.
    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {}

    /** Flat button with the drawing's dark outline. */
    static class OrbButton extends Button {
        OrbButton(int x, int y, int w, int h, Component msg, OnPress press) {
            super(x, y, w, h, msg, press, DEFAULT_NARRATION);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            int fill = !active ? PANEL_OFF : isHoveredOrFocused() ? PANEL_HOVER : PANEL;
            g.fill(getX(), getY(), getX() + width, getY() + height, active ? OLIVE : TEXT_OFF);
            g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, fill);
            var font = net.minecraft.client.Minecraft.getInstance().font;
            int tw = font.width(getMessage());
            g.drawString(font, getMessage(), getX() + (width - tw) / 2, getY() + (height - 8) / 2,
                    active ? OLIVE : TEXT_OFF, false);
        }
    }
}
