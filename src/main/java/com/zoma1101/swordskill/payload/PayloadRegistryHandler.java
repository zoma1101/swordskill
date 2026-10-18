package com.zoma1101.swordskill.payload;

import com.zoma1101.swordskill.SwordSkill;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//PacketDistributor.sendToServer(new SkillRequestPacket());

// MODイベントバス用のイベントサブスクライバーとしてマーク
@EventBusSubscriber(modid = SwordSkill.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class PayloadRegistryHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    // RegisterPayloadHandlersEvent のリスナーメソッド
    @SubscribeEvent
    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        // MODメインクラスからPROTOCOL_VERSIONを取得 (またはここで定義)
        final PayloadRegistrar registrar = event.registrar(SwordSkill.MOD_ID)
                .versioned(SwordSkill.PROTOCOL_VERSION); // MODメインクラスの定数を参照

        LOGGER.info("Registering network payloads for {}", SwordSkill.MOD_ID);

        registrar.playToServer(UseSkillPayload.TYPE, UseSkillPayload.STREAM_CODEC, UseSkillPayload::handle);
        registrar.playToServer(SkillRequestPayload.TYPE, SkillRequestPayload.STREAM_CODEC, SkillRequestPayload::handle);
        registrar.playToServer(SkillLoadSlotPayload.TYPE, SkillLoadSlotPayload.STREAM_CODEC,
                SkillLoadSlotPayload::handle);
        registrar.playToServer(SkillSelectionPayload.TYPE, SkillSelectionPayload.STREAM_CODEC,
                SkillSelectionPayload::handle);
        registrar.playToServer(SkillSlotSelectionPayload.TYPE, SkillSlotSelectionPayload.STREAM_CODEC,
                SkillSlotSelectionPayload::handle);
        registrar.playToServer(CheckSkillUnlockedPayload.TYPE, CheckSkillUnlockedPayload.STREAM_CODEC,
                CheckSkillUnlockedPayload::handle);
        registrar.playToServer(RequestSkillLevelsPayload.TYPE, RequestSkillLevelsPayload.STREAM_CODEC,
                RequestSkillLevelsPayload::handle);

        registrar.playToServer(RequestUnlockSkillPayload.TYPE, RequestUnlockSkillPayload.STREAM_CODEC,
                RequestUnlockSkillPayload::handle);
        registrar.playToServer(RequestSkillLevelUpPayload.TYPE, RequestSkillLevelUpPayload.STREAM_CODEC,
                RequestSkillLevelUpPayload::handle);
        registrar.playToServer(AllocateStatPayload.TYPE, AllocateStatPayload.STREAM_CODEC,
                AllocateStatPayload::handle);
        registrar.playToServer(RequestPlayerLevelPayload.TYPE, RequestPlayerLevelPayload.STREAM_CODEC,
                RequestPlayerLevelPayload::handle);
        registrar.playToServer(SetSmithingModePayload.TYPE, SetSmithingModePayload.STREAM_CODEC,
                SetSmithingModePayload::handle);
        registrar.playToServer(SelectSmithingRecipePayload.TYPE, SelectSmithingRecipePayload.STREAM_CODEC,
                SelectSmithingRecipePayload::handle);

        // --- Server -> Client ペイロード登録 ---
        registrar.playToClient(SyncSkillIndexPayload.TYPE, SyncSkillIndexPayload.STREAM_CODEC,
                SyncSkillIndexPayload::handleClient);
        registrar.playToClient(SyncUnlockedSkillsPayload.TYPE, SyncUnlockedSkillsPayload.STREAM_CODEC,
                SyncUnlockedSkillsPayload::handleClient);
        registrar.playToClient(PlayAnimationPayload.TYPE, PlayAnimationPayload.STREAM_CODEC,
                PlayAnimationPayload::handleClient);
        registrar.playToClient(SkillSlotInfoPayload.TYPE, SkillSlotInfoPayload.STREAM_CODEC,
                SkillSlotInfoPayload::handleClient);
        registrar.playToClient(SyncDataSkillsPayload.TYPE, SyncDataSkillsPayload.STREAM_CODEC,
                SyncDataSkillsPayload::handleClient);
        registrar.playToClient(SyncSkillLevelsPayload.TYPE, SyncSkillLevelsPayload.STREAM_CODEC,
                SyncSkillLevelsPayload::handleClient);
        registrar.playToClient(SyncProficiencyPayload.TYPE, SyncProficiencyPayload.STREAM_CODEC,
                SyncProficiencyPayload::handleClient);
        registrar.playToClient(SyncPlayerLevelPayload.TYPE, SyncPlayerLevelPayload.STREAM_CODEC,
                SyncPlayerLevelPayload::handleClient);
        registrar.playToClient(SyncSPPayload.TYPE, SyncSPPayload.STREAM_CODEC, SyncSPPayload::handle);
        registrar.playToClient(HitstopPayload.TYPE, HitstopPayload.STREAM_CODEC, HitstopPayload::handleClient);
        registrar.playToClient(SkillShockwavePayload.TYPE, SkillShockwavePayload.STREAM_CODEC,
                SkillShockwavePayload::handleClient);
        registrar.playToClient(SkillHitEffectPayload.TYPE, SkillHitEffectPayload.STREAM_CODEC,
                SkillHitEffectPayload::handleClient);
        registrar.playToClient(SyncSmithingDefsPayload.TYPE, SyncSmithingDefsPayload.STREAM_CODEC,
                SyncSmithingDefsPayload::handleClient);

        LOGGER.info("Network payloads registration complete.");
    }
}