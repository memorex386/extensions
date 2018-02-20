package com.rethinkbooks.utils


import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.databinding.BindingAdapter
import android.databinding.BindingConversion
import android.databinding.InverseBindingAdapter
import android.databinding.InverseBindingListener
import android.os.Build
import android.support.design.widget.TabLayout
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.widget.*
import android.support.v7.widget.SearchView
import android.text.Html
import android.view.View
import android.view.animation.Animation
import android.webkit.WebView
import android.widget.*
import com.squareup.picasso.Picasso

object CoreBindings

/**
 * Created by bradley.thome on 10/18/17.
 */

@BindingConversion
fun convertBooleanToVisibility(visible: Boolean): Int {
    return if (visible) View.VISIBLE else View.GONE
}

@BindingAdapter("invisible")
fun setViewModel(view: View, boolean: Boolean) {
    if (boolean) view.visibility = View.VISIBLE
    else view.visibility = View.INVISIBLE
}

@BindingAdapter("android:visibility")
fun setViewModel(view: View, string: String?) {
    if (string.isNullOrBlank()) view.visibility = View.GONE
    else view.visibility = View.VISIBLE
}

@BindingAdapter("text")
fun setText(textView: TextView, int: Int?) {
    textView.setText(int.toString())
}

@SuppressLint("SetJavaScriptEnabled")
@BindingAdapter("url")
fun setUrl(webView: WebView, url: String?) {
    webView.settings.javaScriptEnabled = true
    webView.loadUrl(url)
}

@BindingAdapter("imageUrl")
fun loadImage(imageView: ImageView, url: String?) {
    Picasso.with(imageView.context).load(url).into(imageView)
}

@BindingAdapter("html")
fun setHtmlText(textView: TextView, htmlText: String?) {
    if (!htmlText.isNullOrBlank()) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        textView.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT))
    } else {
        textView.setText(Html.fromHtml(htmlText))
    }
    else textView.setText(null)
}

@BindingAdapter("adapter")
fun loadImage(viewPager: ViewPager, fragmentStatePagerAdapter: FragmentStatePagerAdapter) {
    viewPager.adapter = fragmentStatePagerAdapter
}

@BindingAdapter("adapter")
fun loadAdapter(recyclerView: RecyclerView, adapter: RecyclerView.Adapter<*>) {
    val layoutManager = LinearLayoutManager(recyclerView.context, LinearLayout.VERTICAL, false)
    val defaultItemAnimator = DefaultItemAnimator()
    recyclerView.layoutManager = layoutManager
    recyclerView.itemAnimator = defaultItemAnimator
    recyclerView.adapter = adapter
}

@BindingAdapter("animations")
fun setAnimations(view: View, animation: Animation) {
    view.startAnimation(animation)
}

@BindingAdapter("viewPager")
fun setviewPager(tabLayout: TabLayout, viewPager: ViewPager) {
    tabLayout.setupWithViewPager(viewPager)
}

@BindingAdapter(value = "textAttrChanged")
fun setListener(searchView: SearchView, listener: InverseBindingListener?) {
    listener?.let {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                listener.onChange()
                return true
            }
        })
    }
}

@BindingAdapter("text")
fun setText(searchView: SearchView, value: String) {
    if (searchView.query != value) searchView.setQuery(value, false)
}

@InverseBindingAdapter(attribute = "text")
fun getText(searchView: SearchView): String = searchView.query.toString()

@BindingAdapter(value = *arrayOf("selectedValue", "selectedValueAttrChanged"), requireAll = false)
fun <T> bindSpinnerData(pAppCompatSpinner: AppCompatSpinner, newSelectedValue: T?, newTextAttrChanged: InverseBindingListener) {
    pAppCompatSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
            newTextAttrChanged.onChange()
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}
    }
    if (newSelectedValue != null) {
        val pos = (pAppCompatSpinner.adapter as BaseItemsInterface<T>).getPosition(newSelectedValue)
        pAppCompatSpinner.setSelection(pos, true)
    }
}

@InverseBindingAdapter(attribute = "selectedValue", event = "selectedValueAttrChanged")
fun <T> captureSelectedValue(pAppCompatSpinner: AppCompatSpinner): T {
    return pAppCompatSpinner.selectedItem as T
}

@BindingAdapter(value = *arrayOf("currentValue", "currentValueAttrChanged"), requireAll = false)
fun setCurrentValue(seekBar: SeekBar, newSelectedValue: Int?, currentValueAttrChanged: InverseBindingListener) {
    seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            currentValueAttrChanged.onChange()
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {

        }

        override fun onStopTrackingTouch(p0: SeekBar?) {

        }
    })
    if (newSelectedValue != null) seekBar.progress = newSelectedValue
}

@InverseBindingAdapter(attribute = "currentValue", event = "currentValueAttrChanged")
fun captureSelectedValue(seekBar: SeekBar): Int {
    return seekBar.progress
}

@BindingAdapter("android:enabled")
fun viewEnabled(view: View, isEnabled: Boolean) {
    view.isEnabled = isEnabled
}

@BindingAdapter("android:disabled")
fun viewDisabled(view: View, isDisabled: Boolean) {
    view.isEnabled = !isDisabled
}

@BindingAdapter("android:invisibleOrVisible")
fun visibilitySetter(view: View, isVisible: Boolean?) {
    isVisible ?: return
    view.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("animateBackground")
fun animateBackground(view: View, animateBackground: AnimateBackground) {
    val animation = ValueAnimator().apply {
        duration = animateBackground.duration
        addUpdateListener {
            val item = it.animatedValue

        }
    }
}

@BindingAdapter("doIf")
fun doIf(view: View, doIf: DoIf<View>) {
    doIf.attempt(view)
}

interface DoIf<in T> {
    fun completeIf(item: T): Boolean
    fun ranIfCompleteIsTrue(item: T)
    fun attempt(item: T) {
        if (completeIf(item)) ranIfCompleteIsTrue(item)
    }
}

class AnimateBackground(val color: Int, val duration: Long = 0)

