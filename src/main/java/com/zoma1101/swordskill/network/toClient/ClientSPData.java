package com.zoma1101.swordskill.network.toClient;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientSPData {
    private static double currentSP;
    private static double maxSP;

    public static void set(double sp, double max) {
        currentSP = sp;
        maxSP = max;
    }

    public static double get() {
        return currentSP;
    }

    public static double getMax() {
        return maxSP;
    }
}
