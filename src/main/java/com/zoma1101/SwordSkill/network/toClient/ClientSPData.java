package com.zoma1101.swordskill.network.toClient;

public class ClientSPData {
    private static double playerSP;

    public static void set(double sp) {
        playerSP = sp;
    }

    public static double get() {
        return playerSP;
    }
}
