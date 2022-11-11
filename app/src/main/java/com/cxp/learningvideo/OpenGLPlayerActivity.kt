package com.cxp.learningvideo

import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.view.Surface
import com.cxp.learningvideo.media.decoder.AudioDecoder
import com.cxp.learningvideo.media.decoder.VideoDecoder
import com.cxp.learningvideo.opengl.SimpleRender
import com.cxp.learningvideo.opengl.drawer.IDrawer
import com.cxp.learningvideo.opengl.drawer.VideoDrawer
import kotlinx.android.synthetic.main.activity_opengl_player.*
import java.util.concurrent.Executors


/**
 * 使用OpenGL渲染的播放器
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since LearningVideo
 * @version LearningVideo
 * @Datetime 2019-10-26 21:07
 *
 */
class OpenGLPlayerActivity: AppCompatActivity() {
    lateinit var drawer: IDrawer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opengl_player)
        initRender()
    }

    private fun initRender() {
        drawer = VideoDrawer()
        drawer.setVideoSize(1920, 1080)
        drawer.getSurfaceTexture {
            initPlayer(Surface(it))
        }
        drawer.getSurfaceTexture {  }
        gl_surface.setEGLContextClientVersion(2)
        val render = SimpleRender()
        render.addDrawer(drawer)
        gl_surface.setRenderer(render)
    }

    private fun initPlayer(sf: Surface) {
        val threadPool = Executors.newFixedThreadPool(10)

        val videoDecoder = VideoDecoder(CommonUtils.src1, null, sf)
        threadPool.execute(videoDecoder)

        val audioDecoder = AudioDecoder(CommonUtils.src1)
        threadPool.execute(audioDecoder)

        videoDecoder.goOn()
        audioDecoder.goOn()
    }
}