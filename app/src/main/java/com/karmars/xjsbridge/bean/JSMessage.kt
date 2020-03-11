package com.xa.xsas.jsbridge.bean

/**
 *Created by zhouwen on 2020-02-26.
 */
data class JSMessage(
    val callBackId: String,
    val respId: String,
    val methodName: String,
    val data: String
)