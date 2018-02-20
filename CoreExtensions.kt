package com.rethinkbooks.utils

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.support.annotation.IdRes
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.webkit.WebView
import android.widget.BaseAdapter
import android.widget.TextView
import com.rethinkbooks.observables.Observe
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.Serializable
import java.lang.reflect.Method
import java.util.*
import kotlin.math.roundToInt
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KVisibility
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.safeCast
import kotlin.reflect.full.starProjectedType


/**
 * Created by bradthome on 11/20/17.
 */

object CoreExtensions


object JSONDefaults {
    val OBJECT = "{}"
    val ARRAY = "[]"
}


/**
 * Check if object is equal and optional function to check against for custom variables
 * @param attemptedObj - obj to check
 * @param equalsFunction - OPTIONAL - if [attemptedObj] is the same class, then it is passed to [equalsFunction] for extra checks
 */
fun <CURRENT : Any> CURRENT.baseEquals(attemptedObj: Any?, equalsFunction: ((attemptedObj: CURRENT) -> Boolean) = { true }): Boolean =
        attemptedObj?.let { attemptedObjNotNull ->

            // if obj is a reference than true
            if (this === attemptedObjNotNull)
                return true

            if (attemptedObjNotNull.javaClass != javaClass)
                return false

            javaClass.kotlin.safeCast(attemptedObjNotNull)?.let {
                return equalsFunction(it)
            }
        } ?: false

inline fun <reified CURRENT : Any> CURRENT.baseEqualsTest(attemptedObj: Any?, baseEquals: (item: CURRENT) -> Boolean): Boolean {
    attemptedObj ?: return false
    return javaClass.kotlin.safeCast(attemptedObj)?.let(baseEquals) ?: false
}

/**
 * @return a formatted string in K, M, G, T, etc
 */
fun Long.format(): String {
    val value = this
    // Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
    if (value == java.lang.Long.MIN_VALUE) return (java.lang.Long.MIN_VALUE + 1).format()
    if (value < 0) return "-" + (-value).format()
    if (value < 1000) return java.lang.Long.toString(value) //deal with easy case

    val suffixes = TreeMap<Long, String>()
    suffixes.put(1_000L, "k");
    suffixes.put(1_000_000L, "M");
    suffixes.put(1_000_000_000L, "G");
    suffixes.put(1_000_000_000_000L, "T");
    suffixes.put(1_000_000_000_000_000L, "P");
    suffixes.put(1_000_000_000_000_000_000L, "E");

    val e = suffixes.floorEntry(value)
    val divideBy = e.key
    val suffix = e.value

    val truncated = value / (divideBy!! / 10) //the number part of the output times 10
    val hasDecimal = truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()
    return if (hasDecimal) (truncated / 10.0).toString() + suffix else (truncated / 10).toString() + suffix
}

val Any?.isNull: Boolean
    get() = (this == null)

val Any?.isEmpty: Boolean
    get() = this?.let { item ->
        if (item is String) {
            item.isBlank()
        }
        false
    } ?: true


/**
 * If the String is not null and is not blank
 */
val String?.exists
    get() = this?.isNotBlank() ?: false

/**
 * If the Number is not null and is greater than -1
 */
val Number?.exists
    get() = this?.let {
        when (it) {
            is Int -> it >= 0
            is Double -> it >= 0
            is Long -> it >= 0
            is Float -> it >= 0
            else -> false
        }
    } ?: false

/**
 * If the String is not null and is not blank then it runs the function and returns the result of the function else null
 */
inline fun <R : Any?> String?.ifExistsReturn(ifExitsFun: String.() -> R): R? {
    return this?.let {
        it.exists.ifTrueReturn {
            it.ifExitsFun()
        }
    }
}

/**
 * If the Number is not null and is greater than -1, then it runs the function and returns the result of the function else null
 */
inline fun <T : Number, R : Any?> T?.ifExistsReturn(ifExitsFun: T.() -> R): R? {
    return this?.let {
        it.exists.ifTrueReturn {
            it.ifExitsFun()
        }
    }
}

/**
 * If the String is not null and is not blank then it runs the function
 */
inline fun String?.ifExists(ifExitsFun: String.() -> Unit): String? {
    this?.let {
        it.exists.ifTrue {
            it.ifExitsFun()
        }
    }
    return this
}

inline fun String?.ifNotExists(ifNotExitsFun: () -> Unit): String? {
    this.exists.ifFalse { ifNotExitsFun() }
    return this
}

/**
 * If the Number is not null and is greater than -1, then it runs the function
 */
