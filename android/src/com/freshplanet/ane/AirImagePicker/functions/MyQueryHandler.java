package com.freshplanet.ane.AirImagePicker.functions;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;

import java.lang.ref.WeakReference;

/**
 * Created by peternicolai on 7/29/16.
 */
public final class MyQueryHandler extends AsyncQueryHandler {

    public interface OnQueryCompleteListener {
        public void onQueryComplete(Cursor data, int position);
    }

    private WeakReference<OnQueryCompleteListener> _listener;

    public MyQueryHandler(ContentResolver cr, OnQueryCompleteListener listener) {
        super(cr);
        this._listener = new WeakReference<OnQueryCompleteListener>(listener);
    }

    @Override
    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
        super.onQueryComplete(token, cookie, cursor);
        if (_listener != null && _listener.get() != null) {
            _listener.get().onQueryComplete(cursor, token);
        } else {
            if(cursor != null){
                cursor.close();
            }
        }
    }
}
