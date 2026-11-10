package pl.nks;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class NksMod implements ClientModInitializer {
    private static boolean openGui = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            // GUI otwieramy w nastepnym ticku, zeby ChatScreen go nie zamknal
            if (openGui && mc.currentScreen == null && mc.player != null) {
                openGui = false;
                mc.setScreen(new SellScreen());
            }
            Bot.tick(mc);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("nks-sell").executes(ctx -> {
                openGui = true;
                return 1;
            }));

            dispatcher.register(ClientCommandManager.literal("nks-start").executes(ctx -> {
                Bot.start(MinecraftClient.getInstance());
                return 1;
            }));

            dispatcher.register(ClientCommandManager.literal("nks-stop").executes(ctx -> {
                Bot.stop(MinecraftClient.getInstance(), "Zatrzymano komendą.");
                return 1;
            }));

            dispatcher.register(ClientCommandManager.literal("nks-skrzynia").executes(ctx -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK
                        && mc.crosshairTarget instanceof BlockHitResult hit) {
                    BlockPos pos = hit.getBlockPos();
                    Config c = Config.INSTANCE;
                    c.hasChest = true;
                    c.chestX = pos.getX();
                    c.chestY = pos.getY();
                    c.chestZ = pos.getZ();
                    c.save();
                    ctx.getSource().sendFeedback(Text.literal("§6[NKS] §aUstawiono skrzynię: "
                            + pos.getX() + " " + pos.getY() + " " + pos.getZ()));
                } else {
                    ctx.getSource().sendFeedback(Text.literal("§6[NKS] §cPatrz na skrzynię i wpisz komendę ponownie."));
                }
                return 1;
            }));

            // /nks weebhook <url>   (dzialaja tez /nks webhook <url>)
            dispatcher.register(ClientCommandManager.literal("nks")
                    .then(webhookNode("weebhook"))
                    .then(webhookNode("webhook")));
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> webhookNode(String name) {
        return ClientCommandManager.literal(name)
                .then(ClientCommandManager.argument("url", StringArgumentType.greedyString()).executes(ctx -> {
                    String url = StringArgumentType.getString(ctx, "url").trim();
                    Config.INSTANCE.webhook = url;
                    Config.INSTANCE.save();
                    ctx.getSource().sendFeedback(Text.literal("§6[NKS] §aZapisano webhook Discord."));
                    Webhook.send("✅ Webhook połączony z NKS Sell.");
                    return 1;
                }));
    }
}
