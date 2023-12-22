package endorh.unican.gcrv.util

import de.fabmax.kool.KoolContext
import de.fabmax.kool.input.InputStack
import de.fabmax.kool.input.KeyEvent
import de.fabmax.kool.input.KeyboardInput

internal val ModifierStateKeyListeners = mutableListOf(
   InputStack.SimpleKeyListener(KeyboardInput.KEY_CTRL_LEFT, "Ctrl state", { true }) {
      ModifierState.ctrlPressed = it.isPressed
   },
   InputStack.SimpleKeyListener(KeyboardInput.KEY_ALT_LEFT, "Alt state", { true }) {
      ModifierState.altPressed = it.isPressed
   },
   InputStack.SimpleKeyListener(KeyboardInput.KEY_SHIFT_LEFT, "Shift state", { true }) {
      ModifierState.shiftPressed = it.isPressed
   },
)

internal object ModifierStateListener : InputStack.KeyboardListener {
   override fun handleKeyboard(keyEvents: MutableList<KeyEvent>, ctx: KoolContext) {
      keyEvents.lastOrNull()?.let {
         ModifierState.ctrlPressed = it.isCtrlDown
         ModifierState.altPressed = it.isAltDown
         ModifierState.shiftPressed = it.isShiftDown
      }
   }
}

fun interface ModifierChangeListener {
   fun onModifierChange()
}
object ModifierState {
   private val onModifierChange = mutableListOf<ModifierChangeListener>()

   private var lockedCtrl = false
   private var lockedAlt = false
   private var lockedShift = false

   var ctrlPressed = false
      internal set(value) {
         if (!lockedCtrl) field = value
      }
   var altPressed = false
      internal set(value) {
         if (!lockedAlt) field = value
      }
   var shiftPressed = false
      internal set(value) {
         if (!lockedShift) field = value
      }

   fun emulateCtrl(pressed: Boolean, lock: Boolean = false) {
      lockedCtrl = false
      ctrlPressed = pressed
      lockedCtrl = lock
   }

   fun emulateAlt(pressed: Boolean, lock: Boolean = false) {
      lockedAlt = false
      altPressed = pressed
      lockedAlt = lock
   }

   fun emulateShift(pressed: Boolean, lock: Boolean) {
      lockedShift = false
      shiftPressed = pressed
      lockedShift = lock
   }

   fun registerListeners() {
      val handler = InputStack.InputHandler("Modifier State Listener")
      handler.keyboardListeners += ModifierStateListener
      InputStack.pushTop(handler)
      InputStack.onInputStackChanged += {
         InputStack.handlerStack.remove(handler)
         InputStack.handlerStack.add(handler)
      }
   }

   fun addListener(listener: ModifierChangeListener) {
      onModifierChange += listener
   }
   fun removeListener(listener: ModifierChangeListener) {
      onModifierChange -= listener
   }
}