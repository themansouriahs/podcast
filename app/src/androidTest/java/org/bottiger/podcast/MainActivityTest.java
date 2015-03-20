package org.bottiger.podcast;

import android.content.Intent;
import android.test.ActivityUnitTestCase;

import org.bottiger.podcast.MainActivity;

/**
 * Created by apl on 20-03-2015.
 */
public class MainActivityTest extends ActivityUnitTestCase<MainActivity> {

    private Intent mLaunchIntent = null;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    public MainActivityTest(Class<MainActivity> activityClass) {
        super(activityClass);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertNull("Intent was null", mLaunchIntent);
        mLaunchIntent.toString();

        mLaunchIntent = new Intent(getInstrumentation()
                .getTargetContext(), MainActivityTest.class);
        startActivity(mLaunchIntent, null, null);
        /*
        final Button launchNextButton =
                (Button) getActivity()
                        .findViewById(R.id.launch_next_activity_button);
                        */
    }
}