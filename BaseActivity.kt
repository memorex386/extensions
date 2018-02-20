package com.rethinkbooks.main

import android.app.Application
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.rethinkbooks.datakit.R
import com.rethinkbooks.observables.ActionItem
import com.rethinkbooks.utils.*
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.lang.reflect.Type

interface ReferenceActivityInterface<out ACTIVITY : BaseLifecycleActivity> : ContextReferenceInterface, BaseLifecycleActivityInterface {
    val activity: ACTIVITY
        get() = contextReference as ACTIVITY

    override val lifecycleActivity: BaseLifecycleActivity
        get() = activity

}

interface DefaultActivityInterface<out ACTIVITY : BaseLifecycleActivity> : CompositeDisposableInterface, ReferenceActivityInterface<ACTIVITY> {

    override val compositeDisposable: CompositeDisposable
        get() = activity.compositeDisposable

    fun <T> ActionItem<T>.observe(observer: (T) -> Unit) {
        this.observe(activity, observer)
    }
}

interface BaseActivityInterface : DefaultActivityInterface<BaseActivity<*, *>> {

}

interface BaseLifecycleActivityInterface {

    val lifecycleActivity: BaseLifecycleActivity

    val Class<out BaseLifecycleActivity>.intent: Intent
        get() {
            return Intent(lifecycleActivity, this).apply { putExtra(BaseLifecycleActivity.NAVIGATE_TO, this@intent.canonicalName) }
        }

    fun Intent.startActivityResult(resultCode: Int = BaseLifecycleActivity.DEFAULT_RESULT_CODE) {
        lifecycleActivity.startActivityForResult(this, resultCode)
    }
}

/**
 * Created by bradthome on 12/1/17.
 */
abstract class BaseLifecycleActivity : AppCompatActivity(), CompositeDisposableInterface, ContextNotNullInterface, BaseLifecycleActivityInterface {

    override val lifecycleActivity: BaseLifecycleActivity
        get() = this
    //RxJava Disposable for lifecycle events

    override val contextReference: Context
        get() = this

    val context: Context
        get() = this

    override val compositeDisposable = CompositeDisposable()

    init {
        lifecycle.addObserver(ObserveLifecycle())
    }

    inner class ObserveLifecycle : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fun onCreate() {

        }

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

    fun <T> ActionItem<T>.observe(callback: (T) -> Unit) {
        observe(this@BaseLifecycleActivity, callback)
    }

    inline fun <reified T : Type> T.ifFragmentLoaded(callback: (T) -> Unit = {}): Boolean {
        supportFragmentManager.findFragmentByTag(this::class.java.canonicalName)?.apply {
            if (this is T) {
                callback(this)
                return true
            }
        }
        return false
    }

    fun Fragment.defaultReplace(containerFragment: Int, dontCommit: Boolean = false): FragmentTransaction {
        return supportFragmentManager.beginTransaction().replaceCustom(containerFragment, this).apply {
            if (!dontCommit) commit()
        }
    }

    val ShowHide.systemUiVisibility: Int
        get() {
            fun hide() = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or // hide nav bar  or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or // hide status bar
                    View.SYSTEM_UI_FLAG_IMMERSIVE)
            return when (this) {
                is ShowHide.VISIBLE -> (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                is ShowHide.GONE -> hide()
                is ShowHide.INVISIBLE -> hide()
            }
        }

    companion object {
        val DEFAULT_RESULT_CODE = 5678
        val NAVIGATE_TO = "NAVIGATE_TO"
    }
}

abstract class BaseViewModelActivity<T : BaseViewModel> : BaseLifecycleActivity() {

    protected val viewModel by lazy { ViewModelProviders.of(this).get(viewModelClass) }

    abstract protected val viewModelClass: Class<T>

    protected fun <T : BaseViewModel> getViewModel(clazz: Class<T>) = ViewModelProviders.of(this).get(clazz)

}

abstract class BaseActivity<ViewModel : BaseViewModel, DataBinding : ViewDataBinding> : BaseViewModelActivity<ViewModel>() {

    lateinit var binding: DataBinding
        private set


    override final fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preDataBinded(savedInstanceState)
        binding = DataBindingUtil.setContentView<DataBinding>(this, layoutRes)
        try {
            val found = binding::class.java.declaredMethods.firstOrNull { item ->
                //Check only has one parameter and parameter type is [viewModel] type
                (item.genericParameterTypes.size == 1 &&
                        item.genericParameterTypes[0] == viewModel::class.java)

            }
            //invoke the setViewModel method if found
            found?.invoke(binding, viewModel).apply {
                viewModel.lifecycleOwner = this@BaseActivity
            }
            bindingSucceededForViewModel = true
        } catch (e: Exception) {
            Timber.d(e)
        }

        onCreateDataBinded(savedInstanceState)
    }

    /**
     * once the data has been binded and the view inflated then this is called.  All data binding operations and [onCreate] logic should be ran here
     */
    open fun preDataBinded(savedInstanceState: Bundle?) {

    }

    abstract fun onCreateDataBinded(savedInstanceState: Bundle?)

    abstract protected val layoutRes: Int

    protected var bindingSucceededForViewModel = false
        private set

    fun <T> (() -> T).asyncResult(asyncType: AsyncType = AsyncType.IO_MAIN) = this.asyncResult(asyncType, compositeDisposable)
}

abstract class BaseDefaultActivity<T : BaseViewModel> : BaseViewModelActivity<T>() {

    open val baseActivityLayout = BaseActivityLayout.DEFAULT

    override final fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(baseActivityLayout.layoutRes)
        viewCreated(savedInstanceState)
    }

    abstract protected fun viewCreated(savedInstanceState: Bundle?)


}

abstract class BaseDefaultFragmentActivity : BaseDefaultActivity<BaseDefaultViewModel>() {

    override final val viewModelClass: Class<BaseDefaultViewModel>
        get() = BaseDefaultViewModel::class.java

    override fun viewCreated(savedInstanceState: Bundle?) {
        createDefaultFragment().let {
            supportFragmentManager.beginTransaction().add(baseActivityLayout.fragmentContainerId, it.fragment, it.tag).commit()
            fragmentAdded(it)
        }
    }

    abstract protected fun fragmentAdded(fragmentInfo: FragmentInfo)

    abstract protected fun createDefaultFragment(): FragmentInfo

    protected inner class FragmentInfo(val fragment: Fragment, val tag: String = fragment::class.java.canonicalName)

    protected fun Fragment.fragmentInfo(tag: String = this::class.java.canonicalName): FragmentInfo =
            FragmentInfo(this, tag)
}

interface DefaultActivity {
    val layoutRes: Int
}

interface DefaultFragmentActivity : DefaultActivity {
    val fragmentContainerId: Int
}

enum class BaseActivityLayout(override val layoutRes: Int, override val fragmentContainerId: Int) : DefaultFragmentActivity {
    DEFAULT(R.layout.activity_default_base, R.id.container),
    NO_TOOLBAR(R.layout.activity_no_toolbar, R.id.container)
}

abstract class BaseDefaultViewModel(app: Application) : BaseViewModel(app) {

}

