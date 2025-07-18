/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017-2020 Andy Scherzinger <info@andy-scherzinger>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Chawki Chouib <chouibc@gmail.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageView;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.nextcloud.client.account.User;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.common.NextcloudClient;
import com.nextcloud.utils.GlideHelper;
import com.nextcloud.utils.extensions.BundleExtensionsKt;
import com.owncloud.android.R;
import com.owncloud.android.databinding.UserInfoDetailsTableItemBinding;
import com.owncloud.android.databinding.UserInfoLayoutBinding;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation;
import com.owncloud.android.ui.dialog.AccountRemovalDialog;
import com.owncloud.android.ui.events.TokenPushEvent;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PushUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import kotlin.Unit;

/**
 * This Activity presents the user information.
 */
public class UserInfoActivity extends DrawerActivity implements Injectable {
    public static final String KEY_ACCOUNT = "ACCOUNT";

    private static final String TAG = UserInfoActivity.class.getSimpleName();
    public static final String KEY_USER_DATA = "USER_DATA";

    @Inject AppPreferences preferences;
    private float mCurrentAccountAvatarRadiusDimension;

    private UserInfo userInfo;
    private User user;
    private UserInfoLayoutBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);
        Bundle bundle = getIntent().getExtras();

        if (bundle == null) {
            finish();
            return;
        }

        user = BundleExtensionsKt.getParcelableArgument(bundle, KEY_ACCOUNT, User.class);
        if(user == null) {
            finish();
            return;
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_USER_DATA)) {
            userInfo = BundleExtensionsKt.getParcelableArgument(savedInstanceState, KEY_USER_DATA, UserInfo.class);
        } else if (bundle.containsKey(KEY_ACCOUNT)) {
            userInfo =  BundleExtensionsKt.getParcelableArgument(bundle, KEY_USER_DATA, UserInfo.class);
        }

        mCurrentAccountAvatarRadiusDimension = getResources().getDimension(R.dimen.user_icon_radius);

        binding = UserInfoLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();

        // set the back button from action bar
        ActionBar actionBar = getSupportActionBar();

        // check if is not null
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            viewThemeUtils.files.themeActionBar(this, actionBar);
        }

        binding.userinfoList.setAdapter(new UserInfoAdapter(null, viewThemeUtils));

        if (userInfo != null) {
            populateUserInfoUi(userInfo);
        } else {
            setMultiListLoadingMessage();
            fetchAndSetData();
        }

        setHeaderImage();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (accountManager.getUser().equals(user)) {
            menu.findItem(R.id.action_open_account).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.item_account, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            onBackPressed();
        } else if (itemId == R.id.action_open_account) {
            accountClicked(user.hashCode());
        } else if (itemId == R.id.action_delete_account) {
            openAccountRemovalDialog(user, getSupportFragmentManager());
        } else {
            retval = super.onOptionsItemSelected(item);
        }

        return retval;
    }

    private void setMultiListLoadingMessage() {
        binding.userinfoList.setVisibility(View.GONE);
        binding.emptyList.emptyListView.setVisibility(View.GONE);
    }

    private void setErrorMessageForMultiList(String headline, String message, @DrawableRes int errorResource) {
        binding.emptyList.emptyListViewHeadline.setText(headline);
        binding.emptyList.emptyListViewText.setText(message);
        binding.emptyList.emptyListIcon.setImageResource(errorResource);

        binding.emptyList.emptyListIcon.setVisibility(View.VISIBLE);
        binding.emptyList.emptyListViewText.setVisibility(View.VISIBLE);
        binding.userinfoList.setVisibility(View.GONE);
        binding.loadingContent.setVisibility(View.GONE);
    }

    private void setHeaderImage() {
        if (getStorageManager().getCapability(user.getAccountName()).getServerBackground() == null) {
            return;
        }

        ImageView backgroundImageView = findViewById(R.id.userinfo_background);
        if (backgroundImageView == null) {
            return;
        }

        String backgroundURL = getStorageManager().getCapability(user.getAccountName()).getServerBackground();
        if (backgroundURL == null) {
            return;
        }

        if (!URLUtil.isValidUrl(backgroundURL)) {
            final Drawable drawable = viewThemeUtils.platform.getPrimaryColorDrawable(backgroundImageView.getContext());
            backgroundImageView.setImageDrawable(drawable);
            return;
        }

        Target<Drawable> backgroundImageTarget = createBackgroundImageTarget(backgroundImageView);
        getClientRepository().getNextcloudClient(nextcloudClient -> {
            GlideHelper.INSTANCE.loadIntoTarget(this,
                                                nextcloudClient,
                                                backgroundURL,
                                                backgroundImageTarget,
                                                R.drawable.background);
            return Unit.INSTANCE;
        });
    }

    private Target<Drawable> createBackgroundImageTarget(ImageView backgroundImageView) {
        return new CustomTarget<>() {
            @Override
            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                Drawable[] drawables = {
                    viewThemeUtils.platform.getPrimaryColorDrawable(backgroundImageView.getContext()),
                    resource
                };
                LayerDrawable layerDrawable = new LayerDrawable(drawables);
                backgroundImageView.setImageDrawable(layerDrawable);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                Drawable fallback = ResourcesCompat.getDrawable(backgroundImageView.getResources(), R.drawable.background, null);

                Drawable[] drawables = {
                    viewThemeUtils.platform.getPrimaryColorDrawable(backgroundImageView.getContext()),
                    fallback
                };
                LayerDrawable layerDrawable = new LayerDrawable(drawables);
                backgroundImageView.setImageDrawable(layerDrawable);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }
        };
    }


    private void populateUserInfoUi(UserInfo userInfo) {
        binding.userinfoUsername.setText(user.getAccountName());
        binding.userinfoIcon.setTag(user.getAccountName());
        DisplayUtils.setAvatar(user,
                               this,
                               mCurrentAccountAvatarRadiusDimension,
                               getResources(),
                               binding.userinfoIcon,
                               this);

        if (!TextUtils.isEmpty(userInfo.getDisplayName())) {
            binding.userinfoFullName.setText(userInfo.getDisplayName());
        }

        if (TextUtils.isEmpty(userInfo.getPhone()) && TextUtils.isEmpty(userInfo.getEmail())
            && TextUtils.isEmpty(userInfo.getAddress()) && TextUtils.isEmpty(userInfo.getTwitter())
            && TextUtils.isEmpty(userInfo.getWebsite())) {
            binding.userinfoList.setVisibility(View.GONE);
            binding.loadingContent.setVisibility(View.GONE);
            binding.emptyList.emptyListView.setVisibility(View.VISIBLE);

            setErrorMessageForMultiList(getString(R.string.userinfo_no_info_headline),
                                        getString(R.string.userinfo_no_info_text), R.drawable.ic_user_outline);
        } else {
            binding.loadingContent.setVisibility(View.VISIBLE);
            binding.emptyList.emptyListView.setVisibility(View.GONE);

            if (binding.userinfoList.getAdapter() instanceof UserInfoAdapter) {
                binding.userinfoList.setAdapter(new UserInfoAdapter(createUserInfoDetails(userInfo), viewThemeUtils));
            }

            binding.loadingContent.setVisibility(View.GONE);
            binding.userinfoList.setVisibility(View.VISIBLE);
        }
    }

    private List<UserInfoDetailsItem> createUserInfoDetails(UserInfo userInfo) {
        List<UserInfoDetailsItem> result = new LinkedList<>();

        addToListIfNeeded(result, R.drawable.ic_phone, userInfo.getPhone(), R.string.user_info_phone);
        addToListIfNeeded(result, R.drawable.ic_email, userInfo.getEmail(), R.string.user_info_email);
        addToListIfNeeded(result, R.drawable.ic_map_marker, userInfo.getAddress(), R.string.user_info_address);
        addToListIfNeeded(result, R.drawable.ic_web, DisplayUtils.beautifyURL(userInfo.getWebsite()),
                    R.string.user_info_website);
        addToListIfNeeded(result, R.drawable.ic_twitter, DisplayUtils.beautifyTwitterHandle(userInfo.getTwitter()),
                    R.string.user_info_twitter);

        return result;
    }

    private void addToListIfNeeded(List<UserInfoDetailsItem> info, @DrawableRes int icon, String text,
                                   @StringRes int contentDescriptionInt) {
        if (!TextUtils.isEmpty(text)) {
            info.add(new UserInfoDetailsItem(icon, text, getResources().getString(contentDescriptionInt)));
        }
    }

    public static void openAccountRemovalDialog(User user, FragmentManager fragmentManager) {
        AccountRemovalDialog dialog = AccountRemovalDialog.newInstance(user);
        dialog.show(fragmentManager, "dialog");
    }



    private void fetchAndSetData() {
        Thread t = new Thread(() -> {
            NextcloudClient nextcloudClient;

            try {
                nextcloudClient = OwnCloudClientFactory.createNextcloudClient(user,
                                                                              this);
            } catch (AccountUtils.AccountNotFoundException e) {
                Log_OC.e(this, "Error retrieving user info", e);
                return;
            }

            RemoteOperationResult<UserInfo> result = new GetUserInfoRemoteOperation().execute(nextcloudClient);

            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                if (result.isSuccess() && result.getResultData() != null) {
                    userInfo = result.getResultData();

                    runOnUiThread(() -> populateUserInfoUi(userInfo));
                } else {
                    // show error
                    runOnUiThread(() -> setErrorMessageForMultiList(
                        getString(R.string.user_information_retrieval_error),
                        result.getLogMessage(this),
                        R.drawable.ic_list_empty_error)
                                 );
                    Log_OC.d(TAG, result.getLogMessage());
                }
            }
        });

        t.start();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (userInfo != null) {
            outState.putParcelable(KEY_USER_DATA, userInfo);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(TokenPushEvent event) {
        PushUtils.pushRegistrationToServer(getUserAccountManager(), preferences.getPushToken());
    }


    protected static class UserInfoDetailsItem {
        @DrawableRes public int icon;
        public String text;
        public String iconContentDescription;

        public UserInfoDetailsItem(@DrawableRes int icon, String text, String iconContentDescription) {
            this.icon = icon;
            this.text = text;
            this.iconContentDescription = iconContentDescription;
        }
    }

    protected static class UserInfoAdapter extends RecyclerView.Adapter<UserInfoAdapter.ViewHolder> {
        protected List<UserInfoDetailsItem> mDisplayList;
        protected ViewThemeUtils viewThemeUtils;

        public static class ViewHolder extends RecyclerView.ViewHolder {
            protected UserInfoDetailsTableItemBinding binding;

            public ViewHolder(UserInfoDetailsTableItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        public UserInfoAdapter(List<UserInfoDetailsItem> displayList, ViewThemeUtils viewThemeUtils) {
            mDisplayList = displayList == null ? new LinkedList<>() : displayList;
            this.viewThemeUtils = viewThemeUtils;
        }

        public void setData(List<UserInfoDetailsItem> displayList) {
            mDisplayList = displayList == null ? new LinkedList<>() : displayList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(
                UserInfoDetailsTableItemBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false)
            );
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UserInfoDetailsItem item = mDisplayList.get(position);
            holder.binding.icon.setImageResource(item.icon);
            holder.binding.text.setText(item.text);
            holder.binding.icon.setContentDescription(item.iconContentDescription);
            viewThemeUtils.platform.colorImageView(holder.binding.icon);
        }

        @Override
        public int getItemCount() {
            return mDisplayList.size();
        }
    }
}
