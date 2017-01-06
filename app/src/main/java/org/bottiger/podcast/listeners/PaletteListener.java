package org.bottiger.podcast.listeners;

import android.support.v7.graphics.Palette;

/**
 * Created by apl on 05-08-2014.
 */
public interface PaletteListener {

    public enum TINT {VIBRANT, VIBRANT_LIGHT, VIBRANT_DARK, MUTED, MUTED_LIGHT, MUTED_DARK};

    //public void onPaletteFound(TINT argTint, int argColor);
    public void onPaletteFound(Palette argChangedPalette);
    //public void notifyPaletteChanged(Palette argChangedPalette);
    public String getPaletteUrl();
}
