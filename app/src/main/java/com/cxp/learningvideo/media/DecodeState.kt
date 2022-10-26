package com.cxp.learningvideo.media


/**
 * decoding state
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since LearningVideo
 * @version LearningVideo
 * @Datetime 2019-09-02 10:00
 *
 */
enum class DecodeState {
    /**Start state*/
    START,
    /**Decoding*/
    DECODING,
    /**Decoding pause*/
    PAUSE,
    /**Fast forwarding */
    SEEKING,
    /**Decoding complete*/
    FINISH,
    /**Decoder release*/
    STOP
}