inline fun <T : Number> T?.ifExists(ifExitsFun: T.() -> Unit): T? {
    this?.let {
        it.exists.ifTrue {
            it.ifExitsFun()
        }
    }
    return this
}

inline fun <T : Number> T?.ifNotExists(ifNotExitsFun: T?.() -> Unit): T? {
    if (!this.exists) {
        this.ifNotExitsFun()
    }
    return this
}

val Boolean.isVisibleOrGone: Int
    get() = if (this) View.VISIBLE else View.GONE

/**
 * Position of Enum (passes back the ordinal value)
 */
val Enum<*>.position: Int
    get() = this.ordinal

/**
 * Array to array List
 */
val <T> Array<T>.arrayList: ArrayList<T>
    get() = toCollection(ArrayList())

/**
 * Java Class of Object
 */
inline fun <reified T : Any> T.getClass() = T::class.java


/**
 * Attempt safeCast from a java Class
 * @param item - item to cast
 */
fun <T : Any> Class<T>.safeCast(item: Any?) = kotlin.safeCast(item)

/**
 * If Boolean is true then run function and return any value
 * @param ifTrue - function to run if Boolean is true
 * @return if [ifTrue] is ran then return that function's result, else return null
 */
inline fun <R : Any?> Boolean?.ifTrueReturn(ifTrueFUN: () -> R): R? {
    if (this == true) return ifTrueFUN() else return null
}

inline fun Boolean?.ifTrue(ifTrueFUN: () -> Unit): Boolean? {
    if (this == true) {
        ifTrueFUN()
    }
    return this
}

fun JSONObject.optStringIfNotNull(key: String, optValue: String? = null): String? {
    return optString(key, optValue)?.let {
        if (it.toLowerCase() == "null") null else it
    }
}

inline fun Boolean?.ifFalse(ifFalseFUN: () -> Unit): Boolean? {
    if (this == false) {
        ifFalseFUN()
    }
    return this
}

infix fun <T> T?.equals(item: T?): Boolean = this == item

infix fun Boolean.thenRun(callback: () -> Unit) {
    if (this) callback()
}

fun BufferedReader?.getString(): String? =
        this?.let {
            var inputStr: String? = ""
            fun readLine(): String? {
                inputStr = this.readLine()
                return inputStr
            }

            val stringBuilder = StringBuilder()
            while (readLine().isNotNull) {
                stringBuilder.append(inputStr)
            }
            return stringBuilder.toString()
        }

/**
 * If [this] is null then run [ifNull] function and return the function's result, else null
 * @param ifNull - function to run if [this] is null
 * @return if [ifNull] is ran then return that function's result, else return null
 */
inline fun <R : Any?> R.ifNull(ifNull: () -> Unit): R {
    if (this == null) ifNull()
    return this
}

inline fun <R : Any> R?.ifNullReturn(ifNullFun: () -> R): R {
    if (this != null) return this
    return ifNullFun()
}

inline fun <R : Any, S : Any?> R?.ifNotNullReturn(ifNotNullFun: (R) -> S): S? {
    if (this != null) return ifNotNullFun(this)
    return null
}

/**
 * if the [this] is null then create a default value **same as the elvis operator**
 * @param ifNullFun - the default value to return
 */
fun <R : Any> R?.defaultValue(ifNullFun: () -> R): R {
    if (this != null) return this
    return ifNullFun()
}

/**
 * set default value to -1
 */
val Int?.defaultValue: Int
    get() = defaultValue { -1 }

/**
 * set default value to -1f
 */
val Float?.defaultValue: Float
    get() = defaultValue { -1f }

/**
 * set default value to -1.0
 */
val Double?.defaultValue: Double
    get() = defaultValue { -1.0 }

/**
 * set default value to ""
 */
val String?.defaultValue: String
    get() = defaultValue { "" }

fun <T> Collection<T>.safeGet(index: Int?): T? = index?.let {
    if (index.exists && index < this.size) {
        iterateForEach { item, indexItem ->
            if (index == indexItem) return item
        }
    }
    return null
}

fun <T> Array<T>.safeGet(index: Int?): T? = index?.let {
    when (index.exists && index < this.size) {
        true -> this[index]
        else -> null
    }
}

/**
 * infix fun that returns the greater of two values
 */
infix fun <T> T.greater(value: T?): T {
    if (value == null || !(value is Comparable<*>)) return this
    if (this == null || !(this is Comparable<*>)) return value
    if ((this as? Comparable<T>)?.compareTo(value) ?: 1 > 0) return this else return value
}

