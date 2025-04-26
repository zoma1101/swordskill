package com.zoma1101.swordskill.network;

import com.zoma1101.swordskill.client.handler.ClientSkillSlotHandler;
import com.zoma1101.swordskill.swordskills.SkillData; // WeaponType を使うために import
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Collections; // 追加
import java.util.HashSet;   // 追加
import java.util.Set;       // 追加
import java.util.function.Supplier;

public class SkillSlotInfoPacket {
    private final int[] skillIds;
    private final String weaponName;
    private final Set<SkillData.WeaponType> weaponTypes; // <-- WeaponTypeのセットを追加

    // コンストラクタを修正
    public SkillSlotInfoPacket(int[] skillIds, String weaponName, Set<SkillData.WeaponType> weaponTypes) {
        this.skillIds = skillIds;
        this.weaponName = (weaponName != null) ? weaponName : "None";
        // weaponTypes が null の場合は空のセットを使用
        this.weaponTypes = (weaponTypes != null) ? weaponTypes : Collections.emptySet();
    }

    // ByteBufからの読み込みコンストラクタを修正
    public SkillSlotInfoPacket(FriendlyByteBuf buf) {
        this.skillIds = buf.readVarIntArray();
        this.weaponName = buf.readUtf();
        // WeaponTypeのセットを読み込む
        int typeCount = buf.readVarInt();
        this.weaponTypes = new HashSet<>();
        for (int i = 0; i < typeCount; i++) {
            this.weaponTypes.add(buf.readEnum(SkillData.WeaponType.class)); // Enumとして読み込む
        }
    }

    // ByteBufへの書き込みメソッドを修正
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarIntArray(skillIds);
        buf.writeUtf(this.weaponName);
        // WeaponTypeのセットを書き込む
        buf.writeVarInt(this.weaponTypes.size()); // セットの要素数を書き込む
        for (SkillData.WeaponType type : this.weaponTypes) {
            buf.writeEnum(type); // Enumとして書き込む
        }
    }

    // パケット処理メソッドを修正
    public static void handle(SkillSlotInfoPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    // ClientSkillSlotHandlerに全ての情報を渡す
                    ClientSkillSlotHandler.updateAllInfo(msg.skillIds, msg.weaponName, msg.weaponTypes)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}