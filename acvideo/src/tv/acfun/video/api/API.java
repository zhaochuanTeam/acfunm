/*
 * Copyright (C) 2013 YROM.NET
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

package tv.acfun.video.api;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.json.JSONObject;

import tv.ac.fun.BuildConfig;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.umeng.analytics.MobclickAgent;
import com.umeng.analytics.onlineconfig.UmengOnlineConfigureListener;


/**
 * 
 * @see <a href="http://wiki.acfun.tv/index.php/APIDoc4APP">APIDoc4APP</a>
 * @author Yrom
 * 
 */
public class API {
    private static final String DOMAIN_ROOT = "domain_root";
    private static final String DOMAIN_API = "domain_api";
    private static final String DOMAIN_COMMENT = "domain_comment";
    
    public static final String BASE_URL = "http://api.acfun.tv";
    public static final String HOME_CATS = BASE_URL + "/home/categories";
    public static final String VIDEO_DETAIL = BASE_URL + "/videos/%d";
    public static final String CHANNEL_CATS = BASE_URL + "/videocategories";
    public static final String VIDEO_LIST = BASE_URL + "/videos?class=%d&cursor=%d";
    public static final String EXTRAS_CHANNEL_ID = "extras_channel_id";
    public static final String EXTRAS_CHANNEL_NAME = "extras_channel_name";
    public static final String EXTRAS_CATEGORY_ID = "extras_category_id";
    public static final String EXTRAS_CATEGORY_IDS = "extras_category_ids";
    @Deprecated
    public static final String COMMENTS = "http://www.acfun.com/comment_list_json.aspx?contentId=%d&currentPage=%d";
    @Deprecated
    public static final String URL_SEARCH = "http://www.acfun.com/api/search.aspx?query=%s&exact=1&channelIds=1,58,59,60,70,69,68&orderId=%d&orderBy=%d&pageNo=%d&pageSize=%d";
    
    public static String HOME = "www.acfun.tv";
    public static String API_HOME = "api.acfun.tv";
    public static String COMMENT = "static.comment.acfun.tv";
    public static void updateConfig(Context context){
        init(context);
        MobclickAgent.updateOnlineConfig(context);
        MobclickAgent.setOnlineConfigureListener(new UmengOnlineConfigureListener() {
            @Override
            public void onDataReceived(JSONObject json) {
                if(BuildConfig.DEBUG){
                    Log.v("api", "received online config:"+json);
                }
                if(json == null) return;
                String root = json.optString(DOMAIN_ROOT, HOME);
                String api = json.optString(DOMAIN_API, API_HOME);
                String comment = json.optString(DOMAIN_COMMENT, COMMENT);
                HOME = root;
                API_HOME = api;
                COMMENT = comment;
            }
        });
    }
    private static void init(Context context) {
        String root = MobclickAgent.getConfigParams(context, DOMAIN_ROOT);
        String api  = MobclickAgent.getConfigParams(context, DOMAIN_API);
        String comment = MobclickAgent.getConfigParams(context, DOMAIN_COMMENT);
        if(!TextUtils.isEmpty(root)) HOME = root;
        if(!TextUtils.isEmpty(api)) API_HOME = api;
        if(!TextUtils.isEmpty(comment)) COMMENT = comment;
    }
    public static String getDomainRoot(Context context) {
        if(context == null) return HOME;
        String params = MobclickAgent.getConfigParams(context, DOMAIN_ROOT);
        return TextUtils.isEmpty(params) ? HOME : params;
    }
    
    public static String getDomainApi(Context context) {
        if(context == null) return API_HOME;
        String params = MobclickAgent.getConfigParams(context, DOMAIN_API);
        return TextUtils.isEmpty(params) ? API_HOME : params;
    }
    
    public static String getDomainComment(Context context){
        if(context == null) return COMMENT;
        String params = MobclickAgent.getConfigParams(context, DOMAIN_COMMENT);
        return TextUtils.isEmpty(params) ? COMMENT : params;
    }
    public static String getVideosUrl(int catId, int page, boolean isoriginal) {
        String url = String.format(API.VIDEO_LIST, catId, page*20);
        if (isoriginal) url = url + "&isoriginal=true";
        return url;
    }
    
    public static String getVideoDetailsUrl(int acId){
        return String.format(VIDEO_DETAIL,acId);
    }

    public static String getCommentUrl(Context context, int aid, int page) {
        
        return String.format("http://%s/comment_list_json.aspx?contentId=%d&currentPage=%d", getDomainRoot(context), aid, page);
    }

    /**
     * @param query key word
     * @param orderId 相关、日期、点击、评论、收藏，0~4
     * @param orderBy 按标题标签、用户、内容简介查找，1~3
     * @param pageNo 
     * @param pageSize http://www.acfun.com/api/search.aspx?query={query}&exact=1&channelIds=63&orderId=2&orderBy=1&pageNo=1&pageSize=10&_=1387786184949
     * @return
     */
    public static String getSearchUrl(Context context, String query, int orderId, int orderBy, int pageNo, int pageSize){
        String url = null;
        try {
            String key = URLEncoder.encode(query, "UTF-8");
            url = String.format("http://%s/api/search.aspx?query=%s&exact=1&channelIds=1,58,59,60,70,69,68&orderId=%d&orderBy=%d&pageNo=%d&pageSize=%d",
                            getDomainRoot(context), key, orderId, orderBy, pageNo, pageSize);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return url;
    }
    
}
