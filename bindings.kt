package com.rethinkbooks.bookshout.utils

import android.databinding.BindingAdapter
import android.support.v7.widget.Toolbar
import com.rethinkbooks.main.BaseLifecycleActivity

/**
 * Created by bradthome on 1/27/18.
 */
@BindingAdapter("default")
fun toolbarDefault(toolbar: Toolbar, set: Boolean) {
    val baseLifecycleActivity = toolbar.context as? BaseLifecycleActivity
    baseLifecycleActivity?.apply {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
    }
}