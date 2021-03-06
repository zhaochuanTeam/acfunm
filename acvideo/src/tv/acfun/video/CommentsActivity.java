/*
 * Copyright (C) 2014 YROM.NET
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

package tv.acfun.video;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpException;

import tv.ac.fun.R;
import tv.acfun.util.ArrayUtil;
import tv.acfun.util.net.Connectivity;
import tv.acfun.video.adapter.CommentsAdapter;
import tv.acfun.video.api.API;
import tv.acfun.video.entity.Comment;
import tv.acfun.video.entity.Comments;
import tv.acfun.video.entity.User;
import tv.acfun.video.util.MemberUtils;
import tv.acfun.video.util.TextViewUtils;
import tv.acfun.video.util.net.CommentsRequest;
import tv.acfun.video.widget.EmotionView;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.android.volley.Request;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.umeng.analytics.MobclickAgent;

/**
 * @author Yrom
 *
 */
public class CommentsActivity extends ActionBarActivity implements OnClickListener, OnItemClickListener, OnItemLongClickListener, ErrorListener, OnScrollListener {
    public static void start(Context context, int aid) {
        Intent intent = new Intent(context, CommentsActivity.class);
        intent.putExtra("aid", aid);
        context.startActivity(intent);
    }
    private static final String TAG = "Comments";
    private int aid;
    private InputMethodManager mKeyboard;
    private ListView mList;
    private ProgressBar mLoadingBar;
    private TextView mTimeOutText;
    private View mFootview;
    private CommentsAdapter mAdapter;
    private int pageIndex = 1;
    private boolean isInputShow;
    private View mCommentBar;
    private ImageButton mBtnSend;
    private EditText mCommentText;
    private View mBtnEmotion;
    private GridView mEmotionGrid;
    private boolean isBarShowing = true;
    protected boolean isloading;
    SparseArray<Comment> data = new SparseArray<Comment>();
    List<Integer> commentIdList = new ArrayList<Integer>();
    protected int totalPage;
    protected boolean hasNextPage;
    protected boolean isreload;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        aid = getIntent().getIntExtra("aid", 0);
        if (aid == 0)
            return;
        setContentView(R.layout.activity_comments);
        MobclickAgent.onEvent(this, "view_comment", "ac" + aid);
        ActionBar ab = getSupportActionBar();

        mKeyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        ab.setTitle("ac" + aid + " / 评论");
        initCommentsBar();
        initList();
        requestData(1,true);
        handleKeyboardStatus();
    }
    private void requestData(int page, boolean requestNewData) {
        isloading = true;
        Request<?> request = new CommentsRequest(getApplicationContext(), aid, page, mCommentListener,this);
        request.setTag(TAG);
        request.setShouldCache(true);
        if (requestNewData) {
            mTimeOutText.setVisibility(View.GONE);
            if(mAdapter == null || mAdapter.isEmpty()) 
                mLoadingBar.setVisibility(View.VISIBLE);
            Connectivity.getGloableQueue(this).getCache().invalidate(request.getCacheKey(), true);
        }
        AcApp.addRequest(request);
    }
    Listener<Comments> mCommentListener = new Listener<Comments>() {
        @Override
        public void onResponse(Comments response) {
            isloading = false;
            if (response.totalCount == 0) {
                mLoadingBar.setVisibility(View.GONE);
                mTimeOutText.setVisibility(View.VISIBLE);
                mList.setVisibility(View.GONE);
                mTimeOutText.setText(R.string.no_comment_yet);
                return;
            }

            if (response.page == 1) {
                if (mAdapter != null)
                    mAdapter.notifyDataSetInvalidated();
                data.clear();
                commentIdList.clear();
                mLoadingBar.setVisibility(View.GONE);
                mList.setVisibility(View.VISIBLE);
            }
            ArrayUtil.putAll(response.commentArr, data);
            commentIdList.addAll(ArrayUtil.asList(response.commentList));
            totalPage = response.totalPage;
            hasNextPage = response.nextPage > response.page;
            if (data != null && data.size() > 0) {
                mAdapter.setData(data, commentIdList);
                mAdapter.notifyDataSetChanged();
                isreload = false;
            }
        }
    };
    private void initList() {
        mList = (ListView) findViewById(R.id.list);
        mLoadingBar = (ProgressBar) findViewById(R.id.progressBar);
        mTimeOutText = (TextView) findViewById(R.id.time_out_text);
        mTimeOutText.setOnClickListener(this);
        mList.setHeaderDividersEnabled(false);
        mFootview = LayoutInflater.from(this).inflate(R.layout.list_footerview, mList, false);
        mList.setVisibility(View.INVISIBLE);
        mList.addFooterView(mFootview);
        mList.setFooterDividersEnabled(false);
        mList.setOnItemClickListener(this);
        mList.setOnItemLongClickListener(this);
        mList.setOnScrollListener(this);
//        mList.setOnTouchListener(new OnTouchListener() {
//            private int mMotionY;
//
//            public boolean onTouch(View v, MotionEvent event) {
//                int y = (int) event.getY();
//                switch (event.getAction()) {
//                case MotionEvent.ACTION_DOWN:
//                    mMotionY = y;
//                    break;
//                case MotionEvent.ACTION_MOVE:
//                    int delta = y - mMotionY;
//                    if (Math.abs(delta) < 100)
//                        break;
//                    if (delta > 0) {
//                        showBar();
//                    } else {
//                        hideBar();
//                    }
//                    mMotionY = y;
//                    break;
//                }
//                return false;
//            }
//        });
        mAdapter = new CommentsAdapter(this, data, commentIdList);
        mList.setAdapter(mAdapter);
    }
    
    private void initCommentsBar() {
        mCommentBar = findViewById(R.id.comments_bar);
        mBtnSend = (ImageButton) findViewById(R.id.comments_send_btn);
        mCommentText = (EditText) findViewById(R.id.comments_edit);
        mBtnEmotion = findViewById(R.id.comments_emotion_btn);
        mEmotionGrid = (GridView) findViewById(R.id.emotions);
        mBtnSend.setOnClickListener(this);
        mBtnEmotion.setOnClickListener(this);
        mEmotionGrid.setAdapter(mEmotionAdapter);
        mEmotionGrid.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int index = mCommentText.getSelectionEnd();
                Editable text = mCommentText.getText();
                String emotion = parent.getItemAtPosition(position).toString();
                text.insert(index, emotion);
                EmotionView v = (EmotionView) parent.getAdapter().getView(position, null, null);
                Drawable drawable = TextViewUtils.convertViewToDrawable(v);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth() / 2,
                        drawable.getIntrinsicHeight() / 2);
                text.setSpan(new ImageSpan(drawable), index, index + emotion.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

        });
    }
    ListAdapter mEmotionAdapter = new BaseAdapter() {

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = new EmotionView(getApplicationContext());
            }
            ((EmotionView) convertView).setEmotionId(position + 1);
            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public String getItem(int position) {
            String cat = position >= 54 ? "ais" : "ac";
            int id = position >= 54 ? position - 53 : position + 1;
            return String.format("[emot=%s,%02d/]", cat, id);
        }

        @Override
        public int getCount() {
            return 94;
        }
    };
    private boolean mIsItemVisible;
    private User mUser;
    private Quote mQuoteSpan;
    private ImageSpan mQuoteImage;
    private void handleKeyboardStatus() {
        final View activityRootView = findViewById(R.id.content_frame);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {

                    @Override
                    public void onGlobalLayout() {
                        Rect r = new Rect();
                        activityRootView.getWindowVisibleDisplayFrame(r);

                        int heightDiff = activityRootView.getRootView().getHeight()
                                - (r.bottom - r.top);
                        isInputShow = heightDiff > 100; 
                    }
                });
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.time_out_text:
            pageIndex = 1;
            requestData(pageIndex, true);
            break;
        case R.id.comments_send_btn:
            mKeyboard.hideSoftInputFromWindow(mEmotionGrid.getWindowToken(), 0);
            postComment();
            break;
        case R.id.comments_emotion_btn:
            if (isInputShow) {
                mKeyboard.hideSoftInputFromWindow(mEmotionGrid.getWindowToken(), 0);
                if (mEmotionGrid.getVisibility() != View.VISIBLE)
                    mEmotionGrid.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            mEmotionGrid.setVisibility(View.VISIBLE);

                        }
                    }, 20);
            } else {
                mEmotionGrid.setVisibility(mEmotionGrid.getVisibility() == View.VISIBLE ? View.GONE
                        : View.VISIBLE);
            }
            break;
        }
    }
    private void postComment() {
        if (!validate()) {
            return;
        }
        mEmotionGrid.setVisibility(View.GONE);
        MobclickAgent.onEvent(this, "post_comment");
        int count = getQuoteCount();
        String comment = getComment();
        Comment quote = data == null ? null : data.get(findCid(count));
        new CommentPostTask(comment, quote).execute();
    }
    
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int count = mAdapter.getCount();
        if(position>count){
            if (isreload) {
                mFootview.findViewById(R.id.list_footview_progress).setVisibility(View.VISIBLE);
                TextView textview = (TextView) mFootview.findViewById(R.id.list_footview_text);
                textview.setText(R.string.buffering);
                requestData(pageIndex, false);
            }
            return;
        }
//        showBar(); //TODO: show input bar when selected comment
        Object o = parent.getItemAtPosition(position);
        if(o == null || !(o instanceof Comment)) return;
        Comment c = (Comment)o;
        int quoteCount = getQuoteCount();
        removeQuote(mCommentText.getText());
        if (quoteCount == c.count)
            return; // 取消引用
        String pre = "引用:#" + c.count;
        mQuoteSpan = new Quote(c.count);
        SpannableStringBuilder sb = SpannableStringBuilder.valueOf(mCommentText.getText());
        TextView tv = TextViewUtils.createBubbleTextView(this, pre);
        BitmapDrawable bd = (BitmapDrawable) TextViewUtils.convertViewToDrawable(tv);
        bd.setBounds(0, 0, bd.getIntrinsicWidth(), bd.getIntrinsicHeight());
        sb.insert(0, pre);
        mQuoteImage = new ImageSpan(bd);
        sb.setSpan(mQuoteImage, 0, pre.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(mQuoteSpan, 0, pre.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append("");
        mCommentText.setText(sb);
        mCommentText.setSelection(mCommentText.getText().length());
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        AcApp.cancelAllRequest(TAG);
        if (mAdapter != null) {
            mAdapter.setData(null, null);
            mAdapter = null;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Object o = parent.getItemAtPosition(position);
        if(o == null || !(o instanceof Comment)) return false;
        Comment c = (Comment)o;
        ClipboardManager ma = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            ClipData text = ClipData.newHtmlText(c.userName, c.content, c.content);
            ((android.content.ClipboardManager) ma).setPrimaryClip(text);
        }else{
            ma.setText(c.content);
        }
        Toast.makeText(this, "#"+c.count+"的内容已复制", 0).show();
        return true;
    }
    @Override
    public void onErrorResponse(VolleyError error) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == SCROLL_STATE_IDLE && mIsItemVisible) {
            if(hasNextPage){
                if(!isloading){
                    requestData(++pageIndex,false);
                }
            } else{
                mFootview.findViewById(R.id.list_footview_progress).setVisibility(View.GONE);
                ((TextView)mFootview.findViewById(R.id.list_footview_text)).setText("没有了");
            }
        }
    }
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        mIsItemVisible = totalItemCount > 0 && (firstVisibleItem + visibleItemCount >= totalItemCount - 1);
    }
    boolean validate() {
        mUser = AcApp.getUser();
        if (mUser == null) {
            Toast.makeText(this, getString(R.string.sign_in_first), Toast.LENGTH_SHORT).show();
            startActivity(SigninActivity.createIntent(this));
            return false;
        }
        Editable text = mCommentText.getText();
        int len = text.length() - getQuoteSpanLength(text);
        if (len == 0) {
            Toast.makeText(this, getString(R.string.no_comment), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (len <= 5) {
            Toast.makeText(this, getString(R.string.comment_not_enough), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    int getQuoteSpanLength(Editable text) {
        Quote quote = TextViewUtils.getLast(text, Quote.class);
        int start = text.getSpanStart(quote);
        int end = text.getSpanEnd(quote);
        if (start >= 0) {
            return end - start;
        }
        return 0;

    }

    void removeQuote(Editable text) {
        Quote quote = TextViewUtils.getLast(text, Quote.class);
        int start = text.getSpanStart(quote);
        int end = text.getSpanEnd(quote);
        // Log.d(TAG, String.format("start=%d, end=%d", start, end));
        if (start >= 0) {
            // Log.d(TAG, text.subSequence(start, end).toString());
            text.delete(start, end);
        }
    }

    String getComment() {
        Editable text = SpannableStringBuilder.valueOf(mCommentText.getText());
        Quote quote = TextViewUtils.getLast(text, Quote.class);
        int start = text.getSpanStart(quote);
        int end = text.getSpanEnd(quote);
        if (start < 0)
            return text.toString();
        else if (start == 0) {
            return text.subSequence(end, text.length()).toString();
        } else
            return text.subSequence(0, start).toString()
                    + text.subSequence(end, text.length()).toString();
    }

    /**
     * call before {@code removeQuote()}
     * 
     * @return -1,if not found
     */
    int getQuoteCount() {
        Editable text = mCommentText.getText();
        Quote quote = TextViewUtils.getLast(text, Quote.class);
        int start = text.getSpanStart(quote);
        if (start >= 0) {
            return quote.floosCount;
        }
        return -1;

    }

    class Quote {

        int floosCount;

        public Quote(int count) {
            this.floosCount = count;
        }
    }
    class CommentPostTask extends AsyncTask<Void, Void, Boolean> {

        protected void onPreExecute() {
            mBtnSend.setEnabled(false);
            dialog = ProgressDialog.show(CommentsActivity.this, null, getString(R.string.posting_comment), true, false);
        }

        String comment;
        Comment quote;
        ProgressDialog dialog;

        public CommentPostTask(String comment, Comment quote) {
            this.comment = comment;
            this.quote = quote;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Cookie[] cookies = JSON.parseObject(mUser.cookies, Cookie[].class);
            for (int i = 0; i < 3; i++)
                try {
                    if (MemberUtils.postComments(comment, quote, aid, API.getDomainRoot(getApplicationContext()), cookies))
                        return true;
                } catch (HttpException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            dialog.dismiss();
            mBtnSend.setEnabled(true);
            mCommentText.setText("");
            if (result) {
                Toast.makeText(getApplicationContext(), getString(R.string.comment_success), Toast.LENGTH_SHORT).show();
                pageIndex = 1;
                requestData(pageIndex, true);
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.comment_failed), Toast.LENGTH_SHORT).show();
            }
        }
    }

    int findCid(int floorCount) {
        for (int i = 0; i < commentIdList.size(); i++) {
            int key = commentIdList.get(i);
            Comment c = data.get(key);
            if (c.count == floorCount)
                return c.cid;
        }
        return 0;
    }
}
