package com.cxp.learningvideo

import android.os.Environment

class CommonUtils {

    companion object {
        val src1 = Environment.getExternalStorageDirectory().absolutePath + "/test2.mp4"
        val src2 = Environment.getExternalStorageDirectory().absolutePath + "/test.mp4"
        val destPath = Environment.getExternalStorageDirectory().absolutePath + "/test_dest.mp4"
    }


}