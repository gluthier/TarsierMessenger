package ch.tarsier.tarsier.test;

import android.test.AndroidTestCase;

import ch.tarsier.tarsier.Tarsier;
import ch.tarsier.tarsier.domain.model.Message;
import ch.tarsier.tarsier.domain.repository.MessageRepository;
import ch.tarsier.tarsier.exception.InsertException;
import ch.tarsier.tarsier.exception.InvalidModelException;
import ch.tarsier.tarsier.exception.NoSuchModelException;
import ch.tarsier.tarsier.exception.UpdateException;

/**
 * @author gluthier
 */
public class MessageRepositoryTest extends AndroidTestCase {

    MessageRepository mMessageRepository;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mMessageRepository = Tarsier.app().getMessageRepository();
    }

    // test public Message findById(long id)
    public void testFindIllegalIds() {
        long[] illegalIds = {0, -1, -9001, Long.MIN_VALUE};

        for (long id : illegalIds) {
            try {
                mMessageRepository.findById(id);
                fail("Expecting IllegalArgumentException but none was thrown.");
            } catch (IllegalArgumentException e) {
                assertEquals("Message ID cannot be < 1", e.getMessage());
            }catch (NoSuchModelException e) {
                fail("Expecting IllegalArgumentException to be thrown first.");
            }
        }
    }

    public void testFindGoodIds() {
        long[] goodIds = {1, 42, 9001, Long.MAX_VALUE};

        for (long id : goodIds) {
            try {
                mMessageRepository.findById(id);
            } catch (IllegalArgumentException e) {
                fail("IllegalArgumentException should not be thrown.");
            } catch (NoSuchModelException e) {
                // good since the db is empty
            }
        }
    }

    // test public void insert(Message message)
    public void testInsertNullMessage() {
        try {
            mMessageRepository.insert(null);
            fail("Expecting InvalidModelException to be thrown.");
        } catch (InvalidModelException e) {
            assertEquals("Message should not be null.", e.getMessage());
        } catch (InsertException e) {
            fail("Expecting InvalidModelException to be thrown first.");
        }
    }

    public void testInsertDummyMessage() {
        try {
            mMessageRepository.insert(new Message(1, "hello", 1, 0));
        } catch (InvalidModelException e) {
            fail("InvalidModelException should not be thrown.");
        } catch (InsertException e) {
            fail("InsertException should not be thrown.");
        }
    }

    // test public void update(Message message)
    public void testUpdateNullMessage() {
        try {
            mMessageRepository.update(null);
        } catch (InvalidModelException e) {
            assertEquals("Message should not be null.", e.getMessage());
        } catch (UpdateException e) {
            fail("Expecting InvalidModelException to be thrown first.");
        }
    }

    public void testUpdateDummyMessage() {
        try {
            mMessageRepository.update(new Message(1, "hello", 1, 0));
        } catch (InvalidModelException e) {
            fail("InvalidModelException should not be thrown.");
        } catch (UpdateException e) {
            fail("UpdateException should not be thrown.");
        }
    }
}