/**
 * creates a new list of another type that is passed from [newListOfFun]
 * @param newListOfFun - a fun that takes each child of [this] list and returns a value
 */
fun <T, R> Iterable<T>.newListOf(newListOfFun: T.() -> R): List<R> {
    val list = arrayListOf<R>()
    forEach {
        list.add(it.newListOfFun())
    }
    return list
}

fun <T, R> Array<T>.newListOf(newListOfFun: T.() -> R): List<R> {
    val list = arrayListOf<R>()
    forEach {
        list.add(it.newListOfFun())
    }
    return list
}

/**
 * If Boolean is false then run function and return any value
 * @param ifFalse - function to run if Boolean is false
 * @return if [ifFalse] is ran then return that function's result, else return null
 */
inline fun <T : Any> T?.ifEquals(ifEqualsFun: T.() -> Boolean): IfEquals<T> {
    if (this != null) {
        if (this.ifEqualsFun()) return IfEquals(this)
    }
    return IfEquals(null)
}

class IfEquals<out T>(val item: T?) {
    fun ifTrue(callback: (T) -> Unit) {
        if (item != null) callback(item)
    }

    fun <R> ifTrueReturn(callback: (T) -> R): R? {
        if (item != null) return callback(item) else return null
    }
}

val Any?.elseFalse
    get() = this != null

inline fun <R : Any?> Boolean?.ifFalseReturn(ifFalse: () -> R): R? {
    if (this == false) return ifFalse() else return null
}

val Any?.isNotNull
    get() = this != null

val String?.isNotNullOrBlank
    get() = !isNullOrBlank()

inline fun <R : Any> R?.ifNotNull(ifNotNullFunc: (R) -> Unit): R? {
    if (this != null) ifNotNullFunc(this)
    return this
}

val Int.floatPercentange
    get() = (this / 100).toFloat()


/**
 * If Boolean is false then run function and return any value
 * @param ifFalse - function to run if Boolean is false
 * @return if [ifFalseReturn] is ran then return that function's result, else return null
 */
inline fun <R, T> T.safe(tryCatch: T.() -> R): R? {
    try {
        return tryCatch(this)
    } catch (e: Exception) {
        return null
    }
}

/**
 * semicolon delimited string
 * @param values - an array of objects that are passed in and delimited to the string
 * @return delimited string with the [values]
 */
fun semicolonDelimited(vararg values: Any?): String {
    val styleKey = StringBuilder()
    values.forEachIndexed { index, any ->
        styleKey.append(any)
        if (index < values.size - 1) styleKey.append(";")
    }
    return styleKey.toString()
}

/**
 *
 */
val String.addSemicolon: String
    get() = this + ";"

/**
 * Convert Function name, values, and parameters to formatted String
 * @param methodValues - since reflection cannot be used for actual values we pass the values and inser them in order
 */
fun KFunction<*>.logString(vararg methodValues: Any?): String {
    return StringBuilder().apply {
        append("[")
        append(name)
        append("]")
        parameters.forEachIndexed { index, kParameter ->
            append(" ")
            append(kParameter.name)
            append(":")
            (methodValues.size > index).ifTrueReturn {
                append(" ")
                append(methodValues[index])
                (parameters.size - 1 == index).ifFalseReturn {
                    append(",")
                }
            }
        }
    }.toString()
}

fun KFunction<*>.log(vararg methodValues: Any?) {
    Timber.d("%s", logString(*methodValues))
}

infix fun KProperty0<*>.log(string: String) {
    Timber.d("[$name] $string")
}

fun KProperty0<*>.log() {
    Timber.d("[$name]")
}

/**
 * create a javascript string and make sure it does not start with javascript:
 * @param callingMethod - the values in the function
 * @param methodValues - the values of the [callingMethod]
 * @param stringBuilderFunction - the string to inject into the javscript return function
 * @return the javascript function as a string
 */
fun createJavascript(callingMethod: KFunction<*>, vararg methodValues: Any?, stringBuilderFunction: () -> String): String {
    //  callingMethod.log(*methodValues)

    val script = stringBuilderFunction();
    //Make sure does not start with javascript: {legacy}
    val scriptString = script.startsWith("javascript:").ifTrueReturn {
        script.substring("javascript:".length)
    } ?: script

    Timber.v("[%s] %s", callingMethod.name, scriptString)

    return scriptString
}

/**
 * create a javascript function that returns a result
 * @param callingMethod - the values in the function
 * @param methodValues - the values of the [callingMethod]
 * @param stringBuilderFunction - the string to inject into the javscript return function
 * @return the javascript function as a string
 */
