package osy.kcg.utils;

import android.content.Context;
import android.util.AttributeSet;

public class MyAutoCompleteTextView extends androidx.appcompat.widget.AppCompatAutoCompleteTextView {

    public MyAutoCompleteTextView(Context context) {
        super(context);
    }

    public MyAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean enoughToFilter() {
        //return super.enoughToFilter();
        return true;
    }

    @Override
    public void onFilterComplete(int count) {
        //super.onFilterComplete(count);
    }

    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        //super.performFiltering(text, keyCode);
    }
}
