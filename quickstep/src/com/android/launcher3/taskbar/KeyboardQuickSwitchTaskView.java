/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.taskbar;

import static com.android.quickstep.util.BorderAnimator.DEFAULT_BORDER_COLOR;

import android.animation.Animator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.launcher3.R;
import com.android.quickstep.util.BorderAnimator;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.recents.model.ThumbnailData;

import java.util.function.Consumer;

/**
 * A view that displays a recent task during a keyboard quick switch.
 */
public class KeyboardQuickSwitchTaskView extends ConstraintLayout {

    @NonNull private final BorderAnimator mBorderAnimator;

    @Nullable private ImageView mThumbnailView1;
    @Nullable private ImageView mThumbnailView2;
    @Nullable private ImageView mIcon1;
    @Nullable private ImageView mIcon2;
    @Nullable private View mContent;

    public KeyboardQuickSwitchTaskView(@NonNull Context context) {
        this(context, null);
    }

    public KeyboardQuickSwitchTaskView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyboardQuickSwitchTaskView(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public KeyboardQuickSwitchTaskView(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        TypedArray ta = context.obtainStyledAttributes(
                attrs, R.styleable.TaskView, defStyleAttr, defStyleRes);

        setWillNotDraw(false);
        Resources resources = context.getResources();
        mBorderAnimator = new BorderAnimator(
                /* borderBoundsBuilder= */ bounds -> bounds.set(0, 0, getWidth(), getHeight()),
                /* borderWidthPx= */ resources.getDimensionPixelSize(
                        R.dimen.keyboard_quick_switch_border_width),
                /* borderRadiusPx= */ resources.getDimensionPixelSize(
                        R.dimen.keyboard_quick_switch_task_view_radius),
                /* borderColor= */ ta.getColor(
                        R.styleable.TaskView_borderColor, DEFAULT_BORDER_COLOR),
                /* invalidateViewCallback= */ KeyboardQuickSwitchTaskView.this::invalidate,
                /* viewScaleTargetProvider= */ new BorderAnimator.ViewScaleTargetProvider() {
                    @NonNull
                    @Override
                    public View getContainerView() {
                        return KeyboardQuickSwitchTaskView.this;
                    }

                    @NonNull
                    @Override
                    public View getContentView() {
                        return mContent;
                    }
                });
        ta.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mThumbnailView1 = findViewById(R.id.thumbnail1);
        mThumbnailView2 = findViewById(R.id.thumbnail2);
        mIcon1 = findViewById(R.id.icon1);
        mIcon2 = findViewById(R.id.icon2);
        mContent = findViewById(R.id.content);
    }

    @NonNull
    protected Animator getFocusAnimator(boolean focused) {
        return mBorderAnimator.buildAnimator(focused);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        mBorderAnimator.drawBorder(canvas);
    }

    protected void setThumbnails(
            @NonNull Task task1,
            @Nullable Task task2,
            @Nullable ThumbnailUpdateFunction thumbnailUpdateFunction,
            @Nullable IconUpdateFunction iconUpdateFunction) {
        applyThumbnail(mThumbnailView1, task1, thumbnailUpdateFunction);
        applyThumbnail(mThumbnailView2, task2, thumbnailUpdateFunction);

        if (iconUpdateFunction == null) {
            setContentDescription(task2 == null
                    ? task1.titleDescription
                    : getContext().getString(
                            R.string.quick_switch_split_task,
                            task1.titleDescription,
                            task2.titleDescription));
            return;
        }
        iconUpdateFunction.updateIconInBackground(task1, t -> {
            applyIcon(mIcon1, task1);
            if (task2 != null) {
                return;
            }
            setContentDescription(task1.titleDescription);
        });
        if (task2 == null) {
            return;
        }
        iconUpdateFunction.updateIconInBackground(task2, t -> {
            applyIcon(mIcon2, task2);
            setContentDescription(getContext().getString(
                    R.string.quick_switch_split_task,
                    task1.titleDescription,
                    task2.titleDescription));
        });
    }

    private void applyThumbnail(
            @Nullable ImageView thumbnailView,
            @Nullable Task task,
            @Nullable ThumbnailUpdateFunction updateFunction) {
        if (thumbnailView == null) {
            return;
        }
        if (task == null) {
            return;
        }
        if (updateFunction == null) {
            applyThumbnail(thumbnailView, task.thumbnail);
            return;
        }
        updateFunction.updateThumbnailInBackground(
                task, thumbnailData -> applyThumbnail(thumbnailView, thumbnailData));
    }

    private void applyThumbnail(
            @NonNull ImageView thumbnailView, ThumbnailData thumbnailData) {
        Bitmap bm = thumbnailData == null ? null : thumbnailData.thumbnail;

        thumbnailView.setVisibility(VISIBLE);
        thumbnailView.setImageBitmap(bm);
    }

    private void applyIcon(@Nullable ImageView iconView, @NonNull Task task) {
        if (iconView == null) {
            return;
        }
        iconView.setVisibility(VISIBLE);
        iconView.setImageDrawable(task.icon);
    }

    protected interface ThumbnailUpdateFunction {

        void updateThumbnailInBackground(Task task, Consumer<ThumbnailData> callback);
    }

    protected interface IconUpdateFunction {

        void updateIconInBackground(Task task, Consumer<Task> callback);
    }
}
