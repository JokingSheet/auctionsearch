package com.auctionsniper;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

/**
 * Oeffnet /ah nur wenn kein GUI offen ist und schliesst NUR das Fenster, das als Auktionshaus
 * erkannt wurde und von diesem Auto-Scan geoeffnet wurde. Fremde GUIs werden nie geschlossen.
 */
public final class AutoScan {
    private enum State { COOLDOWN, WAIT_OPEN, SCANNING }

    private State state = State.COOLDOWN;
    private int timer = 60;
    private AbstractContainerScreen<?> ahScreen;

    public void reset() {
        state = State.COOLDOWN;
        timer = 60;
        ahScreen = null;
    }

    public String status() {
        if (!SniperConfig.get().autoScan) return "Auto-Scan: aus";
        return switch (state) {
            case COOLDOWN -> "Auto-Scan: wartet (" + Math.max(0, timer / 20) + "s)";
            case WAIT_OPEN -> "Auto-Scan: öffne /ah ...";
            case SCANNING -> "Auto-Scan: scanne ...";
        };
    }

    public void tick(Minecraft mc) {
        SniperConfig cfg = SniperConfig.get();
        if (!cfg.autoScan || mc.player == null || mc.level == null) {
            if (state != State.COOLDOWN) reset();
            return;
        }
        switch (state) {
            case COOLDOWN -> {
                if (timer > 0) timer--;
                if (timer <= 0 && mc.screen == null) {
                    mc.player.connection.sendCommand("ah");
                    state = State.WAIT_OPEN;
                    timer = 0;
                }
            }
            case WAIT_OPEN -> {
                timer++;
                if (mc.screen instanceof AbstractContainerScreen<?> acs && AuctionScanner.isChest(acs)) {
                    if (AuctionScanner.isAuctionHouse(acs)) {
                        begin(acs);
                        return;
                    }
                    // Titel unbekannt: da wir gerade selbst /ah gesendet haben und das Fenster Preise
                    // enthaelt, lernen wir den Titel einmalig.
                    String title = acs.getTitle().getString().trim();
                    if (!title.isEmpty() && !AuctionScanner.scan(mc, acs).isEmpty()) {
                        cfg.learnedTitles.add(title);
                        cfg.save();
                        mc.player.displayClientMessage(Component.literal(
                                "[Auction Sniper] Auktionshaus-Titel gelernt: \"" + title + "\"")
                                .withStyle(ChatFormatting.YELLOW), false);
                        begin(acs);
                        return;
                    }
                }
                if (timer > 100) finish(cfg); // nichts erkannt -> nichts schliessen
            }
            case SCANNING -> {
                timer++;
                if (mc.screen != ahScreen) { // Spieler hat gewechselt/geschlossen
                    finish(cfg);
                    return;
                }
                if (timer >= cfg.closeAfterTicks) {
                    if (ahScreen != null && AuctionScanner.isAuctionHouse(ahScreen)) {
                        mc.player.closeContainer();
                    }
                    finish(cfg);
                }
            }
        }
    }

    private void begin(AbstractContainerScreen<?> acs) {
        ahScreen = acs;
        state = State.SCANNING;
        timer = 0;
    }

    private void finish(SniperConfig cfg) {
        ahScreen = null;
        state = State.COOLDOWN;
        timer = Math.max(1, cfg.cooldownSeconds) * 20;
    }
}
