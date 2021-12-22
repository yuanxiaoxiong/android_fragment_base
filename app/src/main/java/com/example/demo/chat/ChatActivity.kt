package com.example.demo.chat

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.Settings.Global.putString
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.TimeUtils
import com.example.demo.R
import com.example.demo.app.AppManager
import com.example.demo.chat.adapter.ChatListAdapter
import com.example.demo.databinding.FragmentChatBinding
import com.example.demo.chat.bean.Message
import com.example.demo.chat.widget.OverScrollLayout
import com.example.demo.common.receiver.LocalEventLifecycleViewModel
import com.example.demo.common.receiver.event.LocalLifecycleEvent
import com.example.demo.fragment.conversation.mvvm.MessageViewModel
import com.example.demo.utils.HeightProvider
import com.kehuafu.base.core.container.base.BaseActivity
import com.kehuafu.base.core.container.base.BaseFragment
import com.kehuafu.base.core.container.base.adapter.BaseRecyclerViewAdapterV2
import com.kehuafu.base.core.container.widget.toast.showToast
import com.kehuafu.base.core.fragment.constant.NavMode
import com.kehuafu.base.core.ktx.showHasResult
import java.util.*

class ChatActivity :
    BaseActivity<FragmentChatBinding, MessageViewModel, MessageViewModel.MessageState>(),
    BaseRecyclerViewAdapterV2.OnItemClickListener<Message>,
    LocalEventLifecycleViewModel.OnLocalEventCallback<LocalLifecycleEvent> {


    private var heightProvider: HeightProvider? = null

    private var mChatListAdapter = ChatListAdapter()

    private var userId: String? = ""

    private var keyBoardHeight = 0f

    private var messageList: MutableList<Message> = mutableListOf()

    private var showFileMode = false

    companion object {

        const val EXTRAS_TARGET_ID = "com.example.demo.chat.EXTRAS_TARGET_ID"
        const val REQUEST_CODE_CALL = 0x01

        @JvmStatic
        fun showHasResult(targetId: String) {
            ActivityUtils.getTopActivity()
                ?.showHasResult(ChatActivity::class.java, REQUEST_CODE_CALL) {
                    putString(EXTRAS_TARGET_ID, targetId)
                }
        }
    }

    override fun onInflateArgs(arguments: Bundle) {
        super.onInflateArgs(arguments)
        userId = arguments.getString(EXTRAS_TARGET_ID, "")
    }

    override fun onBackPressed() {
        finish()
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onViewCreated(savedInstanceState: Bundle?) {
        BarUtils.setStatusBarLightMode(this, true)
        AppManager.localEventLifecycleViewModel.register(this, this)
        heightProvider = HeightProvider(this).init()
        viewBinding.nav.backIv.setOnClickListener {
            showFileMode = false
            keyBoardHeight = 0F
            finish()
        }

        viewBinding.frameLayout.setOnClickListener {
            showFileMode = false
            keyBoardHeight = 0F
            if (heightProvider!!.isSoftInputVisible) {
                KeyboardUtils.hideSoftInput(this)
            } else {
                viewBinding.chatInputRl.root.translationY = 0F
                viewBinding.chatFile.root.visibility = View.INVISIBLE
            }
        }

        heightProvider!!.setHeightListener {
            if (showFileMode && keyBoardHeight != 0F) {
                viewBinding.chatInputRl.root.translationY = -keyBoardHeight
                viewBinding.chatFile.chatFileLl.minimumHeight = keyBoardHeight.toInt()
                viewBinding.frameLayout.translationY = viewBinding.chatInputRl.root.translationY
                return@setHeightListener
            }
            if (it.toFloat() > 0f) {
                AppManager.keyboardHeight = it.toFloat()
                keyBoardHeight = it.toFloat()
                viewBinding.chatRv.stopScroll()
                if (!showFileMode && keyBoardHeight != 0F) {
                    viewBinding.chatInputRl.root.translationY = -keyBoardHeight
                } else {
                    viewBinding.chatFile.chatFileLl.minimumHeight = it
                    startTranslateY(viewBinding.chatInputRl.root, -it.toFloat())
                }
                viewBinding.chatRv.scrollToPosition(0)
                if (viewBinding.chatRv.findViewHolderForLayoutPosition(0) != null) {
                    viewBinding.frameLayout.translationY = viewBinding.chatInputRl.root.translationY
                    viewBinding.chatInputRl.etMsg.requestFocus()
                } else {
                    viewBinding.frameLayout.translationY = -it.toFloat()
                }
                return@setHeightListener
            }
            viewBinding.chatFile.root.visibility = View.INVISIBLE
            viewBinding.chatInputRl.root.translationY = -it.toFloat()
            viewBinding.frameLayout.translationY = -it.toFloat()
        }

        withViewBinding {
            nav.titleTv.text = "$userId"
            chatInputRl.root.setOnTouchListener { v, event ->
                true
            }
            chatInputRl.ivNavMore.setOnClickListener {
                showFileMode = !showFileMode
                showToast("切换文件模式$showFileMode")
                if (showFileMode) {
                    viewBinding.chatFile.root.visibility = View.VISIBLE
                    if (heightProvider!!.isSoftInputVisible) {
                        KeyboardUtils.hideSoftInput(this@ChatActivity)
                        chatInputRl.etMsg.isVisible = true
                        chatInputRl.tvVoice.isVisible = false
                    } else if (keyBoardHeight == 0F) {
                        if (AppManager.keyboardHeight != 0F) {
                            viewBinding.chatFile.chatFileLl.minimumHeight =
                                AppManager.keyboardHeight.toInt()
                            viewBinding.chatInputRl.root.translationY = -AppManager.keyboardHeight
                            viewBinding.frameLayout.translationY =
                                viewBinding.chatInputRl.root.translationY
                        } else {
                            viewBinding.chatInputRl.root.translationY =
                                -viewBinding.chatFile.root.height.toFloat()
                            viewBinding.frameLayout.translationY =
                                viewBinding.chatInputRl.root.translationY
                        }
                    }
                } else {
                    KeyboardUtils.showSoftInput(this@ChatActivity)
                }
            }
            chatInputRl.ivVoice.setOnClickListener {
                showFileMode = false
                keyBoardHeight = 0F
                if (chatInputRl.etMsg.isVisible) {
                    showToast("切换语音模式")
                    if (heightProvider!!.isSoftInputVisible) {
                        KeyboardUtils.hideSoftInput(this@ChatActivity)
                    } else {
                        viewBinding.chatFile.root.visibility = View.INVISIBLE
                        viewBinding.chatInputRl.root.translationY = -keyBoardHeight
                        viewBinding.frameLayout.translationY = -keyBoardHeight
                    }
                    chatInputRl.ivVoice.setPadding(6, 6, 6, 6)
                    chatInputRl.ivVoice.setImageDrawable(
                        ContextCompat.getDrawable(
                            this@ChatActivity,
                            R.drawable.keyboard
                        )
                    )
                } else {
                    showToast("切换文本模式")
                    if (!heightProvider!!.isSoftInputVisible) {
                        KeyboardUtils.showSoftInput(this@ChatActivity)
                    }
                    chatInputRl.ivVoice.setPadding(0, 0, 0, 0)
                    chatInputRl.ivVoice.setImageDrawable(
                        ContextCompat.getDrawable(
                            this@ChatActivity,
                            R.drawable.voice_icon
                        )
                    )
                }
                chatInputRl.etMsg.isVisible = !chatInputRl.etMsg.isVisible
                chatInputRl.tvVoice.isVisible = !chatInputRl.etMsg.isVisible
            }
            chatInputRl.ivExpression.setOnClickListener {
                showFileMode = false
                showToast("显示我的表情包")
                chatInputRl.ivExpression.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@ChatActivity,
                        R.drawable.keyboard
                    )
                )
            }
            chatInputRl.btnSendMsg.setOnClickListener {
                withViewBinding {
                    viewModel.sendMsg(
                        chatInputRl.etMsg.text.toString().trim(),
                        userId!!,
                        messageList
                    )
                    if (viewBinding.chatRv.canScrollVertically(-1) || viewBinding.chatRv.canScrollVertically(
                            1
                        )
                    ) {
                        viewBinding.frameLayout.translationY = -keyBoardHeight
                    } else if (viewBinding.chatRv.findViewHolderForLayoutPosition(0) != null) {
                        viewBinding.frameLayout.translationY =
                            -viewBinding.chatInputRl.root.translationY
                    }
                    viewBinding.chatRv.scrollToPosition(0)
                    chatInputRl.etMsg.text.clear()
                }
            }
            chatInputRl.etMsg.doAfterTextChanged {
                if (viewBinding.chatInputRl.etMsg.text.toString().trim().isBlank()) {
                    viewBinding.chatInputRl.btnSendMsg.visibility = View.GONE
                    viewBinding.chatInputRl.ivNavMore.visibility = View.VISIBLE
                } else {
                    viewBinding.chatInputRl.btnSendMsg.visibility = View.VISIBLE
                    viewBinding.chatInputRl.ivNavMore.visibility = View.GONE
                }
            }
            chatInputRl.etMsg.setOnClickListener {
                showFileMode = false
                keyBoardHeight = 0F
            }
            mChatListAdapter.setOnItemClickListener(this@ChatActivity)
            chatRv.itemAnimator = null
            chatRv.layoutManager = LinearLayoutManager(this@ChatActivity)
            (chatRv.layoutManager as LinearLayoutManager).reverseLayout = true//列表翻转
            viewBinding.chatRv.adapter = mChatListAdapter
            chatRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (heightProvider!!.isSoftInputVisible) {
                        KeyboardUtils.hideSoftInput(this@ChatActivity)
                    }
                }
            })
        }
    }

    override fun onLoadDataSource() {
        super.onLoadDataSource()
        viewModel.getC2CHistoryMessageList(userId!!)
    }

    override fun onStateChanged(state: MessageViewModel.MessageState) {
        super.onStateChanged(state)
        messageList = state.messageList
        mChatListAdapter.resetItems(messageList)
    }

    /**
     * 软键盘的过渡动画效果
     */
    private fun startTranslateY(view: View, destinationY: Float) {
        val animatorSet = AnimatorSet()

        val currentY: Float = view.translationY
        val translateYAnimator =
            ObjectAnimator.ofFloat(view, "translationY", currentY, destinationY)

        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)

        animatorSet.play(translateYAnimator).after(alphaAnimator)
        animatorSet.duration = 80
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.start()
    }


    override fun onPause() {
        super.onPause()
        if (heightProvider!!.isSoftInputVisible) {
            KeyboardUtils.hideSoftInput(this@ChatActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        AppManager.iCloudMessageManager.markC2CMessageAsRead(userId!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppManager.localEventLifecycleViewModel.unRegister(this)
    }

    override fun onItemClick(itemView: View, item: Message, position: Int?) {
        when (itemView.id) {
            R.id.left_message_avatar, R.id.right_message_avatar -> {
                showToast("头像")
            }
            R.id.left_msg_text, R.id.right_msg_text -> {
                showToast("文本")
            }
            R.id.iv_send_failed -> {
                showToast("重发")
                viewModel.resendMessage(item, userId!!, messageList, position!!)
            }
            else -> {
                if (heightProvider!!.isSoftInputVisible) {
                    KeyboardUtils.hideSoftInput(this@ChatActivity)
                }
            }
        }
    }

    override fun onEventCallback(event: LocalLifecycleEvent) {
        when (event) {
            is LocalLifecycleEvent.ReceivedChatMsgEvent -> {
                if (event.msg.userID.equals(userId)) {
                    val message = Message(
                        mid = event.msg.msgID,
                        uid = event.msg.userID,
                        name = event.msg.nickName,
                        avatar = event.msg.faceUrl,
                        messageContent = event.msg.textElem.text,
                        messageType = Message.MSG_TYPE_TEXT,
                        messageTime = TimeUtils.date2String(TimeUtils.millis2Date(event.msg.timestamp * 1000)),
                        messageSender = event.msg.sender == AppManager.currentUserID,
                        showTime = false,
                        v2TIMMessage = event.msg
                    )
                    messageList.add(0, message)
                    mChatListAdapter.resetItems(messageList)
                    AppManager.iCloudMessageManager.markC2CMessageAsRead(userId!!)
                }
            }
            else -> {

            }
        }
    }
}