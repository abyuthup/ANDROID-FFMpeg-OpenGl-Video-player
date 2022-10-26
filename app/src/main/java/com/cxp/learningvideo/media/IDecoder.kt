package com.cxp.learningvideo.media

import android.media.MediaFormat


/**
 * Decoder Definition
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since LearningVideo
 * @version LearningVideo
 * @Datetime 2019-09-02 09:49
 *
 */
interface IDecoder: Runnable {

    /**
     * Pause decoding
     */
    fun pause()

    /**
     * continue decoding
     */
    fun goOn()

    /**
     * Jump to the specified position
     * and returns the actual frame time
     *
     * @param pos: milliseconds
     * @return the actual timestamp in milliseconds
     */
    fun seekTo(pos: Long): Long

    /**
     * Jump to the specified position and play
     * and returns the actual frame time
     *
     * @param pos: milliseconds
     * @return the actual timestamp in milliseconds
     */
    fun seekAndPlay(pos: Long): Long

    /**
     * stop decoding
     */
    fun stop()

    /**
     * is decoding
     */
    fun isDecoding(): Boolean

    /**
     *fast forwarding
     */
    fun isSeeking(): Boolean

    /**
     * whether to stop decoding
     */
    fun isStop(): Boolean

    /**
     * set size listener
     */
    fun setSizeListener(l: IDecoderProgress)

    /**
     * set state listener
     */
    fun setStateListener(l: IDecoderStateListener?)

    /**
     * get video width
     */
    fun getWidth(): Int

    /**
     * get video high
     */
    fun getHeight(): Int

    /**
     * Get video length
     */
    fun getDuration(): Long

    /**
     * Current frame time, unit: ms     */
    fun getCurTimeStamp(): Long

    /**
     * Get video rotation angle
     */
    fun getRotationAngle(): Int

    /**
     * Get the format parameters corresponding to the audio and video
     */
    fun getMediaFormat(): MediaFormat?

    /**
     * Get the media track corresponding to the audio and video
     */
    fun getTrack(): Int

    /**
     * Get the decoded file path     */
    fun getFilePath(): String

    /**
     * No need for audio and video synchronization     */
    fun withoutSync(): IDecoder
}