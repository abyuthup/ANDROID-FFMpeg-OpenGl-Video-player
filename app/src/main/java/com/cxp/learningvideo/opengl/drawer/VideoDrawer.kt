package com.cxp.learningvideo.opengl.drawer

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer


/**
 * 视频渲染器
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since LearningVideo
 * @version LearningVideo
 * @Datetime 2019-10-26 15:45
 *
 */
class VideoDrawer : IDrawer {

    // vertex coordinates
    private val mVertexCoors = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )

    // texture coordinates
    private val mTextureCoors = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    private var mWorldWidth: Int = -1
    private var mWorldHeight: Int = -1
    private var mVideoWidth: Int = -1
    private var mVideoHeight: Int = -1

    private var mTextureId: Int = -1

    private var mSurfaceTexture: SurfaceTexture? = null

    private var mSftCb: ((SurfaceTexture) -> Unit)? = null

    //OpenGL program ID
    private var mProgram: Int = -1

    //Matrix transformation receiver
    private var mVertexMatrixHandler: Int = -1

    // vertex coordinate receiver
    private var mVertexPosHandler: Int = -1

    // texture coordinate receiver
    private var mTexturePosHandler: Int = -1

    // texture receiver
    private var mTextureHandler: Int = -1

    // Translucent value receiver
    private var mAlphaHandler: Int = -1

    private lateinit var mVertexBuffer: FloatBuffer
    private lateinit var mTextureBuffer: FloatBuffer

    private var mMatrix: FloatArray? = null

    private var mAlpha = 1f

    init {
//[Step 1: Initialize vertex coordinates]
        initPos()
    }

    private fun initPos() {
        val bb = ByteBuffer.allocateDirect(mVertexCoors.size * 4)
        bb.order(ByteOrder.nativeOrder())
        //Convert coordinate data to FloatBuffer for passing to OpenGL ES program
        mVertexBuffer = bb.asFloatBuffer()
        mVertexBuffer.put(mVertexCoors)
        mVertexBuffer.position(0)

        val cc = ByteBuffer.allocateDirect(mTextureCoors.size * 4)
        cc.order(ByteOrder.nativeOrder())
        mTextureBuffer = cc.asFloatBuffer()
        mTextureBuffer.put(mTextureCoors)
        mTextureBuffer.position(0)
    }

    private var mWidthRatio = 1f
    private var mHeightRatio = 1f
    private fun initDefMatrix() {
        if (mMatrix != null) return
        if (mVideoWidth != -1 && mVideoHeight != -1 &&
            mWorldWidth != -1 && mWorldHeight != -1
        ) {
            mMatrix = FloatArray(16)
            var prjMatrix = FloatArray(16)
            val originRatio = mVideoWidth / mVideoHeight.toFloat()
            val worldRatio = mWorldWidth / mWorldHeight.toFloat()
            if (mWorldWidth > mWorldHeight) {
                if (originRatio > worldRatio) {
                    mHeightRatio = originRatio / worldRatio
                    /*  public static void orthoM(float[] m, int mOffset,
                        float left, float right, float bottom, float top,
                        float near, float far)  */
                    Matrix.orthoM(
                        prjMatrix, 0,
                        -mWidthRatio, mWidthRatio,
                        -mHeightRatio, mHeightRatio,
                        3f, 5f
                    )
                } else {// The original scale is smaller than the window scale, scaling the height will cause the height to exceed, therefore, the height is subject to the window, and the scaling width
                    //originRatio < worldRatio
                    mWidthRatio = worldRatio / originRatio
                    Matrix.orthoM(
                        prjMatrix, 0,
                        -mWidthRatio, mWidthRatio,
                        -mHeightRatio, mHeightRatio,
                        3f, 5f
                    )
                }
            } else {
                //mWorldWidth < mWorldHeight
                if (originRatio > worldRatio) {
                    mHeightRatio = originRatio / worldRatio
                    Matrix.orthoM(
                        prjMatrix, 0,
                        -mWidthRatio, mWidthRatio,
                        -mHeightRatio, mHeightRatio,
                        3f, 5f
                    )
                } else {// The original scale is smaller than the window scale, scaling the height will cause the height to exceed, therefore, the height is based on the window, scaling the width
                    mWidthRatio = worldRatio / originRatio
                    Matrix.orthoM(
                        prjMatrix, 0,
                        -mWidthRatio, mWidthRatio,
                        -mHeightRatio, mHeightRatio,
                        3f, 5f
                    )
                }
            }

            //Set camera position
            val viewMatrix = FloatArray(16)
            /**
             * Defines a viewing transformation in terms of an eye point, a center of
             * view, and an up vector.
             *
             * @param rm returns the result
             * @param rmOffset index into rm where the result matrix starts
             * @param eyeX eye point X
             * @param eyeY eye point Y
             * @param eyeZ eye point Z
             * @param centerX center of view X
             * @param centerY center of view Y
             * @param centerZ center of view Z
             * @param upX up vector X
             * @param upY up vector Y
             * @param upZ up vector Z
             */


            Matrix.setLookAtM(
                viewMatrix, 0,
                0f, 0f, 5.0f,
                0f, 0f, 0f,
                0f, 1.0f, 0f
            )
            //Calculate the transformation matrix
            Matrix.multiplyMM(mMatrix, 0, prjMatrix, 0, viewMatrix, 0)
        }
    }

    override fun setVideoSize(videoW: Int, videoH: Int) {
        mVideoWidth = videoW
        mVideoHeight = videoH
    }

    override fun setWorldSize(worldW: Int, worldH: Int) {
        mWorldWidth = worldW
        mWorldHeight = worldH
    }

    override fun setAlpha(alpha: Float) {
        mAlpha = alpha
    }

    override fun setTextureID(id: Int) {
        mTextureId = id
        mSurfaceTexture = SurfaceTexture(id)
        mSftCb?.invoke(mSurfaceTexture!!)
    }

    override fun getSurfaceTexture(cb: (st: SurfaceTexture) -> Unit) {
        mSftCb = cb
    }

    override fun draw() {
        if (mTextureId != -1) {
            initDefMatrix()
            //[Step 2: Create, compile and start OpenGL shader]
            createGLPrg()
            //[Step 3: Activate and bind texture units]
            activateTexture()
            //[Step 4: Bind the image to the texture unit]
            updateTexture()
            //[Step 5: Start rendering and drawing]
            doDraw()
        }
    }

    private fun createGLPrg() {
        if (mProgram == -1) {
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, getVertexShader())
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, getFragmentShader())

            //Create an OpenGL ES program, note: it needs to be created in the OpenGL rendering thread, otherwise it cannot be rendered
            mProgram = GLES20.glCreateProgram()
            //Add the vertex shader to the program
            GLES20.glAttachShader(mProgram, vertexShader)
            //Add the fragment shader to the program
            GLES20.glAttachShader(mProgram, fragmentShader)
            //connect to the shader program
            GLES20.glLinkProgram(mProgram)

            mVertexMatrixHandler = GLES20.glGetUniformLocation(mProgram, "uMatrix")
            mVertexPosHandler = GLES20.glGetAttribLocation(mProgram, "aPosition")
            mTextureHandler = GLES20.glGetUniformLocation(mProgram, "uTexture")
            mTexturePosHandler = GLES20.glGetAttribLocation(mProgram, "aCoordinate")
            mAlphaHandler = GLES20.glGetAttribLocation(mProgram, "alpha")
        }
        //Use OpenGL programs
        GLES20.glUseProgram(mProgram)
    }

    private fun activateTexture() {
        //Activate the specified texture unit
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        //Bind the texture ID to the texture unit
        //replace all ordinary texture units with extended texture units
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId)
        // Pass the active texture unit into the shader
        GLES20.glUniform1i(mTextureHandler, 0)
        //Configure edge transition parameters
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
    }

    private fun updateTexture() {
        mSurfaceTexture?.updateTexImage()
    }

    private fun doDraw() {
        //handle to enable vertex
        GLES20.glEnableVertexAttribArray(mVertexPosHandler)
        GLES20.glEnableVertexAttribArray(mTexturePosHandler)
        GLES20.glUniformMatrix4fv(mVertexMatrixHandler, 1, false, mMatrix, 0)
        //Set the shader parameters, the second parameter represents the amount of data contained in a vertex, here is xy, so it is 2
        GLES20.glVertexAttribPointer(mVertexPosHandler, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer)
        GLES20.glVertexAttribPointer(
            mTexturePosHandler,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            mTextureBuffer
        )
        GLES20.glVertexAttrib1f(mAlphaHandler, mAlpha)
        //start drawing
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    override fun release() {
        GLES20.glDisableVertexAttribArray(mVertexPosHandler)
        GLES20.glDisableVertexAttribArray(mTexturePosHandler)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDeleteTextures(1, intArrayOf(mTextureId), 0)
        GLES20.glDeleteProgram(mProgram)
    }

    private fun getVertexShader(): String {
        return "attribute vec4 aPosition;" +
                "precision mediump float;" +
                "uniform mat4 uMatrix;" +
                "attribute vec2 aCoordinate;" +
                "varying vec2 vCoordinate;" +
                "attribute float alpha;" +
                "varying float inAlpha;" +
                "void main() {" +
                "    gl_Position = uMatrix*aPosition;" +
                "    vCoordinate = aCoordinate;" +
                "    inAlpha = alpha;" +
                "}"
    }

    private fun getFragmentShader(): String {
        //一Be sure to add a newline "\n", otherwise it will be mixed with the precision of the next line, resulting in a compilation error

        /*
        #extension GL_OES_EGL_image_external : require

        We already know that the video image color space is YUV,
        and to display it on the screen, the image is RGB. Therefore,
        to render the video image to the screen, YUV must be converted to RGB.
        Extending the texture does this transformation.*/

        /*texture unit in the fourth row has also been replaced with an extended texture unit.*/

        return "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;" +
                "varying vec2 vCoordinate;" +
                "varying float inAlpha;" +
                "uniform samplerExternalOES uTexture;" +
                "void main() {" +
                "  vec4 color = texture2D(uTexture, vCoordinate);" +
                "  gl_FragColor = vec4(color.r, color.g, color.b, inAlpha);" +
                "}"
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        //Create vertex shader or fragment shader according to type
        val shader = GLES20.glCreateShader(type)
        //Add the resource to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        return shader
    }

    fun translate(dx: Float, dy: Float) {
        Matrix.translateM(mMatrix, 0, dx * mWidthRatio * 2, -dy * mHeightRatio * 2, 0f)
    }

    fun scale(sx: Float, sy: Float) {
        Matrix.scaleM(mMatrix, 0, sx, sy, 1f)
        mWidthRatio /= sx
        mHeightRatio /= sy
    }
}