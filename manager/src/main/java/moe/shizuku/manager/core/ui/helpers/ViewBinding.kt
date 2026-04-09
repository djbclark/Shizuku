package moe.shizuku.manager.core.ui.helpers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class FragmentViewBindingDelegate<T : ViewBinding>(
    private val viewBindingFactory: (Fragment) -> T
) : ReadOnlyProperty<Fragment, T> {
    private var binding: T? = null

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        binding?.let { return it }

        thisRef.parentFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
                    if (f === thisRef) {
                        binding = null
                        fm.unregisterFragmentLifecycleCallbacks(this)
                    }
                }
            }, false
        )

        return viewBindingFactory(thisRef).also { binding = it }
    }
}

@Suppress("UnusedReceiverParameter")
@JvmName("fragmentViewBindingBind")
fun <T : ViewBinding> Fragment.viewBinding(factory: (View) -> T): FragmentViewBindingDelegate<T> =
    FragmentViewBindingDelegate { it.requireView().let(factory) }

@Suppress("UnusedReceiverParameter")
@JvmName("fragmentViewBindingInflate")
fun <T : ViewBinding> Fragment.viewBinding(
    factory: (LayoutInflater) -> T
): FragmentViewBindingDelegate<T> = FragmentViewBindingDelegate { it.layoutInflater.let(factory) }

inline fun <T : ViewBinding> AppCompatActivity.viewBinding(
    crossinline factory: (LayoutInflater) -> T
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
    factory(layoutInflater)
}

fun <T : ViewBinding> RecyclerView.ViewHolder.viewBinding(factory: (View) -> T): Lazy<T> =
    lazy(LazyThreadSafetyMode.NONE) { factory(itemView) }

inline fun <T : ViewBinding> ViewGroup.viewBinding(
    crossinline factory: (LayoutInflater, ViewGroup) -> T,
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
    factory(LayoutInflater.from(context), this)
}

inline fun <T : ViewBinding> ViewGroup.viewBinding(
    crossinline factory: (LayoutInflater, ViewGroup, Boolean) -> T,
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
    factory(LayoutInflater.from(context), this, true)
}
