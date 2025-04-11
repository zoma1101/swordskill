package com.zoma1101.swordskill.network;

import com.zoma1101.swordskill.server.handler.SkillExecutionManager;
import com.zoma1101.swordskill.swordskills.ISkill;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

import static com.zoma1101.swordskill.data.SkillDataFetcher.isSkillUnlocked;

public class UseSkillPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final int skillId;
    private final int finalTick;

    public UseSkillPacket(int skillId, int finalTick) {
        this.skillId = skillId;
        this.finalTick = finalTick;
    }

    public UseSkillPacket(FriendlyByteBuf buf) {
        this.skillId = buf.readInt();
        this.finalTick = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(skillId);
        buf.writeInt(finalTick);
    }

    public static void handle(UseSkillPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                LOGGER.warn("プレイヤーがnullです。スキル発動を中止します。");
                return;
            }

            SkillData skill = SwordSkillRegistry.SKILLS.get(msg.skillId);
            if (skill != null && isSkillUnlocked(player, msg.skillId) || skill != null && player.gameMode.isCreative()) {
                if (msg.finalTick == 0) {
                    // FinalTickが0の場合、一度だけ実行
                    executeSkill(player, skill);
                } else {
                    // スキル実行情報を保存
                    SkillExecutionManager.startSkillExecution(player, msg.skillId, msg.finalTick);
                }
            } else {
                LOGGER.warn("スキルID {} に対応するスキルが見つかりません。", msg.skillId);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void executeSkill(ServerPlayer player, SkillData skill) {
        try {
            ISkill skillInstance = skill.getSkillClass().getDeclaredConstructor().newInstance();
            skillInstance.execute(player.level(), player, 0,skill.getId());
            LOGGER.info("スキル {} (ID: {}) を Tick {} で実行", skill.getName(), skill.getId(), 0);
        } catch (Exception e) {
            LOGGER.error("スキル実行中にエラーが発生しました。", e);
        }
    }
}