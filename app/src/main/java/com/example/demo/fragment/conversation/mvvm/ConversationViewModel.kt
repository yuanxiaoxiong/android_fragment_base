package com.example.demo.fragment.conversation.mvvm

import com.blankj.utilcode.util.TimeUtils
import com.example.demo.app.AppManager
import com.kehuafu.base.core.container.widget.toast.showToast
import com.example.demo.base.BaseRequestViewModel
import com.example.demo.fragment.conversation.bean.Conversation
import com.example.demo.fragment.mine.mvvm.MineViewModel
import com.kehuafu.base.core.ktx.asyncCall
import com.kehuafu.base.core.redux.Action
import com.kehuafu.base.core.redux.IState
import com.kehuafu.base.core.redux.Reducer


class ConversationViewModel : BaseRequestViewModel<ConversationViewModel.ConversationState>(
    initialState = ConversationState(),
    reducers = listOf(reducer())
) {
    companion object {
        private fun reducer(): Reducer<ConversationState> {
            return { state, action ->
                when (action) {
                    is MessageAction.Success -> {
                        state.copy(
                            conversationList = action.conversationList
                        )
                    }
                    is MessageAction.NetWorkStatusChanged -> {
                        state.copy(
                            netConnected = action.conn
                        )
                    }
                    else -> {
                        state
                    }
                }
            }
        }
    }

    fun getConversationList() {
        httpAsyncCall({
            showToast(it.errorMsg)
        }) {
            val conList = mutableListOf<Conversation>()
            val conversationList =
                AppManager.iCloudConversationManager.getConversationList(0, 100, null)
            for (conversation in conversationList) {
                val message = Conversation(
                    mid = conversation.conversationID,
                    uid = conversation.userID,
                    name = conversation.showName,
                    avatar = conversation.faceUrl,
                    messageContent = Conversation.messageContent(conversation.lastMessage),
                    messageType = conversation.lastMessage.elemType,
                    messageTime = TimeUtils.date2String(TimeUtils.millis2Date(conversation.lastMessage.timestamp * 1000)),
                    messageSender = conversation.userID == AppManager.currentUserID,
                    messageUnreadCount = conversation.unreadCount,
                    messageState = conversation.lastMessage.status,
                    v2TIMMessage = conversation.lastMessage
                )
                conList.add(message)
            }
            dispatch(MessageAction.Success(conversationList = conList))
        }
    }

    fun netWorkStatusChanged(conn: Boolean) {
        asyncCall({
            showToast(it.errorMsg)
        }) {
            dispatch(MessageAction.NetWorkStatusChanged(conn = conn))
        }
    }

    sealed class MessageAction : Action {
        class Success(val conversationList: MutableList<Conversation>) : MessageAction()
        class NetWorkStatusChanged(val conn: Boolean) : MessageAction()
    }

    data class ConversationState(
        val conversationList: MutableList<Conversation> = mutableListOf(),
        val netConnected: Boolean = true
    ) : IState
}