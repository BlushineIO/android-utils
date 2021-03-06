package io.blushine.android;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentTransaction;
import android.view.View;
import android.widget.EditText;

import java.util.HashMap;
import java.util.Map;

import de.mrapp.android.dialog.MaterialDialog;

/**
 * Base class for fullscreen fragments
 */
public abstract class AppFragment extends Fragment {
@StringRes
int mBackMessage;
@StringRes
int mBackPositiveActionText = R.string.discard;
private AppFragmentHelper mFragmentHelper = new AppFragmentHelper(this);
private Map<String, View> mSaveViews = new HashMap<>();

/**
 * Save the state of this view when this view is destroyed and restore it when it's created.
 * Also works for arguments. Be sure to call this method before {@link #onViewStateRestored(Bundle)}
 * is called.
 * @param view the view which state to save. Accepts: <ul> <li>{@link android.widget.EditText}</li>
 * </ul>
 * @param name name of the view to save it as
 * @throws IllegalArgumentException if the view class hasn't been implemented yet
 */
protected void addSaveView(View view, String name) {
	if (!(view instanceof EditText)) {
		throw new IllegalArgumentException(view.getClass().getName() + " not implemented");
	}
	
	mSaveViews.put(name, view);
}

/**
 * Show the current fragment
 */
public void show() {
	AppActivity activity = AppActivity.getActivity();
	FragmentTransaction fragmentTransaction = activity.getSupportFragmentManager().beginTransaction();
	fragmentTransaction.replace(android.R.id.content, this);
	fragmentTransaction.addToBackStack(getClass().getSimpleName());
	fragmentTransaction.commit();
}

/**
 * Set the toolbar and statusbar colors. Colors are applied in {@link #onViewCreated(View, Bundle)}
 * so call this method before then.
 * @param toolbarColor color of the toolbar
 * @param statusbarColor color of the statusbar
 */
protected void setToolbarColor(@ColorRes int toolbarColor, @ColorRes int statusbarColor) {
	mFragmentHelper.setToolbarColor(toolbarColor, statusbarColor);
}

/**
 * Set the message when back is pressed. This will be displayed in a small dialog
 * @param message the message to display in a small dialog
 */
protected void setBackMessage(@StringRes int message) {
	mBackMessage = message;
}

/**
 * Set the message when back is pressed. This will be displayed in a small dialog
 * @param message the message to display in a small dialog
 * @param positiveActionText the positive action button text of the dialog. By default this is
 * R.string.discard
 */
protected void setBackMessage(@StringRes int message, @StringRes int positiveActionText) {
	mBackMessage = message;
	mBackPositiveActionText = positiveActionText;
}

/**
 * Display back dialog discard message if something has been changed in the fragment. If nothing has
 * been changed it simply dismisses the window.
 */
public void back() {
	if (isChanged()) {
		MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(getContext());
		dialogBuilder.setMessage(mBackMessage);
		dialogBuilder.setPositiveButton(mBackPositiveActionText, (dialog, which) -> dismiss());
		dialogBuilder.setNegativeButton(R.string.cancel, null);
		dialogBuilder.show();
	} else {
		dismiss();
	}
}

/**
 * Automatically checks if any of the save views have been changed
 * @return true if values have been changed/edited
 */
protected boolean isChanged() {
	// Values
	for (Map.Entry<String, View> entry : mSaveViews.entrySet()) {
		String name = entry.getKey();
		View view = entry.getValue();
		Object originalValue = getArgument(name);
		
		if (view instanceof EditText) {
			String compareValue = "";
			if (originalValue != null) {
				compareValue = (String) originalValue;
			}
			
			if (!((EditText) view).getText().toString().equals(compareValue)) {
				return true;
			}
		}
	}
	
	return false;
}

public Context getContext() {
	if (Build.VERSION.SDK_INT >= 23) {
		return super.getContext();
	} else {
		return getActivity();
	}
}

@Override
public void onViewStateRestored(Bundle savedInstanceState) {
	super.onViewStateRestored(savedInstanceState);
	mFragmentHelper.onViewRestored(mView, savedInstanceState);
	
	for (Map.Entry<String, View> entry : mSaveViews.entrySet()) {
		String name = entry.getKey();
		View view = entry.getValue();
		
		if (view instanceof EditText) {
			String value = null;
			// Fetch value from saved instance
			if (savedInstanceState != null) {
				value = savedInstanceState.getString(name);
			}
			
			// Try with argument
			if (value == null) {
				value = getArgument(name);
			}
			
			if (value != null) {
				((EditText) view).setText(value);
			}
		}
	}
}

@Override
public void onResume() {
	super.onResume();
	mFragmentHelper.onResume();
}

@Override
public void onSaveInstanceState(Bundle outState) {
	super.onSaveInstanceState(outState);
	
	// Values
	for (Map.Entry<String, View> entry : mSaveViews.entrySet()) {
		String name = entry.getKey();
		View view = entry.getValue();
		
		if (view instanceof EditText) {
			outState.putString(name, ((EditText) view).getText().toString());
		}
	}
}

@Override
public void onPause() {
	super.onPause();
	mFragmentHelper.onPause();
}

@Override
public void onStop() {
	super.onStop();
	mFragmentHelper.onStop();
}

@Override
public void onDestroyView() {
	super.onDestroyView();
	mFragmentHelper.onDestroyView();
}

@Override
public void onDestroy() {
	super.onDestroy();
	mFragmentHelper.onDestroy();
}

/**
 * Dismiss this window.
 */
public void dismiss() {
	mFragmentHelper.dismiss();
}

@Override
public void onViewCreatedImpl(View view, @Nullable Bundle savedInstanceState) {
	super.onViewCreatedImpl(view, savedInstanceState);
}

public class BackOnClickListener implements View.OnClickListener {
	@Override
	public void onClick(View v) {
		back();
	}
}
}