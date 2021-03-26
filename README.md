# Timewaster
Application is used to demonstrate how we can easily setup components without explicitly overriding methods like *onStart()*/*onResume()*/*onPause()*/*onStop()*/*onDestroy()*. 
That approach allows to write all configuration code in one place and reduce amount of intermediate variables.
 
## Example
```kotlin
viewLifecycleOwner.configure {  
	bindState(liveData, txtView) { txtView, state ->  
	  // TODO
	}

	bindClicks(  
		view = root,  
		clickListener = {  
		  // TODO
		}  
	)

	bindObservable(  
		observable = Observable.interval(1, TimeUnit.SECONDS),  
		action = { source ->  
			source  
				.observeOn(AndroidSchedulers.mainThread())  
				.subscribe({ TODO() },  Throwable::printStackTrace)  
		},  
		bindOnEvent = Lifecycle.Event.ON_START  
	)
}
```

## Approach description

Using ability to observe **lifecycle** changes of base android components (*Activity*, *Fragment*, etc.), we create [*Configurator*](app/src/main/java/ru/constorvar/timewaster/Configurator.kt) and tie it to component's lifecycle. 
*Configurator* is immutable object and should be created using [*Builder*](app/src/main/java/ru/constorvar/timewaster/Configurator.kt#L70). In *Builder* we can add **operation** (lambda `(LifecycleOwner) -> Unit`) and associate it with **lifecycle's event**, like:
```kotlin
Configurator.Builder()
	.addOperation(Lifecycle.Event.ON_RESUME) { TODO() }
```

## Basically, configuration is performed in:
- *Activity#onCreate()*
- *Fragment#onCreate()*
- *Fragment#onCreateView()*

## Pros
1. Configuration is placed in one place. (Not splitted between separate hook methods like *onCreate()*/*onStart()*/*onResume()*/*onPause()*/*onStop()*/*onDestroy()*).
2. There is no need in intermediate variables like views, bindings, disposables, etc.

## Cons
1. Need to be configured statically. No ability to add operation at any time of lifecycle.
2. Need to be configured at *create phase* of component.

