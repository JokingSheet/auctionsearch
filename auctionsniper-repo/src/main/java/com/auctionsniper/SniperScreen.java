package com.auctionsniper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class SniperScreen extends Screen {
    private static final int C_TITLE = 0xFFFFD866;
    private static final int C_LABEL = 0xFFB8B8C8;
    private static final int C_TEXT = 0xFFFFFFFF;
    private static final int C_GOOD = 0xFF55FF55;
    private static final int C_BAD = 0xFFFF5555;
    private static final int PANEL_H = 206;

    private EditBox filterBox;
    private EditBox priceBox;
    private Button alarmBtn, encBtn, autoBtn, clearBtn, closeBtn;
    private final Button[] sugBtns = new Button[5];
    private final String[] sugText = new String[5];
    private boolean suppressSuggestions;
    private boolean priceValid = true;
    private int left, top, leftW, rightX, rightW;

    public SniperScreen() {
        super(Component.literal("Auction Sniper"));
    }

    @Override
    protected void init() {
        SniperConfig cfg = SniperConfig.get();
        leftW = Math.max(120, Math.min(190, width / 2 - 14));
        left = 8;
        top = Math.max(4, (height - PANEL_H) / 2);
        rightX = left + leftW + 12;
        rightW = Math.max(40, width - rightX - 8);
        int hw = (leftW - 4) / 2;

        filterBox = new EditBox(font, left, top + 24, leftW, 16, Component.literal("Item-Filter"));
        filterBox.setMaxLength(64);
        filterBox.setValue(cfg.itemFilter);
        filterBox.setHint(Component.literal("z.B. gil, mac, dia"));
        filterBox.setResponder(this::onFilterChanged);
        addRenderableWidget(filterBox);

        for (int i = 0; i < sugBtns.length; i++) {
            final int idx = i;
            sugBtns[i] = Button.builder(Component.empty(), b -> pickSuggestion(idx))
                    .bounds(left, top + 42 + i * 12, leftW, 12).build();
            sugBtns[i].visible = false;
            addRenderableWidget(sugBtns[i]);
        }

        priceBox = new EditBox(font, left, top + 116, leftW, 16, Component.literal("Max. Stückpreis"));
        priceBox.setMaxLength(24);
        priceBox.setValue(cfg.maxPriceText);
        priceBox.setHint(Component.literal("z.B. 700K, 1.5M, 2B"));
        priceBox.setResponder(this::onPriceChanged);
        addRenderableWidget(priceBox);
        priceValid = cfg.maxPriceText.isBlank() || PriceParser.parse(cfg.maxPriceText) >= 0;

        alarmBtn = Button.builder(Component.empty(), b -> {
            cfg.alarmEnabled = !cfg.alarmEnabled;
            cfg.markDirty();
            refreshButtons();
        }).bounds(left, top + 138, hw, 18).build();
        encBtn = Button.builder(Component.empty(), b -> {
            cfg.withEnchants = !cfg.withEnchants;
            cfg.markDirty();
            refreshButtons();
        }).bounds(left + hw + 4, top + 138, hw, 18).build();
        autoBtn = Button.builder(Component.empty(), b -> {
            cfg.autoScan = !cfg.autoScan;
            cfg.markDirty();
            refreshButtons();
        }).bounds(left, top + 160, hw, 18).build();
        String clearLabel = font.width("Angebote leeren") + 12 <= hw ? "Angebote leeren" : "Leeren";
        clearBtn = Button.builder(Component.literal(clearLabel), b -> AuctionSniperMod.clearAll())
                .bounds(left + hw + 4, top + 160, hw, 18).build();
        closeBtn = Button.builder(Component.literal("Schließen"), b -> onClose())
                .bounds(left, top + 182, leftW, 18).build();
        addRenderableWidget(alarmBtn);
        addRenderableWidget(encBtn);
        addRenderableWidget(autoBtn);
        addRenderableWidget(clearBtn);
        addRenderableWidget(closeBtn);
        refreshButtons();
        setInitialFocus(filterBox);
    }

    private void refreshButtons() {
        SniperConfig cfg = SniperConfig.get();
        alarmBtn.setMessage(Component.literal("Alarm: " + (cfg.alarmEnabled ? "AN" : "AUS")));
        encBtn.setMessage(Component.literal("Enchants: " + (cfg.withEnchants ? "MIT" : "OHNE")));
        autoBtn.setMessage(Component.literal("Auto-Scan: " + (cfg.autoScan ? "AN" : "AUS")));
    }

    private void onFilterChanged(String text) {
        SniperConfig cfg = SniperConfig.get();
        cfg.itemFilter = text;
        cfg.markDirty();
        List<String> sug = suppressSuggestions ? List.of() : Suggestions.suggest(text);
        for (int i = 0; i < sugBtns.length; i++) {
            if (i < sug.size()) {
                sugText[i] = sug.get(i);
                sugBtns[i].setMessage(Component.literal(sug.get(i)));
                sugBtns[i].visible = true;
            } else {
                sugText[i] = null;
                sugBtns[i].visible = false;
            }
        }
    }

    private void onPriceChanged(String text) {
        SniperConfig cfg = SniperConfig.get();
        cfg.maxPriceText = text;
        cfg.markDirty();
        priceValid = text.isBlank() || PriceParser.parse(text) >= 0;
    }

    private void pickSuggestion(int idx) {
        String chosen = sugText[idx];
        if (chosen == null) return;
        suppressSuggestions = true;
        filterBox.setValue(chosen); // loest den Responder aus -> Filter wird gespeichert
        suppressSuggestions = false;
        for (Button b : sugBtns) b.visible = false;
        setFocused(filterBox);
    }

    /** Bewusst leer: kein Blur, damit "can only blur once per frame" nicht auftreten kann. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xD0101018);
        g.fill(left - 4, top - 4, left + leftW + 4, top + PANEL_H - 2, 0xFF1E1E2C);
        g.fill(rightX - 4, top - 4, rightX + rightW + 4, top + PANEL_H - 2, 0xFF1E1E2C);

        super.render(g, mouseX, mouseY, partialTick);

        g.drawString(font, "Auction Sniper", left, top + 2, C_TITLE);
        g.drawString(font, "Item-Filter", left, top + 13, C_LABEL);
        g.drawString(font, "Max. Stückpreis", left, top + 105, priceValid ? C_LABEL : C_BAD);
        if (!priceValid) {
            String bad = "ungültig";
            g.drawString(font, bad, left + leftW - font.width(bad), top + 105, C_BAD);
        }

        drawRight(g);
    }

    private void drawRight(GuiGraphics g) {
        long now = System.currentTimeMillis();
        AuctionSniperMod.STATS.prune(now);
        List<Stats.GroupMin> mins = AuctionSniperMod.STATS.groupMins();
        int lh = 10;
        int y = top + 2;
        int bottom = top + PANEL_H - 14;

        g.drawString(font, "Statistik (letzte 10 Min.)", rightX, y, C_TITLE);
        y += lh + 2;
        g.drawString(font, "Angebote: " + AuctionSniperMod.STATS.listingCount()
                + "   Gruppen: " + mins.size(), rightX, y, C_TEXT);
        y += lh + 2;

        int avail = (bottom - y) / lh;
        int statLines = Math.min(mins.size(), Math.max(2, avail / 2 - 2));
        for (int i = 0; i < statLines; i++) {
            Stats.GroupMin m = mins.get(i);
            drawPriceLine(g, m.label(), PriceParser.format(m.minUnitPrice()) + "/Stk", y, C_LABEL);
            y += lh;
        }
        if (mins.size() > statLines) {
            g.drawString(font, "... +" + (mins.size() - statLines) + " weitere", rightX, y, C_LABEL);
            y += lh;
        }
        y += 4;

        g.drawString(font, "Günstigste Angebote (letzter Scan)", rightX, y, C_TITLE);
        y += lh + 2;
        List<Offer> offers = AuctionSniperMod.cheapest;
        if (offers.isEmpty()) {
            g.drawString(font, font.plainSubstrByWidth("Noch keine Daten – Auktionshaus öffnen.", rightW),
                    rightX, y, C_LABEL);
        }
        for (Offer o : offers) {
            if (y + lh > bottom) break;
            drawPriceLine(g, (o.alarmMatch ? "! " : "") + o.displayName(),
                    PriceParser.format(o.unitPrice) + "/Stk", y, o.alarmMatch ? C_GOOD : C_TEXT);
            y += lh;
        }

        g.drawString(font, font.plainSubstrByWidth(AuctionSniperMod.AUTO.status(), rightW),
                rightX, top + PANEL_H - 14, C_LABEL);
    }

    private void drawPriceLine(GuiGraphics g, String label, String price, int y, int color) {
        int pw = font.width(price);
        g.drawString(font, price, rightX + rightW - pw, y, color);
        g.drawString(font, font.plainSubstrByWidth(label, Math.max(10, rightW - pw - 6)), rightX, y, color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        SniperConfig.get().save();
        super.onClose();
    }
}
