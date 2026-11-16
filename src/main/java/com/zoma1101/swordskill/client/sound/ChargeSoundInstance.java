package com.zoma1101.swordskill.client.sound;

import com.zoma1101.swordskill.client.handler.ClientForgeHandler;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * スキルチャージ中だけ再生し続けるループサウンド。
 *
 * <p>毎 tick {@link ClientForgeHandler#isCharging} を確認し、チャージが終了したら
 * 自分から停止する。音の位置はリスナー固定なので、チャージ中は常に一定の音量で聞こえる。</p>
 */
@OnlyIn(Dist.CLIENT)
public class ChargeSoundInstance extends AbstractTickableSoundInstance {

    public ChargeSoundInstance(SoundEvent soundEvent, float volume, float pitch) {
        super(soundEvent, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.relative = true;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void tick() {
        if (!ClientForgeHandler.isCharging) {
            stop();
        }
    }

    /** 即時に鳴らし終わらせる（キャンセル・発動時）。 */
    public void stopNow() {
        stop();
    }
}
