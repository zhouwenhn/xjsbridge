package com.xa.xsas.jsbridge

import android.webkit.WebView
import android.widget.Toast
import com.alibaba.fastjson.JSONException
import com.alibaba.fastjson.JSONObject
import java.lang.reflect.Method
import java.util.logging.Logger

/**
 * Created by zhouwen on 2020/02/24
 */
open class XClientJSBridge {
    private val sLogger =
        Logger.getLogger("XClientJSBridge")
    private val KEY_RESULT = "result"
    private val KEY_MSG = "msg"
    private val KEY_DATA = "data"
    private val mCallJsCache = HashMap<String?, Method?>()
    private val mXjsBClzCache = HashMap<String, Class<*>>()
    fun getJSONString(
        jsonObj: JSONObject?,
        key: String?,
        defalutValue: String
    ): String {
        if (jsonObj == null) return defalutValue
        try {
            if (jsonObj.containsKey(key)) {
                return jsonObj.getString(key)
            }
        } catch (e: JSONException) {
            sLogger.info("JSONException#$e")
        }
        return defalutValue
    }

    fun genCallbackJson(
        result: Boolean,
        message: String?,
        data: Any?
    ): JSONObject {
        val resultJson = JSONObject()
        try {
            resultJson.put(KEY_DATA, data)
            resultJson.put(KEY_RESULT, result)
            resultJson.put(KEY_MSG, message)
        } catch (e: JSONException) {
            sLogger.severe(e.toString())
        }
        return resultJson
    }

    /**
     * 账号信息
     */
    fun getAccount(webView: WebView?, jsonObj: JSONObject?) {
        sLogger.info("XClientJSBridge#getAccount: ${jsonObj}")
        JsExcuter.callJSFunction(webView, "__js_getAccount", "accountInfo")
    }

    /**
     * 退登
     */
    fun logout(webView: WebView?, jsonObj: JSONObject?) {
        sLogger.info("XClientJSBridge#logout: ${jsonObj}")
    }

    /**
     * H5的登录请求发起
     */
    fun login(webView: WebView?, jsonObj: JSONObject?) {
        sLogger.info("XClientJSBridge#login: ${jsonObj}")
    }
}