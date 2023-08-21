package com.bestswlkh0310.rc

import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream

    init {
        try {
            val tmpIn = socket.inputStream
            val tmpOut = socket.outputStream
            inputStream = tmpIn!!
            outputStream = tmpOut!!
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun run() {
        val buffer = ByteArray(1024)
        while (true) {
            try {
                var bytes = inputStream.available()
                if (bytes != 0) {
                    bytes = inputStream.read(buffer, 0, bytes)
                    val receivedData = buffer.copyOfRange(0, bytes)
                    val receivedString = String(receivedData, Charsets.UTF_8)
                    Log.d("TAG", "$receivedString - run() called")
                }
            } catch (e: IOException) {
                Log.d("TAG", "${e.message} - run() called")
                e.printStackTrace()
                break
            }
        }
    }

    fun write(input: String) {
        val bytes = input.toByteArray()
        try {
            outputStream.write(bytes)
        } catch (e: IOException) {
            Log.d("TAG", "${e.message} - write() called")
            e.printStackTrace()
        }
    }

    fun cancel() {
        try {
            socket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}