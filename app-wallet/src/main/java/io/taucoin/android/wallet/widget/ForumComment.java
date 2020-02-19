/**
 * Copyright 2018 Taucoin Core Developers.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.android.wallet.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.R;

public class ForumComment extends RelativeLayout {
    private ViewHolder viewHolder;
    private double maxValue;
    private double value;
    private boolean isError;

    public ForumComment(Context context) {
        this(context, null);
    }

    public ForumComment(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ForumComment(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        loadView();
    }

    private void loadView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_forum_commnet, this, true);
        viewHolder = new ViewHolder(view);
    }

    class ViewHolder {
        @BindView(R.id.iv_vote_down)
        ImageView ivVoteDown;
        @BindView(R.id.tv_vote)
        TextView tvVote;
        @BindView(R.id.iv_vote_up)
        ImageView ivVoteUp;
        @BindView(R.id.tv_comment)
        TextView tvComment;
        @BindView(R.id.tv_amount)
        TextView tvAmount;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}