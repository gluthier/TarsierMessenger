package ch.tarsier.tarsier.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import ch.tarsier.tarsier.R;

/**
 * Created by benjamin on 26/11/14.
 */
public class BitmapFromPath {

    public static Bitmap getBitmapFromPath(Context context, String filePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);

        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.add_picture);
        }
        return bitmap;
    }

}