fun createJavascriptFunction(callingMethod: KFunction<*>, vararg methodValues: Any?, stringBuilderFunction: () -> String): String {
    return String.format("(function(){ return %s;})();", createJavascript(callingMethod, *methodValues, stringBuilderFunction = stringBuilderFunction))
}

/**
 * create a javascript function that returns a result
 * @param callingMethod - the values in the function
 * @param methodValues - the values of the [callingMethod]
 * @param priorToReturn - javascript string to inject before the return logic
 * @param stringBuilderFunction - the string to inject into the javscript return function
 * @return the javascript function as a string
 */
fun createJavascriptFunctionWithLogic(callingMethod: KFunction<*>, methodValues: Array<out Any?>, priorToReturn: String = "", stringBuilderFunction: () -> String): String {
    return String.format("(function(){ %s return %s;})();", priorToReturn, createJavascript(callingMethod, *methodValues, stringBuilderFunction = stringBuilderFunction))
}

/**
 * create a javascript function and inject it into the webView
 * @param callingMethod - the values in the function
 * @param methodValues - the values of the [callingMethod]
 * @param stringBuilderFunction - the string to inject into the javscript return function
 */
fun WebView.evaluateJavascript(callingMethod: KFunction<*>, vararg methodValues: Any?, stringBuilderFunction: () -> String) {
    evaluateJavascriptCallback(callingMethod, methodValues, stringBuilderFunction, { })
}

/**
 * create a javascript function and inject it into the webView
 * @param callingMethod - the values in the function
 * @param methodValues - the values of the [callingMethod]
 * @param stringBuilderFunction - the string to inject into the javscript return function
 * @param callback - the result of the function you called in javascript
 */
fun WebView.evaluateJavascriptCallback(callingMethod: KFunction<*>, methodValues: Array<out Any?>, stringBuilderFunction: () -> String, callback: (String) -> Unit) {
    evaluateJavascript(createJavascript(callingMethod, *methodValues, stringBuilderFunction = stringBuilderFunction), callback)
}

val String?.toJSONObjectSafe: JSONObject
    get() {
        try {
            return JSONObject(this)
        } catch (e: Exception) {

        }
        return JSONObject()
    }

val String?.toJSONObject: JSONObjectResult
    get() {
        try {
            return JSONObjectResult(JSONObject(this))
        } catch (e: JSONException) {
            return JSONObjectResult((e))

        }
    }

val String?.toJSONArray: JSONArrayResult
    get() {
        try {
            return JSONArrayResult((JSONArray(this)))
        } catch (e: JSONException) {
            return JSONArrayResult((e))

        }
    }

val String?.toJSONArraySafe: JSONArray
    get() {
        try {
            return JSONArray(this)
        } catch (e: Exception) {

        }
        return JSONArray()
    }

fun <T> List<T>.toLinkedList(): LinkedList<T> {
    val list = LinkedList<T>()
    list.addAll(this)
    return list
}

class TimberKTString(val string: String) {

}

class TimberKTException(val exception: Exception) {

}

val String.timber
    get() = TimberKTString(this)

val TimberKTString.e
    get() = Timber.e(this.string)

val TimberKTString.d
    get() = Timber.d(this.string)

val Exception.timber
    get() = TimberKTException(this)

infix fun TimberKTException.e(string: String) = Timber.e(this.exception, string)

infix fun TimberKTException.d(string: String) = Timber.d(this.exception, string)

fun Timberd(string: String, e: Exception) = Timber.d(e, string)

fun Timbere(string: String, e: Exception) = Timber.e(e, string)

val Runnable.lambda: () -> Unit
    get() = {
        this.run()
    }

val (() -> Unit).runnable: Runnable
    get() = Runnable { this() }


data class GenericHolderOne<T>(val item: T)

data class GenericHolderTwo<T, R>(val item: T, val secondItem: R)

fun FragmentTransaction.replaceCustom(@IdRes id: Int, fragment: Fragment) = replace(id, fragment, fragment::class.java.canonicalName)

inline fun <reified T : Parcelable> Intent.getParcelableExtra() = getParcelableExtra<T>(T::class.java.canonicalName)

inline fun <reified T : Parcelable> Intent.putParcelableExtra(item: T, name: String = T::class.java.canonicalName) = putExtra(name, item)

inline fun <reified T : Parcelable> Intent.putExtra(item: T, name: String = T::class.java.canonicalName) = putExtra(name, item)

