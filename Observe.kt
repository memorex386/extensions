package com.rethinkbooks.observables

import android.arch.lifecycle.LifecycleOwner
import android.databinding.BaseObservable
import android.databinding.Observable
import android.databinding.ObservableField
import com.rethinkbooks.utils.CompositeDisposableInterface
import timber.log.Timber
import kotlin.reflect.KProperty

/**
 * Custom ObservableField that gives more flexibility about getting and setting values and attaching the observable to a parent
 *
 * Created by bradthome on 11/17/17.
 */
open class Observe<T : Any?>(value: T? = null) : ObservableField<T>(value) {

    /**
     * notify a parent Observable when changes are made to this Observable
     */
    var parentObserve: BaseObservable? = null

    var kProp: KProperty<*>? = null

    /**
     * Easy access to get() and set()
     */
    var item: T
        get() = this.get()
        set(value) = this.set(value)

    var acceptChange: (currentItem: T, newItem: T) -> Boolean = { currentItem, newItem -> currentItem != newItem }

    /**
     * Gives flexibility to define logic that is applied before the actually setter is ran
     */
    var customSetter: ((T) -> T)? = null

    /**
     * Gives flexibility to define logic that is applied before the actually setter is ran
     */
    var propertyChanged: ((newItem: T, oldItem: T) -> Unit)? = null

    /**
     * Gives flexibility to define logic that is applied before the actually getter is ran
     */
    var customGetter: ((T) -> T)? = null

    val finalUpdate = ActionItem<T>()

    fun set(value: T, forceUpdate: Boolean = false): Boolean {
        val oldItem = get()
        var item = value
        customSetter?.let {
            item = it(value)
        }
        if (!forceUpdate && !acceptChange(oldItem, item)) return false
        var changed = false
        val changedListener = object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(p0: Observable?, p1: Int) {
                changed = true
            }
        }
        super.addOnPropertyChangedCallback(changedListener)
        super.set(item)
        super.removeOnPropertyChangedCallback(changedListener)
        if (!changed) notifyChange()
        propertyChanged(item, oldItem)
        propertyChanged?.apply {
            this(item, oldItem)
        }
        kProp?.let {
            Timber.d("[${it.name}] value changed to $item from $oldItem")
        }
        parentObserve?.apply {
            this.notifyChange()
        }
//        finalUpdate.actionOccurred(get())
        return true
    }

    infix fun forceUpdate(value: T) {
        set(value, true)
    }

    override infix fun set(value: T) {
        set(value, false)
    }

    override fun get(): T =
            customGetter?.let { it(super.get()) } ?: super.get()

    protected fun propertyChanged(newItem: T, oldItem: T) {

    }

    /**
     * Set Custom Setter and return this
     */
    fun preSetter(setter: ((T) -> T)?): Observe<T> {
        customSetter = setter
        return this
    }

    fun customGetter(getter: ((T) -> T)?): Observe<T> {
        customGetter = getter
        return this
    }

    /**
     * Set Custom Post Setter and return this
     */
    fun configPostSetter(propertyChanged: ((newItem: T, oldItem: T) -> Unit)?): Observe<T> {
        this.propertyChanged = propertyChanged
        return this
    }

    /**
     * Set Custom Getter and return this
     */
    fun preGetter(getter: ((T) -> T)?): Observe<T> {
        customGetter = getter
        return this
    }

    fun configParent(parent: BaseObservable? = null): Observe<T> {
        parentObserve = parent
        return this
    }

    fun acceptChange(acceptChangeItem: (currentItem: T, newItem: T) -> Boolean): Observe<T> {
        this.acceptChange = acceptChangeItem
        return this
    }

    val allowAllChanges: Observe<T>
        get() {
            this.acceptChange = { itemO, itemd -> true }
            return this
        }

    fun syncWithObserve(observe: Observe<T>): Observe<T> {
        this.addOnPropertyChangedCallback { if (observe.item != it) observe.set(it) }
        observe.addOnPropertyChangedCallback { if (observe.item != it) this.set(it) }
        return this
    }

    fun addOnPropertyChangedCallback(action: (T) -> Unit): Observe<T> {
        this.addOnPropertyChangedCallback(object : android.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: android.databinding.Observable?, propertyId: Int) {
                action(this@Observe.get())
            }
        })
        return this
    }

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) {
        super.addOnPropertyChangedCallback(callback)
    }

    fun onPropertiesChanged(action: (newItem: T, oldItem: T) -> Unit): Observe<T> {
        propertyChanged = action
        return this
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        kProp = property
        return get()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        kProp = property
        set(value)
    }

    fun setFinalUpdate(compositeDisposableInterface: CompositeDisposableInterface, action: (T) -> Unit): Observe<T> {
        finalUpdate.observe(compositeDisposableInterface.compositeDisposable, action)
        return this
    }

    fun setFinalUpdate(lifecycleOwner: LifecycleOwner, action: (T) -> Unit): Observe<T> {
        finalUpdate.observe(lifecycleOwner, action)
        return this
    }

    fun setFinalUpdate(action: (T) -> Unit): Observe<T> {
        finalUpdate.observeForever(action)
        return this
    }

    override fun toString(): String {
        return get().toString()
    }

    val string
        get() = toString()

    val <T> T.observe: Observe<T>
        get() = Observe(this).configParent(this@Observe)

    fun <SETTER, GETTER> SETTER.formattedObserve(formattedGetter: (SETTER) -> GETTER, formattedSetter: (GETTER) -> SETTER): Observe<SETTER> {
        return FormattedObserve<SETTER, GETTER>(this, formattedGetter, formattedSetter).configParent(this@Observe)
    }
}

fun <T> Observe<T>.onChanged(action: (T) -> Unit): Observe<T> {
    this.addOnPropertyChangedCallback(action)
    return this
}

/**
 * Extends Observe and gives the flexibility to format the set and get of another type for this Observable
 *
 * Created by bradthome on 11/17/17.
 */
open class FormattedObserve<SETTER : Any?, GETTER : Any?>(value: SETTER? = null, private var formattedGetter: (SETTER) -> GETTER, private var formattedSetter: (GETTER) -> SETTER) : Observe<SETTER>(value) {
    var formatted: GETTER
        get() = formattedGetter(get())
        set(value) = set(formattedSetter(value))

    override fun toString(): String {
        return formatted.toString()
    }

}