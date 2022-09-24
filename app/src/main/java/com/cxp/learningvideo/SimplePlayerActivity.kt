package com.cxp.learningvideo

import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.cxp.learningvideo.media.decoder.AudioDecoder
import com.cxp.learningvideo.media.decoder.VideoDecoder
import com.cxp.learningvideo.media.muxer.MP4Repack
import kotlinx.android.synthetic.main.activity_simple_player.*
import java.util.concurrent.Executors


/**
 * 简单播放器页面
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since LearningVideo
 * @version LearningVideo
 * @Datetime 2019-10-12 09:33
 *
 */
class SimplePlayerActivity: AppCompatActivity() {
    lateinit var videoDecoder: VideoDecoder
    lateinit var audioDecoder: AudioDecoder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_player)
        initPlayer()
    }

    private fun initPlayer() {
        val threadPool = Executors.newFixedThreadPool(10)

        videoDecoder = VideoDecoder(CommonUtils.src1, sfv, null)
        threadPool.execute(videoDecoder)

        audioDecoder = AudioDecoder(CommonUtils.src1)
        threadPool.execute(audioDecoder)

        videoDecoder.goOn()
        audioDecoder.goOn()
    }

    fun clickRepack(view: View) {
        repack()
    }

    private fun repack() {
        val repack = MP4Repack(CommonUtils.src1)
        repack.start()
    }

    override fun onDestroy() {
        videoDecoder.stop()
        audioDecoder.stop()
        super.onDestroy()
    }
}
