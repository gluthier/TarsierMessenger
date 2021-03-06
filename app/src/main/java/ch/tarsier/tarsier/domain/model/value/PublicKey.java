package ch.tarsier.tarsier.domain.model.value;

import android.util.Base64;

import java.io.Serializable;
import java.util.Arrays;

import ch.tarsier.tarsier.data.ByteArraySerializable;

/**
 * Represents a public key, and provides a few convenience methods to:
 * - serialize it to a byte array
 * - check for equality with another public key
 * - encode it as base64
 *
 * @author romac
 */
public class PublicKey implements Serializable, ByteArraySerializable {

    private byte[] mBytes;

    public PublicKey(byte[] bytes) {
        mBytes = bytes;
    }

    public PublicKey(String base64Encoded) {
        mBytes = Base64.decode(base64Encoded, Base64.NO_WRAP);
    }

    public byte[] getBytes() {
        return mBytes;
    }
    public byte[] toByteArray() {
        return mBytes;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }

        // noinspection SimplifiableIfStatement
        if (that == null || ((Object) this).getClass() != that.getClass()) {
            return false;
        }

        return Arrays.equals(mBytes, ((PublicKey) that).getBytes());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mBytes);
    }

    public String base64Encoded() {
        return Base64.encodeToString(mBytes, Base64.NO_WRAP);
    }

    public String toString() {
        return base64Encoded();
    }

}
