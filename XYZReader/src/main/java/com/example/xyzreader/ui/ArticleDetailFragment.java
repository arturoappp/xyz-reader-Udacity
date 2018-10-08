package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * A fragment representing a single Article detail screen. This fragment is either contained in a
 * {@link ArticleListActivity} in two-pane mode (on tablets) or a {@link ArticleDetailActivity} on
 * handsets.
 */
public class ArticleDetailFragment extends Fragment
    implements LoaderManager.LoaderCallbacks<Cursor> {
  private static final String TAG = "ArticleDetailFragment";

  public static final String ARG_ITEM_ID = "item_id";
  private static final float PARALLAX_FACTOR = 1.25f;

  private Cursor mCursor;
  private long mItemId;
  private View mRootView;
  private ProgressBar progress;

  private ImageView mPhotoView;

  private TextView titleView;
  private TextView bylineView;
  // private TextView bodyView;
  private FloatingActionButton shareButton;
  private Toolbar toolbar;
  private LinearLayout metaBar;
  private CollapsingToolbarLayout collapsing_toolbar;
  private CardView card_text;

  private RecyclerView mRecyclerView;
  private RecyclerView.Adapter mAdapter;
  private RecyclerView.LayoutManager mLayoutManager;
  private String mTransitionName;

  /**
   * Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon
   * screen orientation changes).
   */
  public ArticleDetailFragment() {}

  public static ArticleDetailFragment newInstance(long itemId) {
    Bundle arguments = new Bundle();
    arguments.putLong(ARG_ITEM_ID, itemId);
    ArticleDetailFragment fragment = new ArticleDetailFragment();
    fragment.setArguments(arguments);
    return fragment;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getArguments().containsKey(ARG_ITEM_ID)) {
      mItemId = getArguments().getLong(ARG_ITEM_ID);
      setHasOptionsMenu(true);
    }
  }

  public ArticleDetailActivity getActivityCast() {
    return (ArticleDetailActivity) getActivity();
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    // In supptor library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
    // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
    // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
    // we do this in onActivityCreated.
    Typeface fontTypeface =
        TypeFaceProvider.getTypeFace(getActivity().getApplicationContext(), "Rosario-Regular.ttf");
    // bodyView.setTypeface(fontTypeface);
    bylineView.setTypeface(fontTypeface);
    titleView.setTypeface(fontTypeface);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

    progress = (ProgressBar) mRootView.findViewById(R.id.progress);
    titleView = (TextView) mRootView.findViewById(R.id.article_title);
    bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
    bylineView.setMovementMethod(new LinkMovementMethod());
    shareButton = (FloatingActionButton) mRootView.findViewById(R.id.share_fab);
    toolbar = (Toolbar) mRootView.findViewById(R.id.toolbar);
    metaBar = (LinearLayout) mRootView.findViewById(R.id.meta_bar);
    mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
    collapsing_toolbar = (CollapsingToolbarLayout) mRootView.findViewById(R.id.collapsing_toolbar);
    card_text = (CardView) mRootView.findViewById(R.id.card_text);
    mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.my_recycler_view);

    shareButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            if (mCursor != null) {
              startActivity(
                  Intent.createChooser(
                      ShareCompat.IntentBuilder.from(getActivity())
                          .setType("text/plain")
                          .setText(mCursor.getString(ArticleLoader.Query.TITLE))
                          .getIntent(),
                      getString(R.string.action_share)));
            }
          }
        });

    getLoaderManager().initLoader(0, null, this);

    if (mTransitionName != null
        && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      setTransitionNamesLollipop();
    }

    hideView();
    return mRootView;
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  private void setTransitionNamesLollipop() {
    mPhotoView.setTransitionName(mTransitionName);
  }

  public void setTransitionName(String transitionName) {
    mTransitionName = transitionName;
  }

  private void hideView() {
    collapsing_toolbar.setVisibility(View.INVISIBLE);
    card_text.setVisibility(View.INVISIBLE);
    shareButton.setVisibility(View.INVISIBLE);
    progress.setVisibility(View.VISIBLE);
  }

  private void showView() {
    collapsing_toolbar.setVisibility(View.VISIBLE);
    card_text.setVisibility(View.VISIBLE);
    shareButton.setVisibility(View.VISIBLE);
    progress.setVisibility(View.INVISIBLE);
    ActivityCompat.startPostponedEnterTransition(getActivity());
  }

  private void bindViews() {
    if (mRootView == null) {
      return;
    }
    if (mCursor != null) {
      mRootView.setVisibility(View.VISIBLE);

      if (toolbar != null) {
        ((ArticleDetailActivity) getActivity()).setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        ((ArticleDetailActivity) getActivity())
            .getSupportActionBar()
            .setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                getActivity().finish();
              }
            });
      }
      titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
      bylineView.setText(
          Html.fromHtml(
              DateUtils.getRelativeTimeSpanString(
                          mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                          System.currentTimeMillis(),
                          DateUtils.HOUR_IN_MILLIS,
                          DateUtils.FORMAT_ABBREV_ALL)
                      .toString()
                  + " by <font color='#ffffff'>"
                  + mCursor.getString(ArticleLoader.Query.AUTHOR)
                  + "</font>"));
      String body = mCursor.getString(ArticleLoader.Query.BODY);
      String[] split = body.split("\r\n");
      mRecyclerView.setHasFixedSize(true);
      mLayoutManager = new LinearLayoutManager(getActivity());
      mRecyclerView.setLayoutManager(mLayoutManager);
      mAdapter = new MyAdapter(split);
      mRecyclerView.setAdapter(mAdapter);

      Glide.with(getActivity())
          .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
          .diskCacheStrategy(DiskCacheStrategy.ALL)
          .centerCrop()
          .dontAnimate()
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
                  Palette.from(bitmap)
                      .generate(
                          new Palette.PaletteAsyncListener() {
                            @Override
                            public void onGenerated(Palette palette) {
                              int defaultColor = 0xFF333333;
                              int color = palette.getDarkVibrantColor(defaultColor);
                              metaBar.setBackgroundColor(color);
                              if (collapsing_toolbar != null) {
                                int scrimColor = palette.getDarkMutedColor(defaultColor);
                                collapsing_toolbar.setStatusBarScrimColor(scrimColor);
                                collapsing_toolbar.setContentScrimColor(scrimColor);
                              }
                            }
                          });

                  return false;
                }
              })
          .into(mPhotoView);

      ViewCompat.setTransitionName(mPhotoView, mCursor.getString(ArticleLoader.Query.TITLE));

      showView();
    } else {
      mRootView.setVisibility(View.GONE);
      Snackbar.make(collapsing_toolbar, R.string.errer_message, Snackbar.LENGTH_LONG).show();
    }
  }

  @Override
  public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
    if (!isAdded()) {
      if (cursor != null) {
        cursor.close();
      }
      return;
    }

    mCursor = cursor;
    if (mCursor != null && !mCursor.moveToFirst()) {
      Log.e(TAG, "Error reading item detail cursor");
      mCursor.close();
      mCursor = null;
    }

    bindViews();
  }

  @Override
  public void onLoaderReset(Loader<Cursor> cursorLoader) {
    mCursor = null;
  }

  public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private String[] mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class MyViewHolder extends RecyclerView.ViewHolder {
      // each data item is just a string in this case
      public TextView mTextView;

      public MyViewHolder(TextView v) {
        super(v);
        mTextView = v;
      }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyAdapter(String[] myDataset) {
      mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      // create a new view
      TextView v =
          (TextView)
              LayoutInflater.from(parent.getContext())
                  .inflate(R.layout.my_text_view, parent, false);
      MyViewHolder vh = new MyViewHolder(v);
      return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
      // - get element from your dataset at this position
      // - replace the contents of the view with that element
      Typeface fontTypeface =
          TypeFaceProvider.getTypeFace(
              getActivity().getApplicationContext(), "Rosario-Regular.ttf");
      holder.mTextView.setTypeface(fontTypeface);
      holder.mTextView.setText(Html.fromHtml(mDataset[position]));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
      return mDataset.length;
    }
  }
}
