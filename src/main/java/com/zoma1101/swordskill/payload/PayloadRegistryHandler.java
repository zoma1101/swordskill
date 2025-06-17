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
        registrar.playToServer(SkillUnlockPayload.TYPE, SkillUnlockPayload.STREAM_CODEC, SkillUnlockPayload::handle);
        registrar.playToServer(SkillRequestPayload.TYPE, SkillRequestPayload.STREAM_CODEC, SkillRequestPayload::handle);
        registrar.playToServer(SkillLoadSlotPayload.TYPE, SkillLoadSlotPayload.STREAM_CODEC, SkillLoadSlotPayload::handle);
        registrar.playToServer(SkillSelectionPayload.TYPE, SkillSelectionPayload.STREAM_CODEC, SkillSelectionPayload::handle);
        registrar.playToServer(SkillSlotSelectionPayload.TYPE, SkillSlotSelectionPayload.STREAM_CODEC, SkillSlotSelectionPayload::handle);
        registrar.playToServer(CheckSkillUnlockedPayload.TYPE, CheckSkillUnlockedPayload.STREAM_CODEC, CheckSkillUnlockedPayload::handle);
        registrar.playToServer(ConsumeUnlockItemPayload.TYPE, ConsumeUnlockItemPayload.STREAM_CODEC, ConsumeUnlockItemPayload::handle);



        // --- Server -> Client ペイロード登録 ---
        // ClientSkillMessageHandler クラスのメソッドを参照
        registrar.playToClient(SyncSkillIndexPayload.TYPE, SyncSkillIndexPayload.STREAM_CODEC, SyncSkillIndexPayload::handleClient);
        registrar.playToClient(SyncUnlockedSkillsPayload.TYPE, SyncUnlockedSkillsPayload.STREAM_CODEC, SyncUnlockedSkillsPayload::handleClient);
        registrar.playToClient(PlayAnimationPayload.TYPE, PlayAnimationPayload.STREAM_CODEC, PlayAnimationPayload::handleClient);
        registrar.playToClient(SkillSlotInfoPayload.TYPE, SkillSlotInfoPayload.STREAM_CODEC, SkillSlotInfoPayload::handleClient);


        LOGGER.info("Network payloads registration complete.");
    }
}