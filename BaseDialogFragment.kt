package com.rethinkbooks.main

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.lifecycle.ViewModelProviders
import android.support.v4.app.DialogFragment
import com.rethinkbooks.AsyncTaskReplacment.RxAsyncTask
import io.reactivex.disposables.CompositeDisposable

/**
 * Created by bradthome on 12/4/17.
 */
abstract class BaseDialogFragment : DialogFragment() {

    protected val compositeDisposable = CompositeDisposable()

    init {
        lifecycle.addObserver(ObserveLifecycle())
    }

    inner class ObserveLifecycle : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onResume() {
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun onPause() {
        }


        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            compositeDisposable.clear()
        }
    }

    fun <Params> RxAsyncTask<Params, *, *>.execute(vararg params: Params) {
        this.disposable(compositeDisposable).run(*params)
    }

}

abstract class BaseDialogViewModelFragment : BaseDialogFragment() {
    protected val viewModel by lazy { ViewModelProviders.of(this).get(viewModelClass) }
    abstract protected val viewModelClass: Class<BaseViewModel>

}