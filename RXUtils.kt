package com.rethinkbooks.utils

import android.content.Context
import android.databinding.ObservableField
import com.rethinkbooks.AsyncTaskReplacment.RxAsyncTask
import com.rethinkbooks.observables.*
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit


/**
 * Created by bradley.thome on 10/17/17.
 */


fun <T> CompositeDisposable.asyncMain(single: Single<T>): AsyncObserveResult<T, Throwable> {
    val item = AsyncObserveResult<T, Throwable>()
    add(single.delay(10, TimeUnit.MICROSECONDS).asyncMain.subscribe({
        item.result = it
    }, {
        item.throwable = it
    }))
    return item
}

/**
 * subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
 */
val <T> Single<T>.asyncMain: Single<T>
    get() = this.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

/**
 * subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
 */
val <T> Observable<T>.asyncMain: Observable<T>
    get() = this.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())

/**
 * subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
 */
val <T> Single<T>.async: Single<T>
    get() = this.subscribeOn(Schedulers.io()).observeOn(Schedulers.io())

/**
 * subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
 */
val <T> Observable<T>.async: Observable<T>
    get() = this.subscribeOn(Schedulers.io()).observeOn(Schedulers.io())

enum class AsyncType {
    IO_MAIN, IO_IO, CURRENT_CURRENT, NEW_MAIN, NEW_NEW;

    val subscribeOn
        get() = when (this) {
            IO_IO -> Schedulers.io()
            IO_MAIN -> Schedulers.io()
            NEW_MAIN -> Schedulers.newThread()
            NEW_NEW -> Schedulers.newThread()
            CURRENT_CURRENT -> Schedulers.io()
        }

    val observeOn
        get() = when (this) {
            IO_IO -> Schedulers.io()
            IO_MAIN -> AndroidSchedulers.mainThread()
            NEW_MAIN -> AndroidSchedulers.mainThread()
            NEW_NEW -> Schedulers.newThread()
            CURRENT_CURRENT -> Schedulers.io()
        }
}

fun <T> Single<T>.async(asyncType: AsyncType): Single<T> {
    return when (asyncType) {
        AsyncType.CURRENT_CURRENT -> this
        else -> subscribeOn(asyncType.subscribeOn).observeOn(asyncType.observeOn)
    }
}

fun <T> Single<T>.asyncResult(asyncType: AsyncType = AsyncType.IO_MAIN, compositeDisposable: CompositeDisposable = CompositeDisposable()): AsyncObserveResult<T, Throwable> {
    val item = AsyncObserveResult<T, Throwable>()
    compositeDisposable.add(delay(10, TimeUnit.MICROSECONDS).async(asyncType).subscribe({
        item.result = it
    }, {
        item.throwable = it
    }))
    return item
}

class RetrofitResult<T : Any?> : AsyncObserveResult<T, Throwable>()

open class AsyncObserveResult<T : Any?, ERROR : Any?>() {
    val isSuccessful
        get() = when (throwable) {
            null -> false
            else -> true
        }

    var result: T? = null
        set(value) {
            field = value
            successFun?.let {
                field?.apply(it)
                successFun = null
            }
        }
    var throwable: ERROR? = null
        set(value) {
            field = value
            failFun?.let {
                field?.apply(it)
                failFun = null
            }
        }

    private var successFun: ((T) -> Unit)? = null
    private var failFun: ((ERROR) -> Unit)? = null

    fun ifSuccessful(success: (T) -> Unit): AsyncObserveResult<T, ERROR> {
        if (result != null) {
            result?.let(success)
        } else {
            successFun = success
        }
        return this;
    }

    fun ifFail(fail: (ERROR) -> Unit): AsyncObserveResult<T, ERROR> {
        if (throwable != null) {
            throwable?.let(fail)
        } else {
            failFun = fail
        }
        return this;
    }

    fun ifFail(failResult: AsyncObserveResult<*, ERROR>): AsyncObserveResult<T, ERROR> {
        return ifFail {
            failResult.throwable = it
        }
    }

    fun ifSuccessful(successResult: AsyncObserveResult<T, *>): AsyncObserveResult<T, ERROR> {
        return ifSuccessful {
            successResult.result = it
        }
    }
}

fun <T> Single<T>.subscribe(asyncType: AsyncType, compositeDisposable: CompositeDisposable = CompositeDisposable()): AsyncObserveResult<T, Throwable> {
    val asyncResult = AsyncObserveResult<T, Throwable>()
    compositeDisposable.add(delay(10, TimeUnit.MICROSECONDS).async(asyncType).subscribe({
        asyncResult.result = it
    }, {
        asyncResult.throwable = it
    }))
    return asyncResult
}

fun <T> CompositeDisposable.subscribe(observableField: ObservableField<T>, subscription: (T) -> Unit) = subscribe(observableField, false, subscription)

fun <T> CompositeDisposable.subscribe(observableField: ObservableField<T>, throwOnError: Boolean, subscription: (T) -> Unit) {
    add(observableField.rxObservable.subscribe({
        try {
            subscription(it)
        } catch (e: Exception) {
            throw e
        }
    }, {
        Timber.e(it)
        "RX_ERROR for $observableField : $it".timber.e
        if (throwOnError) throw it
    }))
}

fun <T> ObservableField<T>.subscribe(compositeDisposable: CompositeDisposable, subscription: (T) -> Unit) = subscribe(compositeDisposable, false, subscription)

fun <T> ObservableField<T>.subscribe(compositeDisposable: CompositeDisposable, throwOnError: Boolean, subscription: (T) -> Unit) {
    compositeDisposable.add(rxObservable.subscribe({
        try {
            subscription(it)
        } catch (e: Exception) {
            throw e
        }
    }, {
        Timber.e(it)
        "RX_ERROR : $it".timber.e
        if (throwOnError) throw it
    }))
}

fun <T> ObservableField<T>.subscribe(subscription: (T) -> Unit) {
    addOnPropertyChangedCallback(object : android.databinding.Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(p0: android.databinding.Observable?, p1: Int) {
            subscription(this@subscribe.get())
        }
    })
}

fun <T> Observe<T>.subscribe(observe: Observe<T>, ifTrue: (item: T) -> Boolean = { true }) {
    addOnPropertyChangedCallback(object : android.databinding.Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(p0: android.databinding.Observable?, p1: Int) {
            if (ifTrue(item)) {
                observe set item
            }
        }
    })
}


fun <T> (() -> T).asyncResult(asyncType: AsyncType = AsyncType.IO_MAIN, compositeDisposable: CompositeDisposable = CompositeDisposable()): AsyncObserveResult<T, Throwable> {
    return Single.create<T> {
        try {
            it.onSuccess(this())
        } catch (e: Exception) {
            it.onError(e)
        }
    }.asyncResult(asyncType, compositeDisposable)
}

val <T> ObservableField<T>.rxObservable: Observable<T>
    get() {
    return Observable.create({ emitter ->

        val callback = object : android.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(dataBindingObservable: android.databinding.Observable, propertyId: Int) {
                if (dataBindingObservable === this@rxObservable) {
                    emitter.onNext(this@rxObservable.get())
                }
            }
        }

        this@rxObservable.addOnPropertyChangedCallback(callback)

        emitter.setCancellable({ this@rxObservable.removeOnPropertyChangedCallback(callback) })
    })
}

val <T> T.observe: Observe<T>
    get() = Observe(this)

val <T> T.observeNull
    get() = Observe<T?>(this)

inline fun <reified T> observeItem(item: T? = null, clazz: Class<T> = T::class.java) = Observe<T?>(item)

val <T> T.rxObservable: Observable<T>
    get() = Observable.just(this)

val <T> T.rxSingle: Single<T>
    get() = Single.just(this)

fun delay(delay: Long, unit: TimeUnit = TimeUnit.MILLISECONDS, callback: () -> Unit) = Completable.timer(delay, unit, AndroidSchedulers.mainThread()).subscribe(callback)

fun CompositeDisposable.delay(delay: Long, unit: TimeUnit = TimeUnit.MILLISECONDS, callback: () -> Unit) = add(com.rethinkbooks.utils.delay(delay, unit, callback))

fun <T> Observable<T>.subscribeSuccess(action: (T) -> Unit) = this.subscribe(action)

fun <T> Single<T>.subscribeSuccess(action: (T) -> Unit) = this.subscribe(action)

class RxObservableUpdate<T>(val item: T) {

    lateinit var emitter: ObservableEmitter<T>
        private set
    val rxObservable: Observable<T>

    init {
        rxObservable = Observable.create<T> {
            emitter = it
        }
    }

    fun <I> observe(initialValue: I, preSetter: (I) -> I = { it }, onChanged: (I) -> Unit = {}): Observe<I> {
        return Observe(initialValue).apply {
            this.preSetter(preSetter)
            this.onChanged(onChanged)
        }
    }

}

/*
open class OnChangeDelegate<T>(item: T) : ObservableProperty<T>(item) {

    open var observableField: Observe<T> = Observe(item)

    open var onlyUpdateIfValueChanged = true

    override fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean {
        val attemptedNewValue = observableField.customSetter?.let { it(newValue) } ?: newValue
        if (onlyUpdateIfValueChanged && oldValue == attemptedNewValue) return false
        return true
    }

    override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
        super.afterChange(property, oldValue, newValue)
        observableField.apply {
            set(newValue)
        }
    }

    fun addOnPropertyChangedCallback(onChanged: (T) -> Unit): OnChangeDelegate<T> {
        observableField.addOnPropertyChangedCallback(onChanged)
        return this
    }

    fun syncWithObserve(onChanged: Observe<T>): OnChangeDelegate<T> {
        observableField.addOnPropertyChangedCallback {
            onChanged set it
        }
        return this
    }

    fun preSetter(preSetter: (T) -> T): OnChangeDelegate<T> {
        observableField.preSetter(preSetter)
        return this
    }

    fun customGetter(customGetter: (T) -> T): OnChangeDelegate<T> {
        observableField.preGetter(customGetter)
        return this
    }

    fun onChangedValues(postSetter: (item: T, oldItem: T) -> Unit): OnChangeDelegate<T> {
        observableField.configPostSetter(postSetter)
        return this
    }

    fun onChanged(postSetter: (item: T) -> Unit): OnChangeDelegate<T> {
        observableField.addOnPropertyChangedCallback { postSetter(it) }
        return this
    }

    fun acceptChange(acceptChange: (currentItem: T, newItem: T) -> Boolean): OnChangeDelegate<T> {
        observableField.acceptChange = acceptChange
        return this
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val item = super.getValue(thisRef, property)
        return observableField.customGetter?.let { it(item) } ?: item
    }
}


val <T> T.changeDelegate
    get() = OnChangeDelegate<T>(this)
*/
object CustomDelegates {
    fun <T> observe(item: T) = Observe<T>(item)

}


fun <SETTER, GETTER> SETTER.formattedObserve(formattedGetter: (SETTER) -> GETTER, formattedSetter: (GETTER) -> SETTER): Observe<SETTER> {
    return FormattedObserve<SETTER, GETTER>(this, formattedGetter, formattedSetter)
}

fun <T> createSingle(creator: (SingleEmitter<T>) -> Unit): Single<T> {
    return Single.create<T>(creator)
}

val <T> (() -> T).single
    get() = Single.create<T> { it.onSuccess(this()) }

interface CompositeDisposableInterface {
    val compositeDisposable: CompositeDisposable

    fun <T> Observe<T>.onChanged(action: (T) -> Unit): Observe<T> {
        compositeDisposable.subscribe(this, action)
        return this
    }

    fun <T> Observe<T>.updateFrom(observe: Observe<T>): Observe<T> {
        observe.onChanged {
            this set it
        }
        return this
    }

    fun <T> ObservableFieldItemAction<T>.onChanged(action: (T) -> Unit): ObservableFieldItemAction<T> {
        this.observe(compositeDisposable, action)
        return this
    }

    fun <T> ObservableField<T>.subscribe(subscription: (T) -> Unit) {
        compositeDisposable.subscribe(this, subscription)
    }

    fun <Params> RxAsyncTask<Params, *, *>.execute(vararg params: Params) {
        this.disposable(compositeDisposable).run(*params)
    }

    fun Network.hasNetwork(context: Context, callback: (NetworkStatus) -> Unit) = hasNetwork(context, compositeDisposable, callback)

    fun <T> ActionItem<T>.subscribe(sub: (T) -> Unit) {
        this.observe(compositeDisposable, sub)
    }

    fun <T> ActionItem<T>.onChanged(action: (T) -> Unit) = onActionTaken(action)

    fun <T> ActionItem<T>.onActionTaken(action: (T) -> Unit): ActionItem<T> {
        this.observe(compositeDisposable, action)
        return this
    }

    fun Action.onActionTaken(action: () -> Unit): Action {
        this.observe(compositeDisposable) {
            action()
        }
        return this
    }

    fun <T> Single<T>.asyncResult(asyncType: AsyncType = AsyncType.IO_MAIN) = this.asyncResult(asyncType, compositeDisposable)

}
