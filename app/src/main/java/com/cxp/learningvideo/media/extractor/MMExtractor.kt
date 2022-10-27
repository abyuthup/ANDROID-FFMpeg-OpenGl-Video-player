package com.chenlittleping.videoeditor.decoder

import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer


/**
 * Audio and video splitter
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since VideoEditor
 * @version VideoEditor
 * @Datetime 2019-09-03
 *
 */

class MMExtractor(path: String?) {

    /**Audio and video splitter*/
    private var mExtractor: MediaExtractor? = null

    /**Audio channel index*/
    private var mAudioTrack = -1

    /**Video channel index*/
    private var mVideoTrack = -1

    /**Current frame timestamp*/
    private var mCurSampleTime: Long = 0

    /**Current frame flag*/
    private var mCurSampleFlag: Int = 0

    /**Start decoding time point*/
    private var mStartPos: Long = 0

    init {
        mExtractor = MediaExtractor()
        mExtractor?.setDataSource(path)
    }

    /**
     * Get video format parameters
     */
    fun getVideoFormat(): MediaFormat? {
        for (i in 0 until mExtractor!!.trackCount) {
            val mediaFormat = mExtractor!!.getTrackFormat(i)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            if (mime.startsWith("video/")) {
                mVideoTrack = i
                break
            }
        }
        return if (mVideoTrack >= 0)
            mExtractor!!.getTrackFormat(mVideoTrack)
        else null
    }

    /**
     * Get audio format parameters
     */
    fun getAudioFormat(): MediaFormat? {
        for (i in 0 until mExtractor!!.trackCount) {
            val mediaFormat = mExtractor!!.getTrackFormat(i)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            if (mime.startsWith("audio/")) {
                mAudioTrack = i
                break
            }
        }
        return if (mAudioTrack >= 0) {
            mExtractor!!.getTrackFormat(mAudioTrack)
        } else null
    }

    /**
     * read video data
     */
    fun readBuffer(byteBuffer: ByteBuffer): Int {
        byteBuffer.clear()
        selectSourceTrack()
        //readSampleData retrieves the current encoded sample and store it in the byte buffer starting at the given offset.
        var readSampleCount = mExtractor!!.readSampleData(byteBuffer, 0)
        if (readSampleCount < 0) {
            return -1
        }
        //Record the timestamp of the current frame
        mCurSampleTime = mExtractor!!.sampleTime
        mCurSampleFlag = mExtractor!!.sampleFlags
        //go to next frame
        mExtractor!!.advance()
        return readSampleCount
    }

    /**
     * 选择通道
     */
    private fun selectSourceTrack() {
        if (mVideoTrack >= 0) {
            mExtractor!!.selectTrack(mVideoTrack)
        } else if (mAudioTrack >= 0) {
            mExtractor!!.selectTrack(mAudioTrack)
        }
    }

    /**
     * Seek到指定位置，并返回实际帧的时间戳
     */
    fun seek(pos: Long): Long {
        mExtractor!!.seekTo(pos, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        return mExtractor!!.sampleTime
    }

    /**
     * 停止读取数据
     */
    fun stop() {
        mExtractor?.release()
        mExtractor = null
    }

    fun getVideoTrack(): Int {
        return mVideoTrack
    }

    fun getAudioTrack(): Int {
        return mAudioTrack
    }

    fun setStartPos(pos: Long) {
        mStartPos = pos
    }

    /**
     * 获取当前帧时间
     */
    fun getCurrentTimestamp(): Long {
        return mCurSampleTime
    }

    fun getSampleFlag(): Int {
        return mCurSampleFlag
    }
}