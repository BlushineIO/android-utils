package com.spiddekauga.android.ui.list;

import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.spiddekauga.android.AppActivity;
import com.spiddekauga.android.R;
import com.spiddekauga.android.ui.ColorHelper;
import com.spiddekauga.android.ui.Toaster;

import java.util.HashMap;
import java.util.Map;

/**
 * Add ability to remove items by swiping
 */
class SwipeRemoveFunctionality<T> extends AdapterFunctionality implements ViewHolderFunctionality {
private boolean mUndoFunctionality = false;
private String mRemovedMessage = AppActivity.getActivity().getResources().getString(R.string.item_removed);
private RemoveListener<T> mListener;
@ColorInt
private int mColor = ColorHelper.getColor(AppActivity.getActivity().getResources(), R.color.cancel, null);
private AdvancedAdapter<T, ?> mAdapter;
private Map<T, Runnable> mPendingRemoves = new HashMap<>();
private Handler mHandler = new Handler();

public SwipeRemoveFunctionality(AdvancedAdapter<T, ?> adapter, RemoveListener<T> listener) {
	mAdapter = adapter;
	mListener = listener;
}

public SwipeRemoveFunctionality(AdvancedAdapter<T, ?> adapter, RemoveListener<T> listener, boolean undoFunctionality) {
	mAdapter = adapter;
	mListener = listener;
	mUndoFunctionality = undoFunctionality;
}

public SwipeRemoveFunctionality(AdvancedAdapter<T, ?> adapter, RemoveListener<T> listener, boolean undoFunctionality, String removedMessage) {
	mAdapter = adapter;
	mListener = listener;
	mUndoFunctionality = undoFunctionality;
	mRemovedMessage = removedMessage;
}

public UndoViewHolder onCreateViewHolder(ViewGroup parent) {
	View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.undo_list_item, parent, false);
	return new UndoViewHolder(itemView);
}

@Override
public int getViewType() {
	return AdvancedAdapter.ViewTypes.UNDO.ordinal();
}

@Override
public Class<UndoViewHolder> getViewHolderClass() {
	return UndoViewHolder.class;
}

@Override
public void onBindViewHolder(AdvancedAdapter<?, ?> adapter, RecyclerView.ViewHolder view, int position) {
	UndoViewHolder undoView = (UndoViewHolder) view;
	undoView.itemView.setBackgroundColor(mColor);
	undoView.mRemovedTextView.setText(mRemovedMessage);

	if (mUndoFunctionality) {
		final T item = mAdapter.getItem(position);
		undoView.mUndoButton.setVisibility(View.VISIBLE);
		undoView.mUndoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Runnable pendingRemovalRunnable = mPendingRemoves.get(item);
				if (pendingRemovalRunnable != null) {
					mPendingRemoves.remove(item);
					mHandler.removeCallbacks(pendingRemovalRunnable);
					mAdapter.removeItemViewHolder(item, SwipeRemoveFunctionality.this);
					int currentPos = mAdapter.getItemPosition(item);
					if (currentPos != -1) {
						mAdapter.notifyItemChanged(currentPos);
					}
				}
			}
		});
	} else {
		undoView.mUndoButton.setVisibility(View.GONE);
	}
}

@Override
protected void applyFunctionality(AdvancedAdapter<?, ?> adapter, RecyclerView recyclerView) {
	// Item touch helper
	ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new TransactionRemoveCallback());
	itemTouchHelper.attachToRecyclerView(recyclerView);

	// Red background when erasing
	BackgroundDecoration backgroundDecoration = new BackgroundDecoration();
	recyclerView.addItemDecoration(backgroundDecoration);

}

/**
 * Undo ViewHolder that views the undo functionality
 */
static class UndoViewHolder extends RecyclerView.ViewHolder {
	private TextView mRemovedTextView;
	private Button mUndoButton;

	public UndoViewHolder(View itemView) {
		super(itemView);
		mRemovedTextView = (TextView) itemView.findViewById(R.id.remove_message);
		mUndoButton = (Button) itemView.findViewById(R.id.undo_button);
	}
}

/**
 * Callback when a transaction has been swiped (and should be removed)
 */
class TransactionRemoveCallback extends ItemTouchHelper.Callback {
	private static final int UNDO_DURATION = 3000; // 3sec
	private final int mXMargin = (int) AppActivity.getActivity().getResources().getDimension(R.dimen.margin);
	private Drawable mBackground = new ColorDrawable(mColor);
	private Drawable mXMark = ContextCompat.getDrawable(AppActivity.getActivity(), R.drawable.clear_36dp);

	@Override
	public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
		// Don't allow undo view holders to be swiped
		if (viewHolder instanceof UndoViewHolder) {
			return 0;
		} else {
			int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
			return makeMovementFlags(0, swipeFlags);
		}
	}

	@Override
	public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
		return false;
	}

	@Override
	public boolean isItemViewSwipeEnabled() {
		return true;
	}

	@Override
	public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
		int swipedPosition = viewHolder.getAdapterPosition();
		final T item = mAdapter.getItem(swipedPosition);

		// Ability to undo
		if (mUndoFunctionality) {
			mAdapter.setItemViewHolder(item, SwipeRemoveFunctionality.this);
			Runnable pendingRemovalRunnable = new Runnable() {
				@Override
				public void run() {
					onRemove(item);
				}
			};
			mPendingRemoves.put(item, pendingRemovalRunnable);
			mAdapter.notifyItemChanged(swipedPosition);
			mHandler.postDelayed(pendingRemovalRunnable, UNDO_DURATION);
		}
		// No undo
		else {
			Toaster.show(mRemovedMessage);
			onRemove(item);
		}
	}

	@Override
	public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
		View itemView = viewHolder.itemView;

		// not sure why, but this method get's called for viewholder that are already swiped away
		if (viewHolder.getAdapterPosition() == -1) {
			// not interested in those
			return;
		}

		// draw red background
		// To the right
		if (dX < 0) {
			mBackground.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
		}
		// To the left
		else {
			mBackground.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + (int) dX, itemView.getBottom());
		}

		mBackground.draw(c);

		// draw x mark
		int itemHeight = itemView.getBottom() - itemView.getTop();
		int intrinsicWidth = mXMark.getIntrinsicWidth();
		int intrinsicHeight = mXMark.getIntrinsicWidth();
		int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
		int xMarkBottom = xMarkTop + intrinsicHeight;
		int xMarkLeft;
		int xMarkRight;

		// Right
		if (dX < 0) {
			xMarkLeft = itemView.getRight() - mXMargin - intrinsicWidth;
			xMarkRight = itemView.getRight() - mXMargin;
		}
		// Left
		else {
			xMarkLeft = itemView.getLeft() + mXMargin;
			xMarkRight = itemView.getLeft() + mXMargin + intrinsicWidth;
		}
		mXMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);
		mXMark.draw(c);

		super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
	}

	private void onRemove(T item) {
		mAdapter.remove(item);
		mPendingRemoves.remove(item);
		if (mListener != null) {
			mListener.onRemoved(item);
		}
	}
}

class BackgroundDecoration extends RecyclerView.ItemDecoration {
	private Drawable mBackground = new ColorDrawable(mColor);

	@Override
	public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
		// only if animation is in progress
		if (parent.getItemAnimator().isRunning()) {

			// some items might be animating down and some items might be animating up to close the gap left by the removed item
			// this is not exclusive, both movement can be happening at the same time
			// to reproduce this leave just enough items so the first one and the last one would be just a little off screen
			// then remove one from the middle

			// find first child with translationY > 0
			// and last one with translationY < 0
			// we're after a rect that is not covered in recycler-view views at this point in time
			View lastViewComingDown = null;
			View firstViewComingUp = null;

			// this is fixed
			int left = 0;
			int right = parent.getWidth();

			// this we need to find out
			int top = 0;
			int bottom = 0;

			// find relevant translating views
			int childCount = parent.getLayoutManager().getChildCount();
			for (int i = 0; i < childCount; i++) {
				View child = parent.getLayoutManager().getChildAt(i);
				if (child.getTranslationY() < 0) {
					// view is coming down
					lastViewComingDown = child;
				} else if (child.getTranslationY() > 0) {
					// view is coming up
					if (firstViewComingUp == null) {
						firstViewComingUp = child;
					}
				}
			}

			if (lastViewComingDown != null && firstViewComingUp != null) {
				// views are coming down AND going up to fill the void
				top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
				bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
			} else if (lastViewComingDown != null) {
				// views are going down to fill the void
				top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
				bottom = lastViewComingDown.getBottom();
			} else if (firstViewComingUp != null) {
				// views are coming up to fill the void
				top = firstViewComingUp.getTop();
				bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
			}

			mBackground.setBounds(left, top, right, bottom);
			mBackground.draw(c);

		}
		super.onDraw(c, parent, state);
	}
}
}