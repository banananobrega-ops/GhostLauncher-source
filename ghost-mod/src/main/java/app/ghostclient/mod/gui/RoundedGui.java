package app.ghostclient.mod.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Cantos arredondados desenhados so com context.fill(...) -- de proposito, para nao
 * depender da API de texturas/shader (que muda de versao para versao do MC). E um
 * bocadinho mais trabalho por frame, mas e barato (poucos retangulos, raio pequeno)
 * e funciona em qualquer versao que tenha DrawContext#fill.
 */
public final class RoundedGui {
    private RoundedGui() {}

    /** Retangulo cheio com os 4 cantos arredondados. */
    public static void rect(DrawContext ctx, int x, int y, int w, int h, int radius, int argb) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            ctx.fill(x, y, x + w, y + h, argb);
            return;
        }
        // corpo central (entre as linhas de topo/fundo arredondadas)
        ctx.fill(x, y + r, x + w, y + h - r, argb);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0, (double) r * r - dy * dy)));
            if (inset >= w / 2) continue;
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, argb);
            ctx.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, argb);
        }
    }

    /** Retangulo com fundo + moldura de destaque (desenha a moldura maior por baixo e o fundo por cima), ex. cartoes/botoes selecionados. */
    public static void panelWithBorder(DrawContext ctx, int x, int y, int w, int h, int radius, int fillArgb, int borderArgb) {
        rect(ctx, x - 1, y - 1, w + 2, h + 2, radius + 1, borderArgb);
        rect(ctx, x, y, w, h, radius, fillArgb);
    }
}
