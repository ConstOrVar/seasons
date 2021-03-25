package ru.constorvar.timewaster

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData

typealias Operation = (LifecycleOwner) -> Unit

/**
 * Класс, ответственный за выполнение переданных действий в нужные моменты жизненного цикла компонента.
 */
class Configurator private constructor(
    private val operationsWhenCreated: MutableList<Operation>,
    private val operationsWhenStarted: MutableList<Operation>,
    private val operationsWhenResumed: MutableList<Operation>,
    private val operationsWhenPaused: MutableList<Operation>,
    private val operationsWhenStopped: MutableList<Operation>,
    private val operationsWhenDestroyed: MutableList<Operation>
) {

    /**
     * Метод позволяет привязаться к жизненному циклу для дальгнейшего функционирования.
     *
     * @param lifecycleOwner
     */
    fun manageBy(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {

            override fun onCreate(owner: LifecycleOwner) {
                operationsWhenCreated.forEach { it(owner) }
            }

            override fun onStart(owner: LifecycleOwner) {
                operationsWhenStarted.forEach { it(owner) }
            }

            override fun onResume(owner: LifecycleOwner) {
                operationsWhenResumed.forEach { it(owner) }
            }

            override fun onPause(owner: LifecycleOwner) {
                operationsWhenPaused.forEach { it(owner) }
            }

            override fun onStop(owner: LifecycleOwner) {
                operationsWhenStopped.forEach { it(owner) }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                operationsWhenDestroyed.forEach { it(owner) }

                cleanUp()

                owner.lifecycle.removeObserver(this)
            }
        })
    }

    private fun cleanUp() {
        operationsWhenCreated.clear()
        operationsWhenStarted.clear()
        operationsWhenResumed.clear()
        operationsWhenPaused.clear()
        operationsWhenStopped.clear()
        operationsWhenDestroyed.clear()
    }

    // region Builder
    class Builder {
        private val operationsWhenCreated: MutableList<Operation> = mutableListOf()
        private val operationsWhenStarted: MutableList<Operation> = mutableListOf()
        private val operationsWhenResumed: MutableList<Operation> = mutableListOf()
        private val operationsWhenPaused: MutableList<Operation> = mutableListOf()
        private val operationsWhenStopped: MutableList<Operation> = mutableListOf()
        private val operationsWhenDestroyed: MutableList<Operation> = mutableListOf()

        /**
         * Метод для связки операции с событием жизненного цикла.
         *
         * @param triggerOnEvent событие жизненного цикла.
         * @param operation действие.
         *
         */
        fun addOperation(triggerOnEvent: Lifecycle.Event, operation: Operation) {
            when(triggerOnEvent) {
                Lifecycle.Event.ON_CREATE -> {
                    operationsWhenCreated.add(operation)
                }
                Lifecycle.Event.ON_START -> {
                    operationsWhenStarted.add(operation)
                }
                Lifecycle.Event.ON_RESUME -> {
                    operationsWhenResumed.add(operation)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    operationsWhenPaused.add(operation)
                }
                Lifecycle.Event.ON_STOP -> {
                    operationsWhenStopped.add(operation)
                }
                Lifecycle.Event.ON_DESTROY -> {
                    operationsWhenDestroyed.add(operation)
                }
                Lifecycle.Event.ON_ANY -> {
                    operationsWhenCreated.add(operation)
                    operationsWhenStarted.add(operation)
                    operationsWhenResumed.add(operation)
                    operationsWhenPaused.add(operation)
                    operationsWhenStopped.add(operation)
                    operationsWhenDestroyed.add(operation)
                }
            }
        }

        /**
         * Метод для создания конфигуратора.
         */
        fun build(): Configurator {
            return Configurator(
                operationsWhenCreated.toMutableList(),
                operationsWhenStarted.toMutableList(),
                operationsWhenResumed.toMutableList(),
                operationsWhenPaused.toMutableList(),
                operationsWhenStopped.toMutableList(),
                operationsWhenDestroyed.toMutableList()
            )
        }

    }
    // endregion

}

// region extension API
inline fun LifecycleOwner.configure(block: Configurator.Builder.() -> Unit) {
    Configurator.Builder()
        .apply(block)
        .build()
        .manageBy(this)
}

/**
 * Метод для установки действий по конфигурированию и сбросу
 *
 * @param target целевой элемент.
 * @param bindAction действие по настройке целевого элемента. Возвращает результат, который в последующем можно будет получить для корректного выполнения сброса.
 * @param bindOnEvent событие жизненного цикла, которое спровоцирует настройку элемента.
 * @param unbindAction действие по сбросу настройки.
 * @param unbindOnEvent событие жизненного цикла, которое спровоцирует сброс настройки элемента.
 */
inline fun <T, R: Any?> Configurator.Builder.bind(
    target: T,
    crossinline bindAction: (LifecycleOwner, T) -> R,
    bindOnEvent: Lifecycle.Event,
    crossinline unbindAction: (LifecycleOwner, T, R) -> Unit,
    unbindOnEvent: Lifecycle.Event = bindOnEvent.oppositeEvent()
) {
    var result: R? = null
    addOperation(bindOnEvent) { result = bindAction(it, target) }
    addOperation(unbindOnEvent) { unbindAction(it, target, result!!) }
}

/**
 * Метод для конфигурирования связки состояния с целевым элементом.
 *
 * @param state состояние.
 * @param target целевой элемент.
 * @param bind действие по применению состояния к элементу.
 */
fun <D: Any?, V> Configurator.Builder.bindState(
    state: LiveData<D>,
    target: V,
    bind: (V, D) -> Unit
) {
    addOperation(Lifecycle.Event.ON_CREATE) { lifecycleOwner ->
        state.observe(lifecycleOwner) { bind(target, it) }
    }
}

/**
 * Метод для получения противоположного (диаметрально противоположного) события жизнненого цикла.
 * Не будет корректно отрабатывать в случае вызова на [Lifecycle.Event.ON_ANY].
 */
fun Lifecycle.Event.oppositeEvent(): Lifecycle.Event = when(this) {
    Lifecycle.Event.ON_CREATE -> Lifecycle.Event.ON_DESTROY
    Lifecycle.Event.ON_START -> Lifecycle.Event.ON_STOP
    Lifecycle.Event.ON_RESUME -> Lifecycle.Event.ON_PAUSE
    else -> throw IllegalArgumentException("Specify lifecycle event explicitly")
}
// endregion