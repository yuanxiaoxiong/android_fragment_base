package com.example.demo.chat

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.*
import com.example.demo.R
import com.example.demo.app.AppManager
import com.example.demo.chat.adapter.ChatFileTypeAdapter
import com.example.demo.chat.adapter.ChatListMultipleAdapter
import com.example.demo.databinding.FragmentChatBinding
import com.example.demo.chat.bean.MessageTheme
import com.example.demo.common.receiver.LocalEventLifecycleViewModel
import com.example.demo.common.receiver.event.LocalLifecycleEvent
import com.kehuafu.base.core.container.base.BaseActivity
import com.kehuafu.base.core.container.base.adapter.BaseRecyclerViewAdapterV2
import com.kehuafu.base.core.ktx.showHasResult
import com.tencent.imsdk.v2.V2TIMMessage
import java.util.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.demo.chat.adapter.ChatEmoTypeAdapter
import com.example.demo.chat.bean.IMessage
import com.example.demo.chat.bean.Message
import com.example.demo.chat.bean.MessageEmo
import com.example.demo.chat.mvvm.MessageViewModel
import com.example.demo.chat.widget.ChatInputView
import com.example.demo.utils.*
import com.example.demo.preview.PreviewActivity
import com.kehuafu.base.core.container.base.adapter.BaseRecyclerViewAdapterV4
import com.kehuafu.base.core.container.widget.toast.showToast
import com.kehuafu.base.core.ktx.dp2px
import com.kehuafu.base.core.ktx.show
import com.kehuafu.base.core.ktx.toJsonTxt
import java.io.File
import java.lang.reflect.Field
import java.util.regex.Matcher
import java.util.regex.Pattern


open class ChatActivity :
    BaseActivity<FragmentChatBinding, MessageViewModel, MessageViewModel.MessageState>(),
    BaseRecyclerViewAdapterV4.OnItemClickListener<Message>, ChatInputView.OnChatInputViewListener,
    ActivityResultLauncherUtils.OnLauncherDataListener,
    LocalEventLifecycleViewModel.OnLocalEventCallback<LocalLifecycleEvent> {


    private var heightProvider: HeightProvider? = null

    private var mChatListAdapter = ChatListMultipleAdapter()

    private var mChatFileTypeAdapter = ChatFileTypeAdapter()

    private var mChatEmoTypeAdapter = ChatEmoTypeAdapter()

    private var userId: String? = ""

    private var messageList: MutableList<Message> = mutableListOf()

    private lateinit var lp: WindowManager.LayoutParams

    private lateinit var mPop: PopupWindow

    private lateinit var mAudioRecodeUtils: AudioRecodeUtils

    private lateinit var rl: RelativeLayout

    private var mRecordTime: Long = 0L

    private lateinit var mActivityResultLauncherUtils: ActivityResultLauncherUtils

    companion object {
        const val EXTRAS_TARGET_ID = "com.example.demo.chat.EXTRAS_TARGET_ID"

        @JvmStatic
        fun showHasResult(targetId: String) {
            ActivityUtils.getTopActivity()
                ?.showHasResult(ChatActivity::class.java) {
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
        mActivityResultLauncherUtils = ActivityResultLauncherUtils.newInstance(this)
        mActivityResultLauncherUtils.setOnLauncherDataListener(this)
        viewBinding.chatInputRv.setOnChatInputViewListener(this)
        viewBinding.nav.backIv.setOnClickListener {
            finish()
        }
        withViewBinding {
            nav.titleTv.text = "$userId"
            mChatListAdapter.setOnItemClickListener(this@ChatActivity)
            chatRv.itemAnimator = null
            chatRv.layoutManager = LinearLayoutManager(this@ChatActivity)
            (chatRv.layoutManager as LinearLayoutManager).reverseLayout = true
            chatRv.adapter = mChatListAdapter

            chatFile.removeRmoIv.setOnClickListener {
                try {
                    val pattern: Pattern = Pattern.compile("\\[.+?\\]")
                    //匹配所有带有[]的词语
                    val index = chatInputRv.etMsg().selectionStart
                    val endChar = chatInputRv.etMsg().text.toString().substring(index - 1, index)
                    if (chatInputRv.etMsg().text.contains(pattern.toRegex()) && endChar == "]"
                    ) {
                        val matcher: Matcher = pattern.matcher(chatInputRv.etMsg().text)
                        if (matcher.find()) {
                            chatInputRv.etMsg().text.delete(
                                index - (matcher.end() - matcher.start()),
                                index
                            )
                        } else {
                            chatInputRv.etMsg().text.delete(
                                index - 1,
                                index
                            )
                        }
                    } else {
                        chatInputRv.etMsg().text.delete(
                            index - 1,
                            index
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            mChatEmoTypeAdapter.setOnItemClickListener(object :
                BaseRecyclerViewAdapterV2.OnItemClickListener<MessageEmo> {
                override fun onItemClick(itemView: View, item: MessageEmo, position: Int?) {
                    try {
                        val index = if (position!! <= 9) {
                            "0$position"
                        } else {
                            position
                        }
                        //获取表情图片文件名
                        val field: Field = R.drawable::class.java.getDeclaredField("wx$index")
                        val resourceId = field.getInt(null)
                        // 在android中要显示图片信息，必须使用Bitmap位图的对象来装载
                        val bitmap: Bitmap = BitmapFactory.decodeResource(resources, resourceId)
                        //要让图片替代指定的文字用ImageSpan
                        val imageSpan = ImageSpan(
                            this@ChatActivity,
                            ImageResizeUtil.imageScale(
                                bitmap,
                                dp2px(22f).toInt(),
                                dp2px(22f).toInt()
                            )!!
                        )
                        val spannableString = SpannableString(item.title)
                        spannableString.setSpan(
                            imageSpan,
                            0,
                            item.title.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        chatInputRv.etMsg().text.append(spannableString)
                        chatInputRv.etMsg().setSelection(chatInputRv.etMsg().text.length)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })

            mChatFileTypeAdapter.setOnItemClickListener(object :
                BaseRecyclerViewAdapterV2.OnItemClickListener<MessageTheme> {
                override fun onItemClick(itemView: View, item: MessageTheme, position: Int?) {
                    when (itemView.id) {
                        R.id.iv_type_image -> {
                            PermissionUtils.permission(
                                PermissionConstants.STORAGE,
                                PermissionConstants.CAMERA,
                                PermissionConstants.LOCATION
                            )
                                .callback(object : PermissionUtils.FullCallback {
                                    override fun onGranted(permissionsGranted: List<String>) {
                                        LogUtils.d(permissionsGranted)
                                        when (item.title) {
                                            "相册" -> {
                                                mActivityResultLauncherUtils.launchAlbum()
                                            }
                                            "拍摄" -> {
                                                mActivityResultLauncherUtils.launchCameraUri()
                                            }
                                            "文件" -> {
                                                mActivityResultLauncherUtils.launchVideoPick()
                                            }
                                        }
                                    }

                                    override fun onDenied(
                                        permissionsDeniedForever: List<String>,
                                        permissionsDenied: List<String>
                                    ) {
                                        LogUtils.d(permissionsDeniedForever, permissionsDenied)
                                    }
                                })
                                .theme { activity -> ScreenUtils.setFullScreen(activity) }
                                .request()
                        }
                        else -> {

                        }
                    }
                }
            })
            chatFile.chatFileRv.itemAnimator = null
            chatFile.chatFileRv.layoutManager = GridLayoutManager(this@ChatActivity, 4)
            chatFile.chatFileRv.adapter = mChatFileTypeAdapter
            chatRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (heightProvider!!.isSoftInputVisible) {
                        KeyboardUtils.hideSoftInput(this@ChatActivity)
                    } else {
                        if (ChatInputView.showKeyBoardMode == ChatInputView.KEY_BOARD_MODE_FILE
                            || ChatInputView.showKeyBoardMode == ChatInputView.KEY_BOARD_MODE_EXPRESSION
                        ) {
                            ChatInputView.showKeyBoardMode = ChatInputView.KEY_BOARD_MODE_TEXT
                        }
                        viewBinding.chatInputLl.translationY = dp2px(300f)
                        viewBinding.frameLayout.translationY = dp2px(0f)
                    }
                }
            })
        }
        initKeyBoardHeightListener()
        initRecordVoice()
    }

    private fun initKeyBoardHeightListener() {
        heightProvider = HeightProvider(this).init()
        heightProvider!!.setHeightListener {
            if (ChatInputView.showKeyBoardMode == ChatInputView.KEY_BOARD_MODE_FILE
                || ChatInputView.showKeyBoardMode == ChatInputView.KEY_BOARD_MODE_EXPRESSION
            ) {
                return@setHeightListener
            } else if (ChatInputView.showKeyBoardMode == ChatInputView.KEY_BOARD_MODE_TEXT) {
                viewBinding.chatInputLl.translationY = (-it + dp2px(300f))
                viewBinding.frameLayout.translationY = -it.toFloat()
                return@setHeightListener
            }
            if (it.toFloat() > 0f) {
                viewBinding.chatRv.stopScroll()
//                AnimatorUtils.build()
//                    .startTranslateY(viewBinding.chatInputLl, (-it + dp2px(300f)))//带动画效果
                viewBinding.chatInputLl.translationY = (-it + dp2px(300f))
                viewBinding.frameLayout.translationY = -it.toFloat()
                viewBinding.chatRv.scrollToPosition(0)
                return@setHeightListener
            }
            viewBinding.chatInputLl.translationY = -it + dp2px(300f)
            viewBinding.frameLayout.translationY = it.toFloat()
        }
    }

    private fun initRecordVoice() {
        rl = View.inflate(this, R.layout.popup_window, null) as RelativeLayout
        //设置空白的背景色
        lp = window.attributes
        mPop = PopupWindow(rl)
        val micImage = rl.findViewById<ImageView>(R.id.iv_pro)
        val recordingTime = rl.findViewById<TextView>(R.id.recording_time)
        mAudioRecodeUtils = AudioRecodeUtils()
        mAudioRecodeUtils.setOnAudioStatusUpdateListener(object :
            AudioRecodeUtils.OnAudioStatusUpdateListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onUpdate(db: Double, time: Long) {
                //根据分贝值来设置录音时话筒图标的上下波动
                mRecordTime = time
                micImage.drawable.level = (3000 + 6000 * db / 100).toInt()
                recordingTime.text = LocalDateUtils.getDateCoverString(time)
            }

            override fun onStop(filePath: String) {
                if (mRecordTime < 1500) { //判断，如果录音时间小于1.5秒，则删除文件提示，过短
                    val file = File(filePath)
                    if (file.exists()) { //判断文件是否存在，如果存在删除文件
                        file.delete() //删除文件
                        showToast("录音时间过短")
                    }
                } else {
                    try {
                        recordingTime.text = "00:00"
                        viewModel.sendSoundMsg(
                            filePath,
                            (mRecordTime / 1000).toInt(),
                            userId!!,
                            messageList
                        )
                        mRecordTime = 0
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    override fun onLoadDataSource() {
        super.onLoadDataSource()
        viewModel.getC2CHistoryMessageList(userId!!, true)
        viewModel.initMessageThemeList()
        viewModel.initMessageEmoList()
    }

    override fun onStateChanged(state: MessageViewModel.MessageState) {
        super.onStateChanged(state)
        if (state.initThemed) {
            mChatFileTypeAdapter.resetItems(state.messageTheme)
            state.initThemed = false
        }
        if (state.initEmo) {
            mChatEmoTypeAdapter.resetItems(state.messageEmo)
            state.initEmo = false
        }
        if (state.updateMessage) {
            messageList = state.messageList
            mChatListAdapter.resetItems(messageList)
            state.updateMessage = false
        }
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
        MediaPlayerManager.getInstance().release()
        ChatInputView.resetKeyBoardMode()
    }

    override fun onItemClick(itemView: View, item: Message, position: Int?) {
        when (itemView.id) {
            R.id.left_message_avatar, R.id.right_message_avatar -> {
                showToast("头像")
            }
            R.id.left_message_ll, R.id.right_message_ll -> {
                showToast("文本")
            }
            R.id.iv_send_failed -> {
                showToast("重发")
                viewModel.resendMessage(item, userId!!, messageList, position!!)
            }
            R.id.msg_vv -> {
                when (item.messageType) {
                    IMessage.MSG_TYPE_IMAGE, IMessage.MSG_TYPE_VIDEO -> {
                        val msg = messageList.filter {
                            it.messageType == IMessage.MSG_TYPE_IMAGE || it.messageType == IMessage.MSG_TYPE_VIDEO
                        }
                        PreviewActivity.showHasResult(
                            msg.toJsonTxt(),
                            msg.indexOf(item)
                        )
                    }
                }
            }
            else -> {
                if (heightProvider!!.isSoftInputVisible) {
                    KeyboardUtils.hideSoftInput(this@ChatActivity)
                } else {
                    viewBinding.chatInputLl.translationY = dp2px(300f)
                    viewBinding.frameLayout.translationY = dp2px(0f)
                }
            }
        }
    }

    override suspend fun onEventCallback(event: LocalLifecycleEvent) {
        when (event) {
            is LocalLifecycleEvent.ReceivedChatMsgEvent -> {
                if (event.msg.userID.equals(userId)) {
                    val build = Message.build(event.msg)
                    val message = Message(
                        mid = event.msg.msgID,
                        uid = event.msg.userID,
                        name = event.msg.nickName,
                        avatar = event.msg.faceUrl,
                        messageContent = build.messageContent(),
                        videoUrl = build.getVideoUrl(),
                        imageUlr = build.getImageUrl(),
                        soundUlr = build.getSoundUrl(),
                        messageType = event.msg.elemType,
                        messageTime = TimeUtils.date2String(TimeUtils.millis2Date(event.msg.timestamp * 1000)),
                        messageSender = event.msg.sender == AppManager.currentUserID,
                        showTime = false,
                        loading = event.msg.status == V2TIMMessage.V2TIM_MSG_STATUS_SENDING,
                        sendFailed = event.msg.status == com.tencent.imsdk.message.Message.V2TIM_MSG_STATUS_SEND_FAILED,
                        v2TIMMessage = event.msg
                    )
                    messageList.add(0, message)
                    runOnUiThread {
                        mChatListAdapter.resetItems(messageList)
                    }
                    AppManager.iCloudMessageManager.markC2CMessageAsRead(userId!!)
                }
            }
            else -> {

            }
        }
    }

    override fun onRecordVoice(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lp.alpha = 0.4f
                window.attributes = lp
                mPop.width = LinearLayout.LayoutParams.MATCH_PARENT
                mPop.height = LinearLayout.LayoutParams.MATCH_PARENT
                mPop.showAtLocation(rl, Gravity.CENTER, 0, 0)
                mAudioRecodeUtils.startRecord()
            }
            MotionEvent.ACTION_UP -> {
                //恢复背景色
                lp.alpha = 1f
                window.attributes = lp
                mAudioRecodeUtils.stopRecord() //结束录音（保存录音文件）
                mPop.dismiss()
            }
        }
        return true
    }

    override fun onSendMsg(msg: String) {
        viewModel.sendTextMsg(
            msg,
            userId!!,
            messageList
        )
    }

    override fun onPullUpList(bool: Boolean) {
        if (bool) {
            if (viewBinding.frameLayout.translationY != 0F) {
                return
            }
            viewBinding.chatRv.stopScroll()
            AnimatorUtils.build()
                .startTranslateY(viewBinding.chatInputLl, dp2px(0f))
            viewBinding.frameLayout.translationY = -dp2px(300f)
            viewBinding.chatRv.scrollToPosition(0)
        } else {
            if (heightProvider!!.isSoftInputVisible) {
                KeyboardUtils.hideSoftInput(this)
            } else if (viewBinding.frameLayout.translationY != 0F) {
                viewBinding.chatInputLl.translationY = dp2px(300f)
                viewBinding.frameLayout.translationY = dp2px(0f)
            }
        }
    }

    override fun onShowEmo(show: Boolean) {
        if (show) {
            viewBinding.chatFile.chatFileRv.layoutManager =
                GridLayoutManager(this@ChatActivity, 8)
            viewBinding.chatFile.chatFileRv.adapter = mChatEmoTypeAdapter
            viewBinding.chatFile.chatFileRv.layoutParams.height = dp2px(250f).toInt()
            viewModel.initMessageEmoList()
        } else {
            viewBinding.chatFile.chatFileRv.layoutManager =
                GridLayoutManager(this@ChatActivity, 4)
            viewBinding.chatFile.chatFileRv.adapter = mChatFileTypeAdapter
            viewModel.initMessageThemeList()
        }
    }

    override fun onKeyBoardInputChange(empty: Boolean) {
        if (empty) {
            viewBinding.chatFile.removeRmoIv.setColorFilter(
                ContextCompat.getColor(
                    this,
                    R.color.message_space_line
                )
            )
        } else {
            viewBinding.chatFile.removeRmoIv.setColorFilter(
                ContextCompat.getColor(
                    this,
                    R.color.text_color
                )
            )
        }
    }

    override fun onLauncherForActivityResult(path: String?, type: Int) {
        when (type) {
            ActivityResultLauncherUtils.LAUNCHER_TYPE_ALBUM, ActivityResultLauncherUtils.LAUNCHER_TYPE_CAMERA -> {
                viewModel.sendImageMsg(
                    path!!,
                    userId!!,
                    messageList
                )
            }
        }
    }

    override fun onLauncherForActivityResult(
        path: String,
        firstUrl: String,
        duration: Int,
        type: Int
    ) {
        super.onLauncherForActivityResult(path, firstUrl, duration, type)
        if (type == ActivityResultLauncherUtils.LAUNCHER_TYPE_VIDEO) {
            viewModel.sendVideoMsg(
                path,
                firstUrl,
                duration,
                userId!!,
                messageList
            )
        }
    }
}