package com.karmars.xjsbridge.wedget;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.JsPromptResult;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import com.xa.xsas.jsbridge.XJSBridge;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by zhouwen on 2020/02/24
 */
public class XWebView extends WebView {
    private final static Logger sLogger = Logger.getLogger("XWebView");
    protected Listener listener;
    protected XJSBridge jsBridge = new XJSBridge();
    protected long lastErrorTime;
    protected final Map<String, String> httpHeaders = new HashMap<>();

    public XWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public XWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public XWebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public XWebView(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
        super(context, attrs, defStyleAttr, privateBrowsing);
        init();
    }

    public XJSBridge getJsBridge() {
        return jsBridge;
    }

    protected void init() {
        sLogger.info("XWebView#init");
        setFocusable(true);
        setFocusableInTouchMode(true);
        initSettings();
    }

    private BaseWebChromeClient mBaseWebChromeClient;

    @SuppressLint({"SetJavaScriptEnabled"})
    protected void initSettings() {
        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);  //支持js
        // settings.setPluginState();  //支持插件

        settings.setUseWideViewPort(true);  //将图片调整到适合webview的大小
        settings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小
        settings.setSupportZoom(false);  //支持缩放，默认为true。是下面那个的前提。
        settings.setBuiltInZoomControls(true); //设置内置的缩放控件。
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN); //支持内容重新布局
        settings.setAllowFileAccess(true);  //设置可以访问文件
        settings.setNeedInitialFocus(true); //当webview调用requestFocus时为webview设置节点
        settings.setLoadsImagesAutomatically(true);  //支持自动加载图片
        settings.setDefaultTextEncodingName("utf-8");//设置编码格式
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true); //自动开启窗口 js:window.open()

        settings.setDomStorageEnabled(true);

        String ua = settings.getUserAgentString();
        settings.setUserAgentString(ua + "; ANDROID_ENV");

        setMixedContent(settings);
        setCache(settings);
        setCookiesEnabled(true);

        setWebViewClient(new BaseWebViewClient(this));
        mBaseWebChromeClient = new BaseWebChromeClient(this);
        setWebChromeClient(mBaseWebChromeClient);
    }

    private void setMixedContent(WebSettings settings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // 允许http  https 混合图片加载
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    private void setCache(WebSettings settings) {
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);//默认的缓存使用模式。在进行页面前进或后退的操作时，如果缓存可用并未过期就优先加载缓存，否则从网络上加载数据。这样可以减少页面的网络请求次数
        File cacheDir = getContext().getCacheDir();
        settings.setDomStorageEnabled(true);
        if (cacheDir != null) {
            String appCachePath = cacheDir.getAbsolutePath();
            settings.setDatabaseEnabled(true);
            settings.setAppCacheEnabled(true);
            settings.setDatabasePath(appCachePath);
            settings.setAppCachePath(appCachePath);
        }
    }

    @SuppressLint("NewApi")
    public void setCookiesEnabled(boolean enabled) {
        if (Build.VERSION.SDK_INT >= 21) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, enabled);
        }
        CookieManager.getInstance().setAcceptCookie(enabled);
    }


    public void setDesktopMode(final boolean enabled) {
        final WebSettings webSettings = getSettings();

        final String newUserAgent;
        if (enabled) {
            newUserAgent = webSettings.getUserAgentString().replace("Mobile", "eliboM").replace("Android", "diordnA");
        } else {
            newUserAgent = webSettings.getUserAgentString().replace("eliboM", "Mobile").replace("diordnA", "Android");
        }

        webSettings.setUserAgentString(newUserAgent);
        webSettings.setUseWideViewPort(enabled);
        webSettings.setLoadWithOverviewMode(enabled);
        webSettings.setSupportZoom(enabled);
        webSettings.setBuiltInZoomControls(enabled);
    }


    /**
     * 设置监听
     *
     * @param listener
     */
    public void setListener(@NonNull Listener listener) {
        this.listener = listener;
    }

    @Override
    public void destroy() {
        try {
            ((ViewGroup) getParent()).removeView(this);
            removeAllViews();
        } catch (Exception ignored) {
        } finally {
            super.destroy();
        }
    }


    /**
     * 添加header，load url的时候默认携带
     *
     * @param name  header 名
     * @param value header 值
     * @see #loadUrl(String, Map)
     */
    public void addHeader(@NonNull String name, @NonNull String value) {
        httpHeaders.put(name, value);
    }

    /**
     * 移除header中Name对应的一项
     *
     * @param name header 名
     */
    public void removeHeader(@NonNull String name) {
        httpHeaders.remove(name);
    }

    public void clearHeader() {
        httpHeaders.clear();
    }

    /**
     * 点击返回键处理
     *
     * @return false：证明web view中页面回退
     */
    public boolean onBackPressed() {
        if (canGoBack()) {
            goBack();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void loadUrl(@NonNull String url, Map<String, String> additionalHttpHeaders) {
        if (additionalHttpHeaders == null) {
            additionalHttpHeaders = httpHeaders;
        } else if (httpHeaders.size() > 0) {
            additionalHttpHeaders.putAll(httpHeaders);
        }

        super.loadUrl(url, additionalHttpHeaders);
    }

    @Override
    public void loadUrl(@NonNull String url) {
        if (httpHeaders.size() > 0) {
            super.loadUrl(url, httpHeaders);
        } else {
            super.loadUrl(url);
        }
    }

    public void loadUrl(@NonNull String url, boolean preventCaching) {
        if (preventCaching) {
            url = makeUrlUnique(url);
        }
        loadUrl(url);
    }

    public void loadUrl(@NonNull String url, boolean preventCaching, Map<String, String> additionalHttpHeaders) {
        if (preventCaching) {
            url = makeUrlUnique(url);
        }
        loadUrl(url, additionalHttpHeaders);
    }

    protected String makeUrlUnique(@NonNull String url) {
        StringBuilder unique = new StringBuilder();
        unique.append(url);

        if (url.contains("?")) {
            unique.append('&');
        } else {
            if (url.lastIndexOf('/') <= 7) {
                unique.append('/');
            }
            unique.append('?');
        }

        unique.append(System.currentTimeMillis());
        unique.append('=');
        unique.append(1);

        return unique.toString();
    }


    protected void setLastError() {
        lastErrorTime = System.currentTimeMillis();
    }

    protected boolean hasError() {
        return (lastErrorTime + 500) >= System.currentTimeMillis();
    }

    protected Listener getListener() {
        if (listener == null) {
            listener = new EmptyListener();
        }
        return listener;
    }

    public void setOpenFileChooserCallBack(OpenFileChooserCallBack callBack) {
        mBaseWebChromeClient.setOpenFileChooserCallBack(callBack);
    }

    public void setCreateWindowCallBack(CreateWindowCallBack callBack) {
        mBaseWebChromeClient.setCreateWindowCallBack(callBack);
    }


    public static class BaseWebChromeClient extends WebChromeClient {
        private XWebView xWebView;
        private OpenFileChooserCallBack mOpenFileChooserCallBack;
        private CreateWindowCallBack mCreateWindowCallBack;

        public BaseWebChromeClient(@NonNull XWebView xWebView) {
            this.xWebView = xWebView;
        }

        public void setOpenFileChooserCallBack(OpenFileChooserCallBack callBack) {
            mOpenFileChooserCallBack = callBack;
        }

        public void setCreateWindowCallBack(CreateWindowCallBack callBack) {
            mCreateWindowCallBack = callBack;
        }


        // For Android < 3.0
        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            openFileChooser(uploadMsg, "");
        }

        //For Android 3.0 - 4.0
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
            if (mOpenFileChooserCallBack != null) {
                mOpenFileChooserCallBack.openFileChooserCallBack(uploadMsg, acceptType);
            }
        }

        // For Android 4.0 - 5.0
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            openFileChooser(uploadMsg, acceptType);
        }

        // For Android > 5.0
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            sLogger.info("WebchromClient#onShowFileChooser# filePathCallback: ${filePathCallback}");
            if (mOpenFileChooserCallBack != null) {
                mOpenFileChooserCallBack.showFileChooserCallBack(filePathCallback, fileChooserParams);
            }
            return true;
        }


        @Override
        public void onReceivedTitle(WebView view, String title) {
            xWebView.getListener().onReceiveTitle(title);
            super.onReceivedTitle(view, title);
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            xWebView.getListener().onPageProgressed(newProgress);
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            WebView newWebView = new WebView(view.getContext());
            WebViewTransport transport = (WebViewTransport) resultMsg.obj;
            transport.setWebView(newWebView);
            resultMsg.sendToTarget();

            newWebView.setWebViewClient(new WebViewClient() {
                @SuppressWarnings("deprecation")
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                    browserIntent.setData(Uri.parse(url));
                    view.getContext().startActivity(browserIntent);
                    return true;
                }
            });
            if (mCreateWindowCallBack != null) {
                mCreateWindowCallBack.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
            }
            return true;
        }

        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
            Logger.getLogger("XWebView").info(
                    "onJsPrompt#url:" + url + "####message:" + message);
            xWebView.getJsBridge().callNative(view, message);
            result.confirm();
            return true;
        }


        public boolean onConsoleMessage(ConsoleMessage cm) {
            Log.e("BaseWebView", cm.message() + " -- From line "
                    + cm.lineNumber() + " of "
                    + cm.sourceId());
            return true;
        }
    }

    @SuppressWarnings("deprecation")
    public static class BaseWebViewClient extends WebViewClient {
        private XWebView hybridWebView;

        public BaseWebViewClient(@NonNull XWebView hybridWebView) {
            this.hybridWebView = hybridWebView;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (!hybridWebView.hasError()) {
                hybridWebView.getListener().onPageStarted(url, favicon);
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
//            super.onReceivedSslError(view, handler, error);
            handler.proceed();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (!hybridWebView.hasError()) {
                hybridWebView.getListener().onPageFinished(url);
            }

        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            hybridWebView.setLastError();
            hybridWebView.getListener().onPageError(errorCode, description, failingUrl);
        }

        @Override
        public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
            Log.e("BaseWebView", "shouldOverrideUrlLoading:" + url);
            view.loadUrl(url);
            return true;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            Log.e("BaseWebView", "shouldInterceptRequest:" + url);
            return super.shouldInterceptRequest(view, url);
        }
    }

    public interface Listener {

        void onPageStarted(String url, Bitmap favicon);

        void onPageProgressed(int newProgress);

        void onPageFinished(String url);

        void onPageError(int errorCode, String description, String failingUrl);

        void onReceiveTitle(String title);

    }

    private class EmptyListener implements Listener {

        @Override
        public void onPageStarted(String url, Bitmap favicon) {
        }

        @Override
        public void onPageProgressed(int newProgress) {
        }

        @Override
        public void onPageFinished(String url) {
        }

        @Override
        public void onPageError(int errorCode, String description, String failingUrl) {
        }

        @Override
        public void onReceiveTitle(String title) {
        }
    }

    public interface CreateWindowCallBack {
        void onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg);
    }

    public interface OpenFileChooserCallBack {
        void openFileChooserCallBack(ValueCallback<Uri> uploadMsg, String acceptType);

        void showFileChooserCallBack(ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams);
    }
}