package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Slide;
import android.view.View;
import android.view.ViewGroup;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.util.List;
import java.util.Map;


/** An activity representing a single Article detail screen, letting you swipe between articles. */
public class ArticleDetailActivity extends AppCompatActivity
    implements LoaderManager.LoaderCallbacks<Cursor> {

  private Cursor mCursor;
  private long mStartId;
  private long mSelectedItemId;
  private ViewPager mPager;
  private MyPagerAdapter mPagerAdapter;
  public static final String EXTRA_SELECTED_ID = "EXTRA_SELECTED_ID";
  private static final String KEY_CURRENT_POSITION = "currentPosition";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setupWindowAnimations();

    setContentView(R.layout.activity_article_detail);

    mPagerAdapter = new MyPagerAdapter(getFragmentManager());
    mPager = (ViewPager) findViewById(R.id.pager);
    mPager.setAdapter(mPagerAdapter);

    mPager.addOnPageChangeListener(
        new ViewPager.SimpleOnPageChangeListener() {
          @Override
          public void onPageScrollStateChanged(int state) {
            super.onPageScrollStateChanged(state);
          }

          @Override
          public void onPageSelected(int position) {
            ((ArticleDetailFragment) mPagerAdapter.getItem(position))
                .setTransitionName(mCursor.getString(ArticleLoader.Query.TITLE));

            ArticleListActivity.currentPosition = position;

            if (mCursor != null) {
              mCursor.moveToPosition(position);
            }
            mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
          }
        });

    if (savedInstanceState == null) {
      if (getIntent() != null && getIntent().getData() != null) {
        mStartId = ItemsContract.Items.getItemId(getIntent().getData());
        mSelectedItemId = mStartId;
      }
    } else {
      mSelectedItemId = savedInstanceState.getLong(EXTRA_SELECTED_ID);
      ArticleListActivity.currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION, 0);
    }
    getLoaderManager().initLoader(0, null, this);

    setEnterSharedElementCallback(
        new SharedElementCallback() {
          @Override
          public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            Fragment currentFragment =
                (Fragment)
                    mPager
                        .getAdapter()
                        .instantiateItem(mPager, ArticleListActivity.currentPosition);
            View view = currentFragment.getView();
            if (view == null) {
              return;
            }

            // Map the first shared element name to the child ImageView.
            sharedElements.put(names.get(0), view.findViewById(R.id.photo));
          }
        });
  }

  private void setupWindowAnimations() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

      ActivityCompat.postponeEnterTransition(this);
      Fade fade = new Fade();
      fade.setDuration(1000);
      getWindow().setEnterTransition(fade);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putLong(EXTRA_SELECTED_ID, mSelectedItemId);
    outState.putInt(KEY_CURRENT_POSITION, ArticleListActivity.currentPosition);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    return ArticleLoader.newAllArticlesInstance(this);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
    mCursor = cursor;
    mPagerAdapter.notifyDataSetChanged();

    // Select the start ID
    if (mStartId > 0) {
      mCursor.moveToFirst();
      // TODO: optimize
      while (!mCursor.isAfterLast()) {
        if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
          final int position = mCursor.getPosition();
          mPager.setCurrentItem(position, false);
          break;
        }
        mCursor.moveToNext();
      }
      mStartId = 0;
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> cursorLoader) {
    mCursor = null;
    mPagerAdapter.notifyDataSetChanged();
  }

  private class MyPagerAdapter extends FragmentStatePagerAdapter {
    public MyPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
      super.setPrimaryItem(container, position, object);
    }

    @Override
    public Fragment getItem(int position) {
      mCursor.moveToPosition(position);
      return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
    }

    @Override
    public int getCount() {
      return (mCursor != null) ? mCursor.getCount() : 0;
    }
  }
}
