package com.cxp.learningvideo.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.nio.ByteBuffer


/**
 * Decoder base class
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since LearningVideo
 * @version LearningVideo
 * @Datetime 2019-09-02 09:43
 *
 */
abstract class BaseDecoder(private val mFilePath: String): IDecoder {

    private val TAG = "BaseDecoder"

    //-------------thread related------------------------
    /**
     * Whether the decoder is running
     */
    private var mIsRunning = true

    /**
     *thread waiting for lock
     */
    private val mLock = Object()

    /**
     * Can enter decoding
     */
    private var mReadyForDecode = false

    //---------------Status related-----------------------
    /**
     * Audio and video decoder
     */
    private var mCodec: MediaCodec? = null

    /**
     * Audio and video data reader
     */
    private var mExtractor: IExtractor? = null

    /**
     * decode input buffer
     */
    private var mInputBuffers: Array<ByteBuffer>? = null

    /**
     * decode output buffer
     */
    private var mOutputBuffers: Array<ByteBuffer>? = null

    /**
     * decode data information
     */
    private var mBufferInfo = MediaCodec.BufferInfo()

    private var mState = DecodeState.STOP

    protected var mStateListener: IDecoderStateListener? = null

    /**
     * Whether the stream data has ended
     */
    private var mIsEOS = false

    protected var mVideoWidth = 0

    protected var mVideoHeight = 0

    private var mDuration: Long = 0

    private var mStartPos: Long = 0

    private var mEndPos: Long = 0

    /**
     * Start decoding time for audio and video synchronization
     */
    private var mStartTimeForSync = -1L

    // Whether audio and video rendering synchronization is required
    private var mSyncRender = true

    final override fun run() {
        if (mState == DecodeState.STOP) {
            mState = DecodeState.START
        }
        mStateListener?.decoderPrepare(this)

        //[Decoding steps: 1. Initialize and start the decoder]
        if (!init()) return

        Log.i(TAG, "start decoding")
        try {
            while (mIsRunning) {
                if (mState != DecodeState.START &&
                    mState != DecodeState.DECODING &&
                    mState != DecodeState.SEEKING) {
                    Log.i(TAG, "enter wait：$mState")

                    waitDecode()

                    // ---------【Synchronization time correction】------------
                    //Start time to restore synchronization, that is, remove the time waiting for loss
                    mStartTimeForSync = System.currentTimeMillis() - getCurTimeStamp()
                }

                if (!mIsRunning ||
                    mState == DecodeState.STOP) {
                    mIsRunning = false
                    break
                }

                if (mStartTimeForSync == -1L) {
                    mStartTimeForSync = System.currentTimeMillis()
                }

                //If the data is not decoded, push the data into the decoder to decode
                if (!mIsEOS) {
                    //[Decoding steps: 2. See data into the decoder input buffer]
                    mIsEOS = pushBufferToDecoder()
                }

                //[Decoding step: 3. Pull the decoded data out of the buffer]
                var index = 0

                index = pullBufferFromDecoder()
                if (index >= 0) {
                    // ---------【Audio and video synchronization】-------------
                    if (mSyncRender && mState == DecodeState.DECODING) {
                        sleepRender()
                    }
                    //[Decoding step: 4. Rendering]

                    //the rendering of the video does not require the client to render manually. It only needs to provide the drawing surface surface,
                    //call releaseOutputBuffer, and set the two parameters to true. So, there is no need to do anything here

                    if (mSyncRender) {// No need to render if it's just for encoding and compositing a new video
                        render(mOutputBuffers!![index], mBufferInfo)
                    }

                    //Pass the decoded data out
                    val frame = Frame()
                    frame.buffer = mOutputBuffers!![index]
                    frame.setBufferInfo(mBufferInfo)
                    mStateListener?.decodeOneFrame(this, frame)

                    //[Decoding step: 5. Release the output buffer]
                    //The second parameter named render is used to decide whether to display this frame of data during video decoding
                    mCodec!!.releaseOutputBuffer(index, true)

                    if (mState == DecodeState.START) {
                        mState = DecodeState.PAUSE
                    }
                }
                //[Decoding steps: 6. Determine whether decoding is complete]
                if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    Log.i(TAG, "end of decoding")
                    mState = DecodeState.FINISH
                    mStateListener?.decoderFinish(this)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            doneDecode()
            release()
        }
    }

    private fun init(): Boolean {
        if (mFilePath.isEmpty() || !File(mFilePath).exists()) {
            Log.w(TAG, "file path is empty")
            mStateListener?.decoderError(this, "file path is empty")
            return false
        }

        if (!check()) return false

        //Initialize the data extractor
        mExtractor = initExtractor(mFilePath)
        if (mExtractor == null ||
            mExtractor!!.getFormat() == null) {
            Log.w(TAG, "Could not parse file")
            return false
        }

        //Initialization parameters
        if (!initParams()) return false

        //Initialize the renderer
        if (!initRender()) return false

        //Initialize the decoder
        if (!initCodec()) return false
        return true
    }

    private fun initParams(): Boolean {
        try {
            val format = mExtractor!!.getFormat()!!
            mDuration = format.getLong(MediaFormat.KEY_DURATION) / 1000
            if (mEndPos == 0L) mEndPos = mDuration

            initSpecParams(mExtractor!!.getFormat()!!)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private fun initCodec(): Boolean {
        try {
            val type = mExtractor!!.getFormat()!!.getString(MediaFormat.KEY_MIME)
            mCodec = MediaCodec.createDecoderByType(type)
            if (!configCodec(mCodec!!, mExtractor!!.getFormat()!!)) {
                waitDecode()
            }
            mCodec!!.start()

            mInputBuffers = mCodec?.inputBuffers
            mOutputBuffers = mCodec?.outputBuffers
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private fun pushBufferToDecoder(): Boolean {
        //Query whether there is an available input buffer, returning the buffer index. The parameter 1000 is to wait for 1000ms. If you fill in -1, it will wait indefinitely.
        var inputBufferIndex = mCodec!!.dequeueInputBuffer(1000)
        var isEndOfStream = false

        if (inputBufferIndex >= 0) {
            val inputBuffer = mInputBuffers!![inputBufferIndex]

            //copy the encoded data to input buffer and returns the total sample size copied to InputBuffer
            val sampleSize = mExtractor!!.readBuffer(inputBuffer)

            if (sampleSize < 0) {
                //If the data has been fetched, push the end of data flag：MediaCodec.BUFFER_FLAG_END_OF_STREAM
                mCodec!!.queueInputBuffer(inputBufferIndex, 0, 0,
                    0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                isEndOfStream = true
            } else {
                //queueInputBuffer to push data into the decoder.
                mCodec!!.queueInputBuffer(inputBufferIndex, 0,
                    sampleSize, mExtractor!!.getCurrentTimestamp(), 0)
            }
        }
        return isEndOfStream
    }

    //Pull the decoded data out of the buffer
    private fun pullBufferFromDecoder(): Int {
        // Query whether there is decoded data. When index >=0, it means the data is valid, and index is the buffer index
        var index = mCodec!!.dequeueOutputBuffer(mBufferInfo, 1000)
        when (index) {
            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {}
            MediaCodec.INFO_TRY_AGAIN_LATER -> {}
            MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                mOutputBuffers = mCodec!!.outputBuffers
            }
            else -> {
                return index
            }
        }
        return -1
    }

    private fun sleepRender() {
        val passTime = System.currentTimeMillis() - mStartTimeForSync
        val curTime = getCurTimeStamp()
        if (curTime > passTime) {
            Thread.sleep(curTime - passTime)
        }
    }

    private fun release() {
        try {
            Log.i(TAG, "Decoding stopped, release decoder")
            mState = DecodeState.STOP
            mIsEOS = false
            mExtractor?.stop()
            mCodec?.stop()
            mCodec?.release()
            mStateListener?.decoderDestroy(this)
        } catch (e: Exception) {
        }
    }

    /**
     * Decoding thread goes to wait
     */
    private fun waitDecode() {
        try {
            if (mState == DecodeState.PAUSE) {
                mStateListener?.decoderPause(this)
            }
            synchronized(mLock) {
                mLock.wait()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Notifies the decoding thread to continue running
     */
    protected fun notifyDecode() {
        synchronized(mLock) {
            mLock.notifyAll()
        }
        if (mState == DecodeState.DECODING) {
            mStateListener?.decoderRunning(this)
        }
    }

    override fun pause() {
        mState = DecodeState.DECODING
    }

    override fun goOn() {
        mState = DecodeState.DECODING
        notifyDecode()
    }

    override fun seekTo(pos: Long): Long {
        return 0
    }

    override fun seekAndPlay(pos: Long): Long {
        return 0
    }

    override fun stop() {
        mState = DecodeState.STOP
        mIsRunning = false
        notifyDecode()
    }

    override fun isDecoding(): Boolean {
        return mState == DecodeState.DECODING
    }

    override fun isSeeking(): Boolean {
        return mState == DecodeState.SEEKING
    }

    override fun isStop(): Boolean {
        return mState == DecodeState.STOP
    }

    override fun setSizeListener(l: IDecoderProgress) {
    }

    override fun setStateListener(l: IDecoderStateListener?) {
        mStateListener = l
    }

    override fun getWidth(): Int {
        return mVideoWidth
    }

    override fun getHeight(): Int {
        return mVideoHeight
    }

    override fun getDuration(): Long {
        return mDuration
    }

    override fun getCurTimeStamp(): Long {
        return mBufferInfo.presentationTimeUs / 1000
    }

    override fun getRotationAngle(): Int {
        return 0
    }

    override fun getMediaFormat(): MediaFormat? {
        return mExtractor?.getFormat()
    }

    override fun getTrack(): Int {
        return 0
    }

    override fun getFilePath(): String {
        return mFilePath
    }

    override fun withoutSync(): IDecoder {
        mSyncRender = false
        return this
    }

    /**
     * Check subclass parameters
     */
    abstract fun check(): Boolean

    /**
     * Initialize the data extractor
     */
    abstract fun initExtractor(path: String): IExtractor

    /**
     * Initialize the subclass's own specific parameters
     */
    abstract fun initSpecParams(format: MediaFormat)

    /**
     * Configure the decoder
     */
    abstract fun configCodec(codec: MediaCodec, format: MediaFormat): Boolean

    /**
     * Initialize the renderer
     */
    abstract fun initRender(): Boolean

    /**
     * render
     */
    abstract fun render(outputBuffer: ByteBuffer,
                        bufferInfo: MediaCodec.BufferInfo)

    /**
     * end decoding
     */
    abstract fun doneDecode()
}