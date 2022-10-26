package com.cxp.learningvideo.media

import android.media.MediaFormat
import java.nio.ByteBuffer


/**
 * Definition of audio and video splitter *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since LearningVideo
 * @version LearningVideo
 * @Datetime 2019-09-02 10:07
 *
 */
interface IExtractor {

    fun getFormat(): MediaFormat?

    /**
     * Read audio and video data     */
    fun readBuffer(byteBuffer: ByteBuffer): Int

    /**
     * Get the current frame time     */
    fun getCurrentTimestamp(): Long

    fun getSampleFlag(): Int

    /**
     * Seek to the specified position and return the timestamp of the actual frame     */
    fun seek(pos: Long): Long

    fun setStartPos(pos: Long)

    /**
     * stop reading data     */
    fun stop()
}