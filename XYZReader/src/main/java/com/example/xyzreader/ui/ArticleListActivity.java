package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.format.DateUtils;
import android.transition.Explode;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity
    implements LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener {

  private static final String TAG = ArticleListActivity.class.toString();
  private SwipeRefreshLayout mSwipeRefreshLayout;
  private RecyclerView mRecyclerView;
  private CoordinatorLayout mCoordinatorLayout;

  public static int currentPosition;
  private static final String KEY_CURRENT_POSITION = "currentPosition";

  private BroadcastReceiver mRefreshingReceiver =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
            boolean mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
            updateRefreshingUI(mIsRefreshing);
          }
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setupWindowAnimations();
    setContentView(R.layout.activity_article_list);
    mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordiatorLayout);

    mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
    mSwipeRefreshLayout.setOnRefreshListener(this);

    mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
    getLoaderManager().initLoader(0, null, this);

    if (savedInstanceState == null) {
      onRefresh();
    } else {
      currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION, 0);
    }
    setExitSharedElementCallback(
        new SharedElementCallback() {
          @Override
          public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            // Locate the ViewHolder for the clicked position.
            RecyclerView.ViewHolder selectedViewHolder =
                mRecyclerView.findViewHolderForAdapterPosition(ArticleListActivity.currentPosition);
            if (selectedViewHolder == null || selectedViewHolder.itemView == null) {
              return;
            }

            // Map the first shared element name to the child ImageView.
            sharedElements.put(
                names.get(0), selectedViewHolder.itemView.findViewById(R.id.thumbnail));
          }
        });
  }

  // https://gist.github.com/lopspower/1a0b4e0c50d90fbf2379
  private void setupWindowAnimations() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      Explode explode = new Explode();
      explode.setDuration(1000);
      //getWindow().setExitTransition(explode);
    }
  }

  @Override
  public void onRefresh() {
    startService(new Intent(this, UpdaterService.class));
    Snackbar.make(mCoordinatorLayout, R.string.refreshed, Snackbar.LENGTH_LONG)
        .setAction(R.string.OK, null)
        .setActionTextColor(Color.GREEN)
        .show();
  }

  @Override
  protected void onStart() {
    super.onStart();
    registerReceiver(
        mRefreshingReceiver, new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
  }

  @Override
  protected void onStop() {
    super.onStop();
    unregisterReceiver(mRefreshingReceiver);
  }

  private void updateRefreshingUI(boolean mIsRefreshing) {
    mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    return ArticleLoader.newAllArticlesInstance(this);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
    Adapter adapter = new Adapter(cursor);
    adapter.setHasStableIds(true);
    mRecyclerView.setAdapter(adapter);
    int columnCount = getResources().getInteger(R.integer.list_column_count);
    StaggeredGridLayoutManager sglm =
        new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
    mRecyclerView.setLayoutManager(sglm);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    mRecyclerView.setAdapter(null);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(KEY_CURRENT_POSITION, currentPosition);
  }

  private class Adapter extends RecyclerView.Adapter<ViewHolder> {
    private Cursor mCursor;

    public Adapter(Cursor cursor) {
      mCursor = cursor;
    }

    @Override
    public long getItemId(int position) {
      mCursor.moveToPosition(position);
      return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
      final ViewHolder vh = new ViewHolder(view);
      view.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              Uri uri = ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()));
              Intent intent = new Intent(ArticleListActivity.this, ArticleDetailActivity.class);
              intent.setData(uri);
              // startActivity(intent);

              View sharedView = vh.thumbnailView;
              String transitionName = getString(R.string.transition_photo);
              ViewCompat.setTransitionName(vh.thumbnailView, vh.titleView.getText().toString());

              ActivityOptionsCompat activityOptionsCompat =
                  ActivityOptionsCompat.makeSceneTransitionAnimation(
                      ArticleListActivity.this,
                      sharedView,
                      ViewCompat.getTransitionName(vh.thumbnailView));

              startActivity(intent, activityOptionsCompat.toBundle());

              //              Bundle bundle =
              // ActivityOptions.makeSceneTransitionAnimation(ArticleListActivity.this).toBundle();
              //              startActivity(intent,bundle);

              //              startActivity(intent,
              //
              // ActivityOptions.makeSceneTransitionAnimation(ArticleListActivity.this).toBundle());

            }
          });
      return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
      mCursor.moveToPosition(position);
      holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
      holder.subtitleView.setText(
          DateUtils.getRelativeTimeSpanString(
                      mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                      System.currentTimeMillis(),
                      DateUtils.HOUR_IN_MILLIS,
                      DateUtils.FORMAT_ABBREV_ALL)
                  .toString()
              + " by "
              + mCursor.getString(ArticleLoader.Query.AUTHOR));

      Glide.clear(holder.thumbnailView);
      Glide.with(holder.thumbnailView.getContext())
          .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
          .diskCacheStrategy(DiskCacheStrategy.ALL)
          .dontAnimate()
          .centerCrop()
          .listener(
              new RequestListener<String, GlideDrawable>() {
                @Override
                public boolean onException(
                    Exception e,
                    String model,
                    Target<GlideDrawable> target,
                    boolean isFirstResource) {
                  return false;
                }

                @Override
                public boolean onResourceReady(
                    GlideDrawable resource,
                    String model,
                    Target<GlideDrawable> target,
                    boolean isFromMemoryCache,
                    boolean isFirstResource) {
                  Bitmap bitmap = ((GlideBitmapDrawable) resource.getCurrent()).getBitmap();
                  Palette palette = Palette.generate(bitmap);
                  int defaultColor = 0xFF333333;
                  int color = palette.getVibrantColor(defaultColor);
                  holder.itemView.setBackgroundColor(color);
                  return false;
                }
              })
          .into(holder.thumbnailView);
    }

    @Override
    public int getItemCount() {
      return mCursor.getCount();
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    public ImageView thumbnailView;
    public TextView titleView;
    public TextView subtitleView;

    public ViewHolder(View view) {
      super(view);
      thumbnailView = (ImageView) view.findViewById(R.id.thumbnail);
      titleView = (TextView) view.findViewById(R.id.article_title);
      subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
    }
  }
}
