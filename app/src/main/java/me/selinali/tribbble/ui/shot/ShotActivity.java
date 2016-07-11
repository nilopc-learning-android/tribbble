package me.selinali.tribbble.ui.shot;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

import org.parceler.Parcels;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.selinali.tribbble.R;
import me.selinali.tribbble._;
import me.selinali.tribbble.api.Dribble;
import me.selinali.tribbble.model.Comment;
import me.selinali.tribbble.model.Shot;
import me.selinali.tribbble.ui.common.DividerItemDecoration;
import me.selinali.tribbble.utils.ViewUtils;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class ShotActivity extends AppCompatActivity {

  private static final String EXTRA_SHOT = "EXTRA_SHOT";

  public static Intent launchIntentFor(Shot shot, Context context) {
    return new Intent(context, ShotActivity.class)
        .putExtra(EXTRA_SHOT, Parcels.wrap(shot));
  }

  @BindView(R.id.toolbar) Toolbar mToolbar;
  @BindView(R.id.shot_content_container) View mShotContentContainer;
  @BindView(R.id.shot_details_view) ShotDetailsView mShotDetailsView;
  @BindView(R.id.imageview_shot) ImageView mShotImageView;
  @BindView(R.id.recyclerview_comments) RecyclerView mCommentsRecyclerView;
  @BindView(R.id.progress_container) View mProgressContainer;

  private Shot mShot;
  private Subscription mShotSubscription;

  private interface ImageLoader {
    void load(Action1<Bitmap> futureBitmap);
  }

  private final ImageLoader mStaticImageLoader = callback ->
      Glide.with(this)
          .load(mShot.getImages().getHighResImage())
          .placeholder(R.drawable.grid_item_placeholder)
          .diskCacheStrategy(DiskCacheStrategy.SOURCE)
          .into(new GlideDrawableImageViewTarget(mShotImageView) {
            @Override public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> animation) {
              super.onResourceReady(resource, animation);
              callback.call(((GlideBitmapDrawable) resource).getBitmap());
            }
          });

  private final ImageLoader mGifImageLoader = callback ->
      Glide.with(this)
          .load(mShot.getImages().getHighResImage())
          .placeholder(R.drawable.grid_item_placeholder)
          .diskCacheStrategy(DiskCacheStrategy.SOURCE)
          .into(new GlideDrawableImageViewTarget(mShotImageView) {
            @Override public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> animation) {
              super.onResourceReady(resource, animation);
              callback.call(((GifDrawable) resource).getFirstFrame());
            }
          });

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_shot);
    ButterKnife.bind(this);
    setupPadding();
    overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);

    mShot = Parcels.unwrap(getIntent().getParcelableExtra(EXTRA_SHOT));

    setSupportActionBar(mToolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setDisplayShowTitleEnabled(false);

    (mShot.isAnimated() ? mGifImageLoader : mStaticImageLoader).load(bitmap -> {
      Palette.from(bitmap).maximumColorCount(8)
          .generate(palette -> mShotDetailsView.bind(palette.getSwatches()));
    });

    mShotSubscription = Dribble.instance()
        .getShot(mShot.getId())
        .onErrorReturn(t -> mShot)
        .flatMap(shot -> {
          Observable<List<Comment>> comments = Dribble.instance().getComments(shot);
          return Observable.zip(Observable.just(shot), comments, Shot::withComments);
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::bindShot, throwable -> {
          // TODO
        });
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    _.unsubscribe(mShotSubscription);
  }

  private void bindShot(Shot shot) {
    mShot = shot;
    mShotDetailsView.bind(shot);
    mCommentsRecyclerView.setNestedScrollingEnabled(false);
    mCommentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    mCommentsRecyclerView.addItemDecoration(new DividerItemDecoration(this));
    mCommentsRecyclerView.setAdapter(new CommentsAdapter(shot.getComments()));
    ViewCompat.animate(mShotContentContainer).alpha(1f).setDuration(200);
  }

  private void setupPadding() {
    mShotContentContainer.setPadding(
        mShotContentContainer.getPaddingLeft(),
        mShotContentContainer.getPaddingTop(),
        mShotContentContainer.getPaddingRight(),
        ViewUtils.getNavigationBarHeight());
  }

  @Override public void onBackPressed() {
    super.onBackPressed();
    overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