inline fun <reified T : Serializable> Intent.getSerializableExtra() = getSerializableExtra(T::class.java.canonicalName) as? T

inline fun <reified T : Serializable> Intent.putSerializableExtra(item: T, name: String = T::class.java.canonicalName) = putExtra(name, item)

inline fun <reified T : Serializable> Intent.putExtra(item: T, name: String = T::class.java.canonicalName) = putExtra(name, item)

fun <T> Iterable<T>.asyncForEach(callback: (item: T, index: Int, forEachItems: (ForEachItem) -> Unit) -> Unit, onComplete: () -> Unit) {

    val iterator = this.iterator()

    var counter = 0

    fun getCounter(): Int = counter.apply { counter++ }

    fun attempt() {
        if (iterator.hasNext()) {
            callback(iterator.next(), getCounter()) {
                when (it) {
                    ForEachItem.MOVE_TO_NEXT -> attempt()
                    else -> {
                    }
                }
            }
        } else onComplete()
    }

    attempt()
}

fun <T> Iterable<T>.contains(ifTrue: (T) -> Boolean) = firstOrNull { ifTrue(it) }.isNotNull

fun (() -> Any?).run() = this()

inline fun <T> Iterable<T>.iterateForEach(callback: (item: T, index: Int) -> Unit) {
    val iterate = this.iterator()
    while (iterate.hasNext()) {
        val item = iterate.next()
        callback(item, this.indexOf(item))
    }
}

fun Float.toIntTimesHundred() = (this * 100f).roundToInt()

fun Int.toFloatDivideHundred() = (this.toFloat() / 100f)

fun Float.toIntHex() = (this * 255f).toInt()

fun Int.toFloatHex() = (this.toFloat() / 255f)

enum class ForEachItem {
    MOVE_TO_NEXT,
    DONE
}


fun Any.invokeFirstMethod(vararg itemToInvoke: Any) {
    try {
        fun match(item: Method): Boolean {
            if (item.genericParameterTypes.size != itemToInvoke.size) return false
            item.genericParameterTypes.forEachIndexed { index, type -> if (type != itemToInvoke.get(index)::class.java) return false }
            return true
        }

        val found = this::class.java.declaredMethods.firstOrNull { item ->
            //Check only has one parameter and parameter type is [viewModel] type
            match(item)

        }
        //invoke the setViewModel method if found
        found?.invoke(this, *itemToInvoke).apply {
            //  viewModel.lifecycleOwner = this@BaseActivity
        }
        //  bindingSucceededForViewModel = true
    } catch (e: Exception) {
        Timber.d(e)
    }
}

fun Any.invokeFirstMember(itemToInvoke: Any) = this::class.memberProperties
        .filter { it.visibility == KVisibility.PUBLIC }
        // We only want strings
        .filter { it.returnType.isSubtypeOf(itemToInvoke::class.starProjectedType) }
        .filterIsInstance<KMutableProperty<*>>()
        .forEach { prop ->
            // Instead of printing the property we set it to some value
            prop.setter.call(this, itemToInvoke)
        }


interface BaseItemsInterface<T> {
    fun getCount(): Int

    fun getItemType(i: Int): T?

    fun getItem(i: Int): Any? = getItemType(i)

    fun getPosition(item: T): Int

    fun getItemId(i: Int): Long
}

interface BaseListInterface<T> : BaseItemsInterface<T> {
    val list: Collection<T>

    override fun getCount(): Int = list.size

    override fun getItemType(i: Int): T? = list.safeGet(i)

    override fun getPosition(item: T): Int = list.indexOfFirst { it == item }

    override fun getItemId(i: Int): Long = 0
}

interface BaseArrayInterface<T> : BaseItemsInterface<T> {
    val list: Array<T>

    override fun getCount(): Int = list.size

    override fun getItemType(i: Int): T? = list.safeGet(i)

    override fun getPosition(item: T): Int = list.indexOfFirst { it == item }

    override fun getItemId(i: Int): Long = 0
}

abstract class BaseArrayAdapter<T> : BaseAdapter(), BaseArrayInterface<T> {

}

fun <T : TextView> T.onTextChanged(changed: (CharSequence?) -> Unit): T {
    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            changed(p0)
        }
    })
    return this
}

fun <T : TextView> T.onTextChanged(observe: Observe<String>): T {
    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            observe.item = text.toString()
        }
    })
    return this
}

interface ContextReferenceInterface {
    val contextReference: Context?
}

interface ContextNotNullInterface : ContextReferenceInterface {
    override val contextReference: Context
}
