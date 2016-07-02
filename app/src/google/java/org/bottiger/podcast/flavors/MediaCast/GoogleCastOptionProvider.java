package org.bottiger.podcast.flavors.MediaCast;

import android.content.Context;

import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;

import org.bottiger.podcast.R;

import java.util.List;

/**
 * Created by aplb on 29-06-2016.
 */

public class GoogleCastOptionProvider implements OptionsProvider {

    @Override
    public CastOptions getCastOptions(Context argContext) {
        CastOptions castOptions = new CastOptions.Builder()
                .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID) // FIXME should be a proper ID
                .build();
        return castOptions;
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }


}
