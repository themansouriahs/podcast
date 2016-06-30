package org.bottiger.podcast;

import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import org.bottiger.podcast.TestUtils.RecyclerTestUtils;
import org.bottiger.podcast.TestUtils.TestUtils;
import org.bottiger.podcast.utils.OPMLImportExport;
import org.bottiger.podcast.views.dialogs.DialogOPML;
import org.junit.Before;

import java.io.File;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

/**
 * Created by aplb on 17-09-2015.
 */
@LargeTest
public class EspressoOPMLTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private static final String TAG = "EspressoOPMLTest";
    private static final int FIRST_ITEM = 0;
    private static final String exportDir = "/sdcard/";

    private static final int NUMBER_OF_SUBSCRIPTIONS = 3;

    private OPMLImportExport mOPMLImportExport;
    private File mOutputFile;

    public EspressoOPMLTest() {
        super(MainActivity.class);
    }

    public EspressoOPMLTest(Class<MainActivity> activityClass) {
        super(activityClass);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());

        // I guess we assume we have permision here
        mOPMLImportExport = new OPMLImportExport(getActivity());
        mOutputFile = mOPMLImportExport.getExportFile();
        mOutputFile.delete();


        getActivity();

        TestUtils.clearAllData(getActivity());
        TestUtils.subscribe(NUMBER_OF_SUBSCRIPTIONS);

        openDialog();

    }

    public void testSDcardFolderExists() {
        File dir = new File(exportDir);
        assertTrue(dir.isDirectory());
    }

    public void testOpenImportExportDialog() {
        onView(withText(R.string.opml_radio_import)).check(matches(isDisplayed()));
        onView(withId(R.id.radio_export)).inRoot(isDialog()).check(matches(isDisplayed()));
    }

    public void testImportOPMLButton() {

        // close the dialog
        onView(withText(DialogOPML.getNegativeString())).perform(click());

        onView(withText(R.string.opml_radio_import)).check(matches(not(isDisplayed())));

        TestUtils.unsubscribeAll(getActivity());

        onView(withId(R.id.import_opml_button)).check(matches(isDisplayed()));
        onView(withId(R.id.import_opml_button)).perform(click());

        onView(withText(R.string.opml_radio_import)).check(matches(isDisplayed()));
    }

    public void testExportFeeds() {

        File expectedFile = expectedExportedFile();
        File actualFile   = mOPMLImportExport.getExportFile();

        exportOPMLFile(expectedFile, actualFile);

        actualFile.delete();

        assertFalse(expectedFile.exists());
        assertFalse(actualFile.exists());
    }

    public void testImportFeeds() {
        File expectedOutputFile = expectedExportedFile();
        File expectedImportFile = expectedImportedFile();


        File actualOutputFile   = mOPMLImportExport.getExportFile();
        File actualImportFile = mOPMLImportExport.getImportFile();

        exportOPMLFile(expectedOutputFile, actualOutputFile);

        actualOutputFile.renameTo(actualImportFile);

        Log.d(TAG, "Actual output file: " + actualOutputFile.getAbsolutePath());
        Log.d(TAG, "Actual import file: " + actualImportFile.getAbsolutePath());
        Log.d(TAG, "Expected import file: " + expectedImportFile.getAbsolutePath());

        assertTrue(expectedImportFile.exists());

        TestUtils.unsubscribeAll(getActivity());

        // Reopen the dialog
        openDialog();

        // Start the import
        onView(withId(R.id.radio_import))
                .perform(click());
        onView(withText(R.string.opml_radio_import)).check(matches(isDisplayed()));
        onView(withText(DialogOPML.getPositiveString())).perform(click());

        for (int i = 0; i < NUMBER_OF_SUBSCRIPTIONS; i++) {
            RecyclerTestUtils.withRecyclerView(R.id.feed_recycler_view)
                    .atPosition(i)
                    .matches(isDisplayed());
        }
    }

    private File expectedExportedFile() {
        return new File(exportDir + OPMLImportExport.getExportFilename());
    }

    private File expectedImportedFile() {
        return new File(exportDir + OPMLImportExport.getImportFilename());
    }

    private void exportOPMLFile(File expectedFile, File actualFile) {

        assertFalse(expectedFile.exists());
        assertFalse(actualFile.exists());

        onView(withId(R.id.radio_export))
                .perform(click());

        onView(withText(R.string.opml_radio_import)).check(matches(isDisplayed()));

        onView(withText(DialogOPML.getPositiveString())).perform(click());
        //onView(withText(R.string.opml_radio_import)).check(matches(not(isDisplayed())));

        Log.d(TAG, "Excepted file: " + expectedFile.getAbsolutePath());
        Log.d(TAG, "Actual file: " + actualFile.getAbsolutePath());

        assertTrue(actualFile.exists());
        assertTrue(expectedFile.exists());
    }

    private void openDialog() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.menu_import)).perform(click());
    }
}
