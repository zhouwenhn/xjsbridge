package com.xa.xsas.jsbridge

import androidx.core.content.FileProvider

/**
 * Android 7.0 禁止在应用外部公开 file:// URI，所以我们必须使用 content:// 替代
 */
class UploadFileProvider : FileProvider()