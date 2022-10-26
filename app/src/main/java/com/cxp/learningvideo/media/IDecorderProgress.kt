package com.cxp.learningvideo.media


/**
 * Decoding progress *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since LearningVideo
 * @version LearningVideo
 * @Datetime 2019-09-02 09:54
 *
 */
interface IDecoderProgress {
    /**
     * Video width and height callback     */
    fun videoSizeChange(width: Int, height: Int, rotationAngle: Int)

    /**
     * Video playback progress callback     */
    fun videoProgressChange(pos: Long)
}