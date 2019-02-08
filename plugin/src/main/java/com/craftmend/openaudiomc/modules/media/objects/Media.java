package com.craftmend.openaudiomc.modules.media.objects;

import com.craftmend.openaudiomc.OpenAudioMc;
import com.craftmend.openaudiomc.modules.media.enums.MediaFlag;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
public class Media {

    //media tracker
    @Setter @Getter private String mediaId = UUID.randomUUID().toString();

    //media information
    private String source;
    private int startInstant;
    @Setter @Getter private transient int keepTimeout = -1;
    @Getter @Setter private Boolean doPickup = true;
    @Getter @Setter private Boolean loop = false;
    @Getter @Setter private int fadeTime = 0;
    @Getter @Setter private MediaFlag flag = MediaFlag.DEFAULT;

    /**
     * @param source Create a new Media source based on your url
     */
    public Media(String source) {
        this.source = OpenAudioMc.getInstance().getMediaModule().process(source);
        this.startInstant = (int) (OpenAudioMc.getInstance().getTimeService().getSyncedInstant().toEpochMilli() / 1000);
    }

    /**
     * @param options The options. Selected via the command
     * @return instance of self
     */
    public Media applySettings(MediaOptions options) {
        this.loop = options.getLoop();
        this.keepTimeout = options.getExpirationTimeout();
        if (options.getId() != null) this.mediaId = options.getId();
        this.doPickup = options.getPickUp();
        this.setFadeTime(options.getFadeTime());
        return this;
    }

}