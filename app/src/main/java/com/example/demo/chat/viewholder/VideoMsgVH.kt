package com.example.demo.chat.viewholder

import android.graphics.BitmapFactory
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import androidx.core.view.isVisible
import com.example.demo.R
import com.example.demo.chat.bean.Message
import com.example.demo.databinding.LayItemChatImageMsgBinding
import com.example.demo.databinding.LayItemChatTextMsgBinding
import com.example.demo.databinding.LayItemChatVideoMsgBinding
import com.example.demo.utils.DensityTool
import com.kehuafu.base.core.container.base.adapter.BaseRecyclerViewAdapterV4
import com.kehuafu.base.core.ktx.loadImage
import com.tencent.imsdk.v2.V2TIMValueCallback

class VideoMsgVH(override val viewBinding: LayItemChatVideoMsgBinding) :
    BaseRecyclerViewAdapterV4.BaseViewHolder<Message>(
        viewBinding
    ) {
    override fun setState(item: Message, position: Int) {
        super.setState(item, position)
        setStateToVideoMsg(viewBinding, item, position)
    }

    private fun setStateToVideoMsg(
        viewBinding: LayItemChatVideoMsgBinding,
        item: Message,
        position: Int
    ) {
        viewBinding.leftMessageAvatar.setOnClickListener {
            mOnItemClickListener?.onItemClick(it, item = item, position)
        }
        viewBinding.rightMessageAvatar.setOnClickListener {
            mOnItemClickListener?.onItemClick(it, item = item, position)
        }
        viewBinding.leftMsgVv.setOnClickListener {
            mOnItemClickListener?.onItemClick(it, item = item, position)
        }
        viewBinding.rightMsgVv.setOnClickListener {
            mOnItemClickListener?.onItemClick(it, item = item, position)
        }
        viewBinding.ivSendFailed.setOnClickListener {
            mOnItemClickListener?.onItemClick(it, item = item, position)
        }
        if (item.messageSender) {
            viewBinding.layoutRightText.visibility = View.VISIBLE
            viewBinding.rightVideoDuration.text =
                item.secToTime(item.v2TIMMessage.videoElem.duration)
            val height =
                item.v2TIMMessage.videoElem.snapshotHeight / 8 * 2.5
            val width = item.v2TIMMessage.videoElem.snapshotWidth / 8 * 2.5
            if (height.toInt() != 0 && width.toInt() != 0) {
                DensityTool.setWH(viewBinding.rightMsgVv, width.toInt(), height.toInt())
            } else {
                if (!item.loading) {
                    val opts = BitmapFactory.Options()
                    BitmapFactory.decodeFile(item.messageContent, opts)
                    val imageWidth: Int = (opts.outWidth / 8 * 2.5).toInt()
                    val imageHeight: Int = (opts.outHeight / 8 * 2.5.toInt())
                    Log.e("@@@@@@", "loadImage-messageSender-->${imageWidth}${imageHeight}")
                    DensityTool.setWH(viewBinding.rightMsgVv, imageWidth, imageHeight)
                }
            }
            viewBinding.rightMsgVv.tag = item.messageContent
            if (TextUtils.equals(viewBinding.rightMsgVv.tag as String, item.messageContent)) {
                viewBinding.rightMsgVv.loadImage(item.messageContent)
            }
            viewBinding.rightIvSendSuccess.isVisible = !item.loading
        } else {
            viewBinding.layoutLeftText.visibility = View.VISIBLE
            viewBinding.leftIvSendSuccess.visibility = View.VISIBLE
            viewBinding.leftVideoDuration.text =
                item.secToTime(item.v2TIMMessage.videoElem.duration)
            val height =
                item.v2TIMMessage.videoElem.snapshotHeight / 8 * 2.5
            val width = item.v2TIMMessage.videoElem.snapshotWidth / 8 * 2.5
            DensityTool.setWH(viewBinding.leftMsgVv, width.toInt(), height.toInt())
            item.v2TIMMessage.videoElem.getSnapshotUrl(object : V2TIMValueCallback<String> {
                override fun onSuccess(p0: String?) {
                    Log.e("@@@@@@", "loadImage-onSuccess-->${p0}")
                    viewBinding.rightMsgVv.tag = p0
                    if (TextUtils.equals(viewBinding.rightMsgVv.tag as String, p0)) {
                        viewBinding.rightMsgVv.loadImage(p0)
                    }
                }

                override fun onError(p0: Int, p1: String?) {
                    Log.e("@@@@@@", "loadImage-onError-->${p1}")
                }
            })
        }
        viewBinding.tvTime.isVisible = item.showTime!!
        viewBinding.tvTime.text = item.messageTime
        if (item.loading) {
            viewBinding.ivSendLoading.visibility = View.VISIBLE
            val mOperatingAnimCenter =
                AnimationUtils.loadAnimation(viewBinding.root.context, R.anim.loading_rotate_center)
            mOperatingAnimCenter.interpolator = LinearInterpolator()
            viewBinding.ivSendLoading.startAnimation(mOperatingAnimCenter)
            viewBinding.leftIvSendSuccess.visibility = View.GONE
        } else {
            viewBinding.ivSendLoading.visibility = View.GONE
        }
        viewBinding.ivSendFailed.isVisible = item.sendFailed
    }
}