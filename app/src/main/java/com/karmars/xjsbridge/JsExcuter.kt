package com.xa.xsas.jsbridge

import android.os.Build
import android.os.Looper
import android.webkit.WebView
import com.alibaba.fastjson.JSON
import com.xa.xsas.jsbridge.bean.JSMessage
import java.util.logging.Logger

/**
 *Created by zhouwen on 2020-03-04.
 */
object JsExcuter {
    private val sLogger = Logger.getLogger("JsExcuter")
    private val JS_FORMAT = "javascript:%s('%s');"

    fun callJSFunction(webView: WebView?, funName: String, args: String) {
        val jsScript = String.format(JS_FORMAT, funName, args)
        sLogger.info("callJSFunction>>>>>")
        callJS(webView, jsScript)
    }


    fun callJSFunction(webView: WebView, funName: String, message: JSMessage): Boolean {
        val messageStr: String = JSON.toJSONString(message)
        val jsScript = String.format(JS_FORMAT, funName, messageStr)

        executeJs(webView, jsScript)
        return true
    }

    fun executeJs(webView: WebView, jsScript: String) {
        if (Thread.currentThread() != Looper.getMainLooper().thread) {
            webView.post {
                executeJs(webView, jsScript)
            }
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(jsScript, null)
        } else {
            webView.loadUrl(jsScript)
        }
    }

    private fun callJS(webView: WebView?, js: String) {
        if (webView == null) {
            return
        }
        sLogger.info("callNative callback callJS ${js}")
        executeJs(webView, js)
    }
}