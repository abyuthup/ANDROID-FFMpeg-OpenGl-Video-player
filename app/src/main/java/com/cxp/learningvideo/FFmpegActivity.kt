package com.cxp.learningvideo

import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.view.Surface
import android.view.SurfaceHolder
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_ffmpeg_info.*
import java.io.File


/**
 * FFmpeg测试页面
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since LearningVideo
 * @version LearningVideo
 *
 */
class FFmpegActivity: AppCompatActivity() {


    private var player: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ffmpeg_info)
        tv.text = ffmpegInfo()
        initSfv()
    }

    private fun initSfv() {
        if (File(CommonUtils.src1).exists()) {
            sfv.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceChanged(holder: SurfaceHolder, format: Int,
                                            width: Int, height: Int) {}
                override fun surfaceDestroyed(holder: SurfaceHolder) {}

                override fun surfaceCreated(holder: SurfaceHolder) {
                    if (player == null) {
                        player = createPlayer(CommonUtils.src1, holder.surface)
                        play(player!!)
                    }
                }
            })
        } else {
            Toast.makeText(this, "The video file does not exist, please put it in the root directory of the phone test.mp4", Toast.LENGTH_SHORT).show()
        }
    }

    private external fun ffmpegInfo(): String

    private external fun createPlayer(path: String, surface: Surface): Int

    private external fun play(player: Int)

    private external fun pause(player: Int)

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}