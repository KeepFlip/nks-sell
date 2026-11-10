package pl.nks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class Bot {
    private enum State { CHECK, WAIT_CHEST, SELL }

    public static boolean running = false;
    private static State state = State.CHECK;
    private static int cooldown = 0;
    private static int timeout = 0;
    private static int takeClicks = 0;
    private static boolean savedPause = true;

    public static void start(MinecraftClient mc) {
        Config c = Config.INSTANCE;
        if (item() == Items.AIR) {
            msg(mc, "§cNie wybrano przedmiotu. Użyj /nks-sell");
            return;
        }
        running = true;
        state = State.CHECK;
        cooldown = 0;
        savedPause = mc.options.pauseOnLostFocus;
        mc.options.pauseOnLostFocus = false; // pozwala zminimalizowac okno
        msg(mc, "§aBot uruchomiony: " + c.amount + "x " + c.item + " za " + c.price);
        Webhook.send("▶️ Bot uruchomiony: " + c.amount + "x " + c.item + " za " + c.price);
    }

    public static void stop(MinecraftClient mc, String reason) {
        if (!running) {
            msg(mc, "§eBot nie był uruchomiony.");
            return;
        }
        running = false;
        mc.options.pauseOnLostFocus = savedPause;
        if (mc.player != null && mc.currentScreen instanceof GenericContainerScreen) mc.player.closeHandledScreen();
        msg(mc, "§cBot zatrzymany. " + reason);
        Webhook.send("⏹️ Bot zatrzymany. " + reason);
    }

    public static Item item() {
        Identifier id = Identifier.tryParse(Config.INSTANCE.item);
        return id == null ? Items.AIR : Registries.ITEM.get(id);
    }

    private static void msg(MinecraftClient mc, String s) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("§6[NKS] §r" + s), false);
    }

    private static int count(ClientPlayerEntity p, Item item) {
        int c = 0;
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) c += s.getCount();
        }
        return c;
    }

    /** Wkłada najwiekszy stack przedmiotu do reki. */
    private static boolean ensureHeld(MinecraftClient mc, ClientPlayerEntity p, Item item) {
        PlayerInventory inv = p.getInventory();
        int best = -1, bestCount = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item) && s.getCount() > bestCount) {
                best = i;
                bestCount = s.getCount();
            }
        }
        if (best < 0) return false;
        if (best < 9) {
            inv.selectedSlot = best;
        } else {
            mc.interactionManager.clickSlot(0, best, inv.selectedSlot, SlotActionType.SWAP, p);
        }
        return true;
    }

    public static void tick(MinecraftClient mc) {
        if (!running) return;
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || mc.world == null) {
            running = false;
            return;
        }
        // zminimalizowane / nieaktywne okno: zamknij menu pauzy, zeby bot nie stal
        if (!mc.isWindowFocused() && mc.currentScreen instanceof GameMenuScreen) {
            mc.setScreen(null);
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        Config cfg = Config.INSTANCE;
        Item item = item();

        switch (state) {
            case CHECK -> {
                if (mc.currentScreen != null) return; // czekamy az gracz zamknie okno
                if (count(p, item) >= cfg.amount) {
                    if (ensureHeld(mc, p, item)) {
                        state = State.SELL;
                        cooldown = 4;
                    }
                    return;
                }
                // brak przedmiotow w ekwipunku -> skrzynia
                if (!cfg.hasChest) {
                    stop(mc, "Brak przedmiotów i nie ustawiono skrzyni (/nks-skrzynia).");
                    return;
                }
                BlockPos pos = new BlockPos(cfg.chestX, cfg.chestY, cfg.chestZ);
                if (p.getEyePos().distanceTo(Vec3d.ofCenter(pos)) > 4.5) {
                    stop(mc, "Skrzynia jest za daleko.");
                    return;
                }
                BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
                mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, hit);
                state = State.WAIT_CHEST;
                timeout = 60;
                takeClicks = 0;
            }
            case WAIT_CHEST -> {
                if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
                    if (--timeout <= 0) stop(mc, "Nie udało się otworzyć skrzyni.");
                    return;
                }
                GenericContainerScreenHandler handler = screen.getScreenHandler();
                int size = handler.getRows() * 9;
                int need = cfg.amount - count(p, item);

                int found = -1;
                if (need > 0) {
                    // szukamy w calej skrzyni - takze ze srodka
                    for (int i = 0; i < size; i++) {
                        if (handler.getSlot(i).getStack().isOf(item)) {
                            found = i;
                            break;
                        }
                    }
                }

                if (found >= 0 && takeClicks < 80) {
                    mc.interactionManager.clickSlot(handler.syncId, found, 0, SlotActionType.QUICK_MOVE, p);
                    takeClicks++;
                    cooldown = 1;
                    return;
                }

                p.closeHandledScreen();
                if (count(p, item) < cfg.amount) {
                    Webhook.send("⚠️ Skończyły się przedmioty w skrzyni (" + cfg.item + ").");
                    stop(mc, "Brak wystarczającej ilości przedmiotów w skrzyni.");
                    return;
                }
                state = State.CHECK;
                cooldown = 10;
            }
            case SELL -> {
                p.networkHandler.sendChatCommand(cfg.sellCommand + " " + cfg.amount + " " + cfg.price);
                Webhook.send("💰 Wystawiono " + cfg.amount + "x " + cfg.item + " za " + cfg.price);
                state = State.CHECK;
                cooldown = Math.max(5, cfg.delayTicks);
            }
        }
    }
}
