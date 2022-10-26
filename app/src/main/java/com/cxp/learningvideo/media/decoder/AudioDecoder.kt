package com.cxp.learningvideo.media.decoder

import android.media.*
import com.cxp.learningvideo.media.BaseDecoder
import com.cxp.learningvideo.media.IExtractor
import com.cxp.learningvideo.media.extractor.AudioExtractor
import java.nio.ByteBuffer


/**
 * Audio codec *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since LearningVideo
 * @version LearningVideo
 * @Datetime 2019-09-03 10:52
 *
 */
class AudioDecoder(path: String): BaseDecoder(path) {
    /**Sampling Rate*/
    private var mSampleRate = -1

    /**Number of sound channels*/
    private var mChannels = 1

    /**PCM sample bits*/
    private var mPCMEncodeBit = AudioFormat.ENCODING_PCM_16BIT

    /**audio player*/
    private var mAudioTrack: AudioTrack? = null

    /**Audio data buffer*/
    private var mAudioOutTempBuf: ShortArray? = null
    
    override fun check(): Boolean {
        return true
    }

    override fun initExtractor(path: String): IExtractor {
        return AudioExtractor(path)
    }

    override fun initSpecParams(format: MediaFormat) {
        try {
            mChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)

            mPCMEncodeBit = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                format.getInteger(MediaFormat.KEY_PCM_ENCODING)
            } else {
                //Without this parameter, the default is 16-bit sampling
                AudioFormat.ENCODING_PCM_16BIT
            }
        } catch (e: Exception) {
        }
    }

    override fun configCodec(codec: MediaCodec, format: MediaFormat): Boolean {
        codec.configure(format, null , null, 0)
        return true
    }

    override fun initRender(): Boolean {
        val channel = if (mChannels == 1) {
            //mono
            AudioFormat.CHANNEL_OUT_MONO
        } else {
            //stereo
            AudioFormat.CHANNEL_OUT_STEREO
        }

        //get minimum buffer
        val minBufferSize = AudioTrack.getMinBufferSize(mSampleRate, channel, mPCMEncodeBit)

        mAudioOutTempBuf = ShortArray(minBufferSize/2)

        mAudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,//Play Type: Music
            mSampleRate, //Sampling Rate
            channel, //通道
            mPCMEncodeBit, //Sampling bits
            minBufferSize, //buffer size
            AudioTrack.MODE_STREAM) //Play mode: data stream is dynamically written, the other is one-time write

        mAudioTrack!!.play()
        return true
    }

    override fun render(outputBuffer: ByteBuffer,
                        bufferInfo: MediaCodec.BufferInfo) {
        if (mAudioOutTempBuf!!.size < bufferInfo.size / 2) {
            mAudioOutTempBuf = ShortArray(bufferInfo.size / 2)
        }
        outputBuffer.position(0)
        outputBuffer.asShortBuffer().get(mAudioOutTempBuf, 0, bufferInfo.size/2)
        mAudioTrack!!.write(mAudioOutTempBuf!!, 0, bufferInfo.size / 2)
    }

    override fun doneDecode() {
        mAudioTrack?.stop()
        mAudioTrack?.release()
    }
}