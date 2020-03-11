package com.xa.xsas.jsbridge

import android.net.Uri
import android.text.TextUtils
import android.webkit.WebView
import com.alibaba.fastjson.JSONObject
import com.xa.xsas.jsbridge.conts.JSConst
import com.xa.xsas.jsbridge.bean.JSParames
import java.lang.reflect.Method
import java.util.*
import java.util.logging.Logger

/**
 *Created by zhouwen on 2020-02-25.
 */
open class XJSBridge {

    private val sLogger = Logger.getLogger("XJSBridge")

    private val JSBRIDGE = "XJSBridge#"
    private val mBridgedClassNameMap =
        HashMap<String, String>(2)
    private val mBridgedClassCache =
        HashMap<String?, Class<*>>()
    private val mBridgedMethodCache =
        HashMap<String?, Method?>()
    private val JS_PREFIX = "javascript:"
    private val JS_FORMAT = "javascript:%s('%s');"

    init {
        mBridgedClassNameMap.put(
            "XJSClient",
            "com.xa.xsas.jsbridge.XClientJSBridge"
        )
    }

    fun callNative(webView: WebView?, jsonStr: String): String? {
        if ("" != jsonStr) {
            //parse uri
            val uri: Uri = Uri.parse(jsonStr)
            if (uri == null) {
                return ""
            }

            sLogger.info("callNative#host:${uri.host} >> scheme:${uri.getScheme()}")
            if (!JSConst.PROTOCOL_SCHEME_NAME.equals(uri.getScheme())
                || !JSConst.PARAMETER_HOST.equals(uri.host)) {
                return ""
            }
            val argsJson: String = uri.getQueryParameter(JSConst.PARAMETER_NAME)
            sLogger.info("callNative# argsJson: ${argsJson} ")
            try {
                val jsonObject = JSONObject.parseObject(argsJson)
                val clz:String = jsonObject.getString(JSParames.CLZ)
                val method = jsonObject.getString(JSParames.METHOD)
                val args = jsonObject.getJSONObject(JSParames.ARGS)
                val jsParames = JSParames(clz, method, args)
                sLogger.info("jsParames>>>> className: ${jsParames.className}" +
                        "className: ${jsParames.method} >>args:${jsParames.args}")
                val result = callNative(webView, jsParames)
            } catch (e: java.lang.Exception){
                sLogger.severe("Exception>>>${e.message}")
            }
        }
        return ""
    }

    fun callNative(webView: WebView?, jsp: JSParames): String? {
        try {
            val className = jsp.className
            var clz: Class<*>? =
                mBridgedClassCache.get(className)
            if (clz == null) {
                clz = Class.forName(
                    mBridgedClassNameMap.get(className)
                )
                mBridgedClassCache.put(className, clz)
            }

            val methodName = jsp.method
            var method: Method? =
                mBridgedMethodCache.get(methodName)
            sLogger.info(
                "${JSBRIDGE} callNative: ${className}.${methodName}, method: ${method}, Args: ${jsp.args}"

            )
            if (method == null) {
                method = if (jsp.args != null) {
                    clz?.getMethod(methodName, WebView::class.java, JSONObject::class.java)
                } else {
                    clz?.getMethod(methodName, WebView::class.java)
                }
                sLogger.severe("XClientJSBridge>>>>>>>>>>method: ${method}")
                mBridgedMethodCache.put(
                    methodName,
                    method
                )
            }

            sLogger.severe("XClientJSBridge>>>>>>>>>>method: ${method}")
            if (method?.parameterTypes?.size == 2) {
                method?.invoke(clz?.newInstance(), webView, jsp.args)
            } else {
                method?.invoke(clz?.newInstance(), webView) as String
            }
            return "callNative Suc"
        } catch (e: Exception) {
            sLogger.severe(
                "${JSBRIDGE} JS call ${jsp.className}::${jsp.method}, error:${e} " +
                        "+>cause> ${e.cause}"
            )
            if (jsp.args != null) {
                val callbackId: String = XClientJSBridge().getJSONString(jsp.args, "callbackId", "")
                if (!TextUtils.isEmpty(callbackId)) {
                    val resultJson: JSONObject =
                        XClientJSBridge().genCallbackJson(false, "NotFoundException", "{}")
                    sLogger.info(
                        "${JSBRIDGE} callbackJS callbackId ${callbackId} ${resultJson} "

                    )
                    JsExcuter.callJSFunction(webView, callbackId, resultJson.toString())
                }
                return callbackId
            }
        }
        return ""
    }


//    lateinit var threadSafeStrBuilder: ThreadLocal<StringBuilder>
//    private fun getThreadSafeStringBuilder(): StringBuilder? {
//        if (threadSafeStrBuilder == null) {
//            threadSafeStrBuilder =
//                ThreadLocal<StringBuilder>()
//        }
//        var sb: StringBuilder = threadSafeStrBuilder.get()
//        if (sb == null) {
//            sb = StringBuilder()
//            threadSafeStrBuilder.set(sb)
//        }
//        sb.delete(0, sb.length)
//        return sb
//    }

    private fun callJS(webView: WebView?, js: String) {
        if (webView == null) {
            return
        }
        val url: String = JS_PREFIX + js
        sLogger.info("${JSBRIDGE} callNative callback callJS ${url}")
        JsExcuter.executeJs(webView, url)
    }
//    fun callbackJS(
//        webview: XWebView?,
//        callbackId: String?,
//        jsonObject: JSONObject
//    ) {
//        val sb: StringBuilder = getThreadSafeStringBuilder()!!.append(
//            JSConst.isCallbackExsistString
//        )
//        sb.append(callbackId).append(',').append(jsonObject.toString()).append(')')
//        callJS(webview, sb.toString())
//    }

//    fun callbackJS(
//        webview: XWebView?,
//        callbackId: String?,
//        str: String?
//    ) {
//        val sb: StringBuilder = getThreadSafeStringBuilder()!!.append(
//            JSConst.isCallbackExsistString
//        )
//        sb.append(callbackId).append(',').append(str).append(')')
//        callJS(webview, sb.toString())
//    }
}