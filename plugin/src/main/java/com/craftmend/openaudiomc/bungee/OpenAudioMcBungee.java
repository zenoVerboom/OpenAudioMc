package com.craftmend.openaudiomc.bungee;

import com.craftmend.openaudiomc.OpenAudioMcCore;
import com.craftmend.openaudiomc.bungee.modules.commands.BungeeCommandModule;
import com.craftmend.openaudiomc.bungee.modules.player.PlayerManager;
import com.craftmend.openaudiomc.generic.platform.Platform;
import com.craftmend.openaudiomc.generic.state.states.IdleState;

import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;

import java.time.Duration;
import java.time.Instant;

@Getter
public class OpenAudioMcBungee extends Plugin {

    /**
     * Constant: main plugin instance
     */
    @Getter private static OpenAudioMcBungee instance;

    /**
     * Managers
     */
    @Getter private PlayerManager playerManager;
    @Getter private BungeeCommandModule commandModule;

    @Override
    public void onEnable() {
        // Timing
        Instant boot = Instant.now();

        instance = this;

        // setup core
        new OpenAudioMcCore(Platform.BUNGEE);

        // load managers and shit
        this.playerManager = new PlayerManager(this);
        this.commandModule = new BungeeCommandModule(this);

        // set state to idle, to allow connections and such
        OpenAudioMcCore.getInstance().getStateService().setState(new IdleState("OpenAudioMc started and awaiting command"));

        // timing end and calc
        Instant finish = Instant.now();
        System.out.println(OpenAudioMcCore.getLOG_PREFIX() + "Starting and loading took " + Duration.between(boot, finish).toMillis() + "MS");
    }

    /**
     * save configuration and stop the plugin
     */
    @Override
    public void onDisable() {
        OpenAudioMcCore.getInstance().getConfigurationInterface().saveAll();
        if (OpenAudioMcCore.getInstance().getStateService().getCurrentState().isConnected()) {
            OpenAudioMcCore.getInstance().getNetworkingService().stop();
        }
    }

}