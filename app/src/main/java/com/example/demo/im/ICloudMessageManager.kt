package com.example.demo.im

import com.tencent.imsdk.v2.*

interface ICloudMessageManager {

    fun createTextMessage(text: String): V2TIMMessage

    fun createImageMessage(imagePath: String): V2TIMMessage

    fun createSoundMessage(soundPath: String, duration: Int): V2TIMMessage

    fun createVideoMessage(
        videoFilePath: String,
        type: String,
        duration: Int,
        snapshotPath: String
    ): V2TIMMessage

    fun createFileMessage(filePath: String, fileName: String): V2TIMMessage

    fun createLocationMessage(desc: String, longitude: Double, latitude: Double): V2TIMMessage

    fun createFaceMessage(index: Int, data: Byte): V2TIMMessage

    fun sendMessage(
        message: V2TIMMessage,
        receiver: String?,
        groupID: String?,
        priority: Int,
        onlineUserOnly: Boolean? = false,
        offlinePushInfo: V2TIMOfflinePushInfo?,
        callback: V2TIMSendCallback<V2TIMMessage>
    ): String

    fun revokeMessage(msg: V2TIMMessage, callback: V2TIMCallback)

    fun markC2CMessageAsRead(userID: String, callback: V2TIMCallback)

    fun markGroupMessageAsRead(groupID: String, callback: V2TIMCallback)

    fun markAllMessageAsRead(callback: V2TIMCallback)

    fun addAdvancedMsgListener(listener: V2TIMAdvancedMsgListener)

    fun removeAdvancedMsgListener(listener: V2TIMAdvancedMsgListener)

    suspend fun getC2CHistoryMessageList(userID: String, ): List<V2TIMMessage>?
}