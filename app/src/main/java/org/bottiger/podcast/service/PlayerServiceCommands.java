package org.bottiger.podcast.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Created by apl on 27-05-2015.
 */
public class PlayerServiceCommands {
/*
    private static final String TAG = PlayerServiceCommands.class.toString();

    @NonNull
    private Context mContext;
    private PlayerService mPlayerService = null;
    private IBinder mPlayerServiceBinder = null;

    public PlayerServiceCommands(@NonNull BroadcastReceiver argBroadcastReceiver, @NonNull Context argContext) {
        mContext = argContext;

        mPlayerServiceBinder = argBroadcastReceiver.peekService(argContext, new Intent(argContext, PlayerService.class));
    }

    public void playNext() {
        executePlayerServiceCommand(new ServiceCommand() {
            @Override
            public void executeCommand(PlayerService argPlayerService) {
                argPlayerService.playNext();
            }
        });
    }

    public void halt() {
        executePlayerServiceCommand(new ServiceCommand() {
            @Override
            public void executeCommand(PlayerService argPlayerService) {
                argPlayerService.halt();
            }
        });
    }

    public void executePlayerServiceCommand(@NonNull final ServiceCommand argServiceCommand) {
        PlayerService ps = mPlayerService.get();
        if (ps != null) {
            argServiceCommand.executeCommand(ps);
            return;
        }



        new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                Log.d(TAG, "onServiceConnected");
                PlayerService playerService = ((PlayerService.PlayerBinder) service).getService();
                mPlayerService = new WeakReference<>(playerService);
                argServiceCommand.executeCommand(playerService);
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                Log.d(TAG, "onServiceDisconnected");
                mPlayerService = new WeakReference<>(null);
            }
        };
    }


    public interface ServiceCommand {
        void executeCommand(@NonNull PlayerService argPlayerService);
    }
    */
}
