package ch.tarsier.tarsier.test;

import android.test.ActivityInstrumentationTestCase2;
import ch.tarsier.tarsier.ui.activity.ChatActivity;

/**
 * @author marinnicolini
 */
public class ChatActivityTest extends ActivityInstrumentationTestCase2<ChatActivity> {

    public ChatActivityTest() {
        super(ChatActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Espresso will not launch our activity for us, we must launch it via getActivity().
        getActivity();
    }

    /* test the sendImageButton if is clickable when something is to be sent or not
    public void testSendMessageButtonClickable() {
        String messageSent = "This is a new message to be sent.";
        onView(withId(R.id.sendImageButton)).check(matches(not(isClickable())));
        onView(withId(R.id.message_to_send)).perform(typeText(messageSent));
        onView(withId(R.id.sendImageButton)).check(matches(isClickable()));
        onView(withId(R.id.message_to_send)).perform(clearText(), closeSoftKeyboard());
        onView(withId(R.id.sendImageButton)).check(matches(not(isClickable())));
    }*/

    /*
    test if the scrolling fetch the last messages

    public void testScrollToFetchMessages() {

    }
    */

    /*
    test if the view go to the last message after scrolling and then send a message

    public void testScrollToCurrentSendMessageButton() {

    }
     */

    /*
    test if the dateSeparator is actually visible when we scroll
    public void testDateSeparatorOnScroll() {

    }
     */
}