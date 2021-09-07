package com.example.mvr.core.ktx

import android.app.Activity
import android.view.View
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.example.mvr.core.container.delegate.BaseViewBindingDelegate
import com.example.mvr.core.container.delegate.ViewLifecycle


@MainThread
inline fun <reified VB : ViewBinding> View.viewBindings(view: View? = null): BaseViewBindingDelegate<View, VB> {
    val viewViewBindingDelegate: BaseViewBindingDelegate<View, VB> =
        BaseViewBindingDelegate(VB::class.java, view)
    val lifecycleOwner = ViewLifecycle(this)
    lifecycleOwner.lifecycle.addObserver(viewViewBindingDelegate)
    return viewViewBindingDelegate
}

@MainThread
inline fun <reified VB : ViewBinding> Fragment.viewBindings(): BaseViewBindingDelegate<Fragment, VB> {
    val viewViewBindingDelegate: BaseViewBindingDelegate<Fragment, VB> =
        BaseViewBindingDelegate(VB::class.java)
    lifecycle.addObserver(viewViewBindingDelegate)
    return viewViewBindingDelegate
}

@MainThread
inline fun <reified VB : ViewBinding> AppCompatActivity.viewBindings(): BaseViewBindingDelegate<Activity, VB> {
    val viewViewBindingDelegate: BaseViewBindingDelegate<Activity, VB> =
        BaseViewBindingDelegate(VB::class.java)
    lifecycle.addObserver(viewViewBindingDelegate)
    return viewViewBindingDelegate
}