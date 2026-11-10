package pl.nks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

public class SellScreen extends Screen {
    private TextFieldWidget amountField;
    private TextFieldWidget priceField;
    private String selectedId = Config.INSTANCE.item;
    private int gridX, gridY;

    public SellScreen() {
        super(Text.literal("NKS Sell"));
    }

    @Override
    protected void init() {
        gridX = width / 2 - 81;
        gridY = height / 2 - 60;

        amountField = new TextFieldWidget(textRenderer, gridX, gridY + 100, 70, 18, Text.literal("Ilość"));
        amountField.setTextPredicate(s -> s.matches("\\d{0,9}"));
        amountField.setText(String.valueOf(Config.INSTANCE.amount));
        addDrawableChild(amountField);

        priceField = new TextFieldWidget(textRenderer, gridX + 80, gridY + 100, 82, 18, Text.literal("Cena"));
        priceField.setTextPredicate(s -> s.matches("\\d{0,9}"));
        priceField.setText(String.valueOf(Config.INSTANCE.price));
        addDrawableChild(priceField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Zapisz"), b -> save())
                .dimensions(gridX, gridY + 126, 162, 20).build());
    }

    /** Slot ekwipunku wyswietlany w kratce (rzad, kolumna): 3 rzedy glownego + hotbar na dole. */
    private int invIndex(int row, int col) {
        return row < 3 ? 9 + row * 9 + col : col;
    }

    private void save() {
        Config c = Config.INSTANCE;
        try {
            int a = Integer.parseInt(amountField.getText());
            int p = Integer.parseInt(priceField.getText());
            if (a < 1 || p < 1) throw new NumberFormatException();
            c.amount = a;
            c.price = p;
        } catch (NumberFormatException e) {
            if (client.player != null)
                client.player.sendMessage(Text.literal("§6[NKS] §cPodaj poprawną ilość i cenę."), false);
            return;
        }
        c.item = selectedId;
        c.save();
        if (client.player != null)
            client.player.sendMessage(Text.literal("§6[NKS] §aZapisano: /wystawy " + c.amount + " " + c.price
                    + " (" + c.item + ")"), false);
        close();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("NKS Sell - wybierz przedmiot do sprzedaży"),
                width / 2, gridY - 30, 0xFFFFFF);

        ItemStack selStack = new ItemStack(Registries.ITEM.get(net.minecraft.util.Identifier.tryParse(selectedId)));
        ctx.drawItem(selStack, gridX, gridY - 18);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Wybrano: " + selectedId), gridX + 22, gridY - 13, 0xAAAAAA);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Ilość"), gridX, gridY + 90, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Cena"), gridX + 80, gridY + 90, 0xFFFFFF);

        ItemStack hovered = ItemStack.EMPTY;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int x = gridX + col * 18;
                int y = gridY + row * 18 + (row == 3 ? 4 : 0);
                ItemStack s = client.player.getInventory().getStack(invIndex(row, col));
                boolean sel = !s.isEmpty() && Registries.ITEM.getId(s.getItem()).toString().equals(selectedId);
                ctx.fill(x, y, x + 17, y + 17, sel ? 0xFF55FF55 : 0xFF555555);
                ctx.fill(x + 1, y + 1, x + 16, y + 16, 0xFF8B8B8B);
                if (!s.isEmpty()) {
                    ctx.drawItem(s, x, y);
                    ctx.drawStackOverlay(textRenderer, s, x, y);
                }
                if (mouseX >= x && mouseX < x + 17 && mouseY >= y && mouseY < y + 17) hovered = s;
            }
        }
        if (!hovered.isEmpty()) ctx.drawItemTooltip(textRenderer, hovered, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int x = gridX + col * 18;
                int y = gridY + row * 18 + (row == 3 ? 4 : 0);
                if (mouseX >= x && mouseX < x + 17 && mouseY >= y && mouseY < y + 17) {
                    ItemStack s = client.player.getInventory().getStack(invIndex(row, col));
                    if (!s.isEmpty()) {
                        selectedId = Registries.ITEM.getId(s.getItem()).toString();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
