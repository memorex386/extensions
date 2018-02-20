package com.rethinkbooks.observables

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.support.annotation.MainThread
import com.rethinkbooks.utils.subscribe
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

/**
 * Created by bradley.thome on 10/19/17.
 */
class ResultsHolder<out T : Any?>(val item: T)

open class ObservableFieldItemAction<T : Any?> {
    //LiveData that backs up our LiveDataAction
    val liveData = Observe<ResultsHolder<T>?>(null)

    @MainThread
    fun observe(compositeDisposable: CompositeDisposable, observer: (T) -> Unit) {
        liveData.subscribe(compositeDisposable) { t ->
            t?.let {
                observer(it.item)
                liveData.set(null)
            }
        }
    }

    @MainThread
    fun actionOccurred(item: T) {
        //set backing liveData to true
        liveData.set(ResultsHolder(item))
    }
}

class ObservableFieldAction : ObservableFieldItemAction<Boolean>() {

    @MainThread
    fun actionOccurred() {
        //set backing liveData to true
        liveData.set(ResultsHolder(false))
    }
}


open class ActionItem<T : Any?> {
    //LiveData that backs up our LiveDataAction
    val liveData = MutableLiveData<ResultsHolder<T>?>()

    @MainThread
    fun observe(owner: LifecycleOwner, observer: (T) -> Unit) {

        // Observe the internal MutableLiveData
        liveData.observe(owner, Observer<ResultsHolder<T>?> { t ->
            t?.let {
                observer(it.item)
                liveData.value = null
            }
        })
    }

    @MainThread
    fun observe(compositeDisposable: CompositeDisposable, observer: (T) -> Unit) {
        // Observe the internal MutableLiveData

        val observe = Observable.create<ResultsHolder<T>?> {

            val observerItem = Observer<ResultsHolder<T>?> { t ->
                t?.let {
                    observer(it.item)
                    liveData.value = null
                }
            }

            it.setCancellable {
                liveData.removeObserver(observerItem)
            }

            liveData.observeForever(observerItem)
        }.subscribe({}, {}, {}, {})

        compositeDisposable.add(observe)

    }
    /**
     * This function allows easy testing without needing a LifecycleOwner.
     */
    @MainThread
    fun observeForever(observer: (T) -> Unit) {
        // Observe the internal MutableLiveData
        liveData.observeForever({ t ->
            t?.let {
                observer(it.item)
                liveData.value = null
            }
        })
    }

    @MainThread
    fun actionOccurred(item: T) {
        //set backing liveData to true
        //   if (Looper.myLooper() == Looper.getMainLooper())
        liveData.value = ResultsHolder(item)
    }
}

class Action : ActionItem<Boolean>() {
    @MainThread
    fun actionOccurred() {
        //set backing liveData to true
        liveData.value = ResultsHolder(true)
    }
}
