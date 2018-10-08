package com.example.xyzreader.ui;

import android.content.Context;
import android.graphics.Typeface;

import java.util.Hashtable;

/**
 * Created by E560XT on 10/4/2018.
 */

public class TypeFaceProvider {
  private static Hashtable<String, Typeface> sTypeFaces = new Hashtable<String, Typeface>(
      4);

  public static Typeface getTypeFace(Context context, String fileName) {
    Typeface tempTypeface = sTypeFaces.get(fileName);

    if (tempTypeface == null) {
      tempTypeface = Typeface.createFromAsset(context.getResources().getAssets(), fileName);
      sTypeFaces.put(fileName, tempTypeface);
    }

    return tempTypeface;
  }
}
