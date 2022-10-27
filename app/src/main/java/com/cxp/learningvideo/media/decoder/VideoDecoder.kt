package com.cxp.learningvideo.media.decoder

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.cxp.learningvideo.media.BaseDecoder
import com.cxp.learningvideo.media.IExtractor
import com.cxp.learningvideo.media.extractor.VideoExtractor
import java.nio.ByteBuffer


/**
 * video decoder
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since LearningVideo
 * @version LearningVideo
 * @Datetime 2019-09-03 10:52
 *
 */
class VideoDecoder(path: String, sfv: SurfaceView?, surface: Surface?): BaseDecoder(path) {
    private val TAG = "VideoDecoder"
    
    private val mSurfaceView = sfv
    private var mSurface = surface
    
    override fun check(): Boolean {
        if (mSurfaceView == null && mSurface == null) {
            Log.w(TAG, "Both SurfaceView and Surface are empty, at least one needs to be not empty")
            mStateListener?.decoderError(this, "Display is empty")
            return false
        }
        return true
    }

    override fun initExtractor(path: String): IExtractor {
        return VideoExtractor(path)
    }

    override fun initSpecParams(format: MediaFormat) {
    }

    override fun configCodec(mediaCodec: MediaCodec, format: MediaFormat): Boolean {
        if (mSurface != null) {
            mediaCodec.configure(format, mSurface , null, 0)
            notifyDecode()
        } else if (mSurfaceView?.holder?.surface != null) {
            mSurface = mSurfaceView?.holder?.surface
            configCodec(mediaCodec, format)
        } else {
            mSurfaceView?.holder?.addCallback(object : SurfaceHolder.Callback2 {
                override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                }

                override fun surfaceCreated(holder: SurfaceHolder) {
                    mSurface = holder.surface
                    configCodec(mediaCodec, format)
                }
            })

            return false
        }
        return true
    }

    override fun initRender(): Boolean {
        return true
    }

    override fun render(outputBuffer: ByteBuffer,
                        bufferInfo: MediaCodec.BufferInfo) {
    }

    override fun doneDecode() {
    }
}