package com.craftmend.openaudiomc.generic.networking.drivers.handler;

import com.craftmend.openaudiomc.OpenAudioMc;
import com.craftmend.openaudiomc.generic.environment.MagicValue;
import com.craftmend.openaudiomc.generic.client.objects.ClientConnection;
import com.craftmend.openaudiomc.generic.networking.drivers.interfaces.NotificationHandler;
import com.craftmend.openaudiomc.generic.networking.drivers.models.BackendNotification;
import com.craftmend.openaudiomc.generic.networking.interfaces.NetworkingService;
import com.craftmend.openaudiomc.generic.platform.Platform;
import com.craftmend.openaudiomc.generic.platform.interfaces.TaskService;

import java.util.UUID;

/**
 * Simple handler for admin notifications
 *
 * these events send simple chat messages to online staff/admins
 */
public class AdminNotification implements NotificationHandler {

    @Override
    public void handle(BackendNotification notificationData) {
        String message = MagicValue.COMMAND_PREFIX.get(String.class);
        message += Platform.makeColor("RED") + "ADMIN NOTIFICATION: " + Platform.makeColor("YELLOW") + Platform.translateColors(notificationData.getMessage());

        for (ClientConnection client : OpenAudioMc.getService(NetworkingService.class).getClients()) {
            if (client.getUser().isAdministrator()) {
                client.getUser().sendMessage(message);
            }
        }

        TaskService ts = OpenAudioMc.resolveDependency(TaskService.class);
        NetworkingService ns = OpenAudioMc.getService(NetworkingService.class);

        // schedule a temporary listener to handle notifications post join
        String finalMessage = message;
        UUID subscriber = ns.subscribeToConnections((cc) -> {
            if (cc.getUser().isAdministrator()) {
                ts.schduleSyncDelayedTask(() -> {
                    // send the message
                    cc.getUser().sendMessage(finalMessage);
                }, 25);
            }
        });

        // remove the listener in like a minute
        ts.schduleSyncDelayedTask(() -> {
            ns.unsubscribeClientEventHandler(subscriber);
        }, 60 * 20);
    }
}
