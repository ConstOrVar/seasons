package ru.constorvar.timewaster

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import ru.constorvar.timewaster.databinding.FragmentTimeWastingBinding
import java.util.*
import java.util.concurrent.TimeUnit

private const val TIME_KEY = "time_spent"
private const val COLOR_KEY = "color"

@Suppress("unused")
class TimeWastingFragment : Fragment() {
    private val rand = Random()

    private val timeSpent = MutableLiveData<Long>()
    private val color = MutableLiveData<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        timeSpent.value = savedInstanceState?.getLong(TIME_KEY) ?: 0
        color.value = savedInstanceState?.getInt(COLOR_KEY) ?: generateRandomColor()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentTimeWastingBinding.inflate(inflater, container, false)

        setup(binding)

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putLong(TIME_KEY, timeSpent.value!!)
        outState.putInt(COLOR_KEY, color.value!!)
    }

    private fun setup(binding: FragmentTimeWastingBinding) {
        viewLifecycleOwner.configure {
            bindState(timeSpent, binding.txtTime) { txtView, time ->
                txtView.text = "$time"
            }

            bindState(color, binding.root) { view, color ->
                view.setBackgroundColor(color)
            }

            bindObservable(
                observable = Observable.interval(1, TimeUnit.SECONDS),
                action = { source ->
                    source
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            {
                                timeSpent.value = timeSpent.value!! + 1
                            },
                            Throwable::printStackTrace
                        )
                },
                bindOnEvent = Lifecycle.Event.ON_START
            )

            val colorChangeStream = PublishSubject.create<Unit>()

            bindClicks(
                view = binding.root,
                clickListener = {
                    color.value = generateRandomColor()
                    colorChangeStream.onNext(Unit)
                }
            )

            bindObservable(
                observable = colorChangeStream
                    .startWith(Unit)
                    .switchMap { Observable.interval(5,5, TimeUnit.SECONDS) },
                action = { source ->
                    source
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            {
                                color.value = generateRandomColor()
                            },
                            Throwable::printStackTrace
                        )
                },
                bindOnEvent = Lifecycle.Event.ON_RESUME
            )
        }
    }

    private fun generateRandomColor(): Int {
        return Color.argb(255, rand.nextInt(256), rand.nextInt(256), rand.nextInt(256))
    }

}

fun <T> Configurator.Builder.bindObservable(
    observable: Observable<T>,
    action: (Observable<T>) -> Disposable,
    bindOnEvent: Lifecycle.Event = Lifecycle.Event.ON_CREATE,
    unbindOnEvent: Lifecycle.Event = bindOnEvent.oppositeEvent()
) {
    bind(
        target = observable to action,
        bindAction = { _, (stream, act) ->
            act(stream)
        },
        bindOnEvent = bindOnEvent,
        unbindAction = { _, _, subscription ->
            subscription.dispose()
        },
        unbindOnEvent
    )
}

fun Configurator.Builder.bindClicks(
    view: View,
    clickListener: View.OnClickListener,
    bindOnEvent: Lifecycle.Event = Lifecycle.Event.ON_CREATE,
    unbindOnEvent: Lifecycle.Event = bindOnEvent.oppositeEvent()
) {
    bind(
        target = clickListener to view,
        bindAction = { _, (listener, v) ->
            v.setOnClickListener(listener)
        },
        bindOnEvent = bindOnEvent,
        unbindAction = { _, (_, v), _ ->
            v.setOnClickListener(null)
        },
        unbindOnEvent
    )
}