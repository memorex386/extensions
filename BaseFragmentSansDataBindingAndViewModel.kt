package com.rethinkbooks.main

import android.arch.lifecycle.*
import android.content.Context
import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.rethinkbooks.observables.ActionItem
import com.rethinkbooks.utils.CompositeDisposableInterface
import com.rethinkbooks.utils.ContextReferenceInterface
import com.rethinkbooks.utils.ifFalse
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

/**
 * Created by bradthome on 12/1/17.
 */
abstract class BaseFragmentSansDataBindingAndViewModel : Fragment(), LifecycleOwner, CompositeDisposableInterface, ContextReferenceInterface {

    override val contextReference: Context?
        get() = context

    override val compositeDisposable = CompositeDisposable()

    init {
        lifecycle.addObserver(ObserveLifecycle())
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        (context is BaseLifecycleActivity).ifFalse { throw RuntimeException("Activity must be " + BaseLifecycleActivity::class.java.simpleName) }
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


    val supportFragmentManager
        get() = (context as BaseLifecycleActivity).supportFragmentManager

    infix fun <T> ActionItem<T>.attach(liveDataItemAction: ActionItem<T>) {
        observe(this@BaseFragmentSansDataBindingAndViewModel) {
            liveDataItemAction.actionOccurred(it)
        }
    }

    fun <T> ActionItem<T>.observe(callback: (T) -> Unit) {
        observe(this@BaseFragmentSansDataBindingAndViewModel, callback)
    }


}

abstract class BaseViewModelFragment<T : BaseViewModel> : BaseFragmentSansDataBindingAndViewModel() {

    protected val viewModel by lazy { ViewModelProviders.of(this).get(viewModelClass) }

    abstract protected val viewModelClass: Class<T>

    protected fun <T : BaseViewModel> getViewModel(clazz: Class<T>) = ViewModelProviders.of(this).get(clazz)

}

abstract class BaseFragment<VIEW_MODEL : BaseViewModel, DATA_BINDING : ViewDataBinding> : BaseViewModelFragment<VIEW_MODEL>() {

    lateinit var binding: DATA_BINDING
        private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate<DATA_BINDING>(inflater, layoutRes, container, false)
        try {
            val found = binding::class.java.declaredMethods.firstOrNull { item ->
                //Check only has one parameter and parameter type is [viewModel] type
                (item.genericParameterTypes.size == 1 &&
                        item.genericParameterTypes[0] == viewModel::class.java)

            }
            //invoke the setViewModel method if found
            found?.invoke(binding, viewModel).apply {
                try {
                    viewModel.lifecycleOwner = this@BaseFragment
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }
            bindingSucceededForViewModel = true
        } catch (e: Exception) {
            Timber.d(e)
        }

        onCreateDataBinded(savedInstanceState)
        return binding.root
    }

    /**
     * once the data has been binded and the view inflated then this is called.  All data binding operations and [onCreateView] logic should be ran here
     */
    abstract fun onCreateDataBinded(savedInstanceState: Bundle?)

    abstract protected val layoutRes: Int

    protected var bindingSucceededForViewModel = false
}

