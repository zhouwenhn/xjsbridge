package com.xa.xsas.jsbridge.bean

import com.alibaba.fastjson.JSONObject


/**
 *Created by zhouwen on 2020-02-27.
 *
 * 格式:
 * className：XAClient
 * method：functionName
 * args:{[JSONObject]}
 *
 * 协议:
 * xjsbridge://jsapp?args="${JSON.stringify(args)}
 */
data class JSParames(var className: String, var method: String, var args: JSONObject){
    companion object {
        const val CLZ = "className"// 类名
        const val METHOD = "method" // 方法
        const val ARGS = "args" // 业务参数
    }
}