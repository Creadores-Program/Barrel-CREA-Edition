package org.barrelmc.barrel.utils.nukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * All supported formatting values for chat and console.
 */
public enum TextFormat {
    // Colores de Minecraft
    BLACK("\u001B[30m"),
    DARK_BLUE("\u001B[34m"),
    DARK_GREEN("\u001B[32m"),
    DARK_AQUA("\u001B[36m"),
    DARK_RED("\u001B[31m"),
    DARK_PURPLE("\u001B[35m"),
    GOLD("\u001B[33m"),
    GRAY("\u001B[37m"),
    DARK_GRAY("\u001B[90m"),
    BLUE("\u001B[94m"),
    GREEN("\u001B[92m"),
    AQUA("\u001B[96m"),
    RED("\u001B[91m"),
    LIGHT_PURPLE("\u001B[95m"),
    YELLOW("\u001B[93m"),
    WHITE("\u001B[97m"),
    
    // Formatos de texto de Minecraft
    OBFUSCATED(""), // No tiene equivalente directo en la consola
    BOLD("\u001B[1m"),
    STRIKETHROUGH(""), // No tiene equivalente directo en la consola
    UNDERLINE("\u001B[4m"),
    ITALIC("\u001B[3m"),
    RESET("\u001B[0m"),
    
    // Colores de materiales de Minecraft (estos son ejemplos y pueden no ser precisos)
    MATERIAL_QUARTZ("\u001B[97m"),
    MATERIAL_IRON("\u001B[37m"),
    MATERIAL_NETHERITE("\u001B[90m"),
    MATERIAL_REDSTONE("\u001B[91m"),
    MATERIAL_COPPER("\u001B[33m"),
    MATERIAL_GOLD("\u001B[93m"),
    MATERIAL_EMERALD("\u001B[92m"),
    MATERIAL_DIAMOND("\u001B[96m"),
    MATERIAL_LAPIS("\u001B[94m"),
    MATERIAL_AMETHYST("\u001B[95m");

    private final String ansiCode;

    TextFormat(String ansiCode) {
        this.ansiCode = ansiCode;
    }

    public String getAnsiCode() {
        return ansiCode;
    }
}
