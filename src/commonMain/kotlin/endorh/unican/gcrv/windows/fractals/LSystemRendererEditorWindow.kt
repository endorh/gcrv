package endorh.unican.gcrv.windows.fractals

import de.fabmax.kool.math.Vec2i
import de.fabmax.kool.modules.ui2.*
import de.fabmax.kool.util.MsdfFont
import endorh.unican.gcrv.FractalsScene
import endorh.unican.gcrv.l_system.CharRule
import endorh.unican.gcrv.l_system.RegexRule
import endorh.unican.gcrv.serialization.Color
import endorh.unican.gcrv.ui2.LabeledStringField
import endorh.unican.gcrv.windows.BaseWindow

class LSystemRendererEditorWindow(scene: FractalsScene) : BaseWindow<FractalsScene>("LSystem Rendering Editor", scene) {
   init {
      windowDockable.setFloatingBounds(width = Dp(420F), height = Dp(380F))
   }

   val font = MsdfFont.DEFAULT_FONT.derive(14F)
   val charAttributes = TextAttributes(font, Color.LIGHT_CYAN)
   val patternAttributes = TextAttributes(font, Color.LIGHT_GREEN)
   val textAttributes = TextAttributes(font, Color.WHITE)
   val errorAttributes = TextAttributes(font, Color.LIGHT_RED)
   val lines = mutableStateListOf(
      TextLine(listOf("F   F+F-F-F+F" to textAttributes)))
   val lineProvider = ListTextLineProvider(lines)
   val editorHandler = object : TextEditorHandler {
      val delegate = DefaultTextEditorHandler(lines)
      override fun insertText(line: Int, caret: Int, insertion: String, textAreaScope: TextAreaScope): Vec2i =
         delegate.insertText(line, caret, insertion, textAreaScope).also {
            onTextUpdate()
         }

      override fun replaceText(
         selectionStartLine: Int,
         selectionEndLine: Int,
         selectionStartChar: Int,
         selectionEndChar: Int,
         replacement: String,
         textAreaScope: TextAreaScope
      ) = delegate.replaceText(selectionStartLine, selectionEndLine, selectionStartChar, selectionEndChar, replacement, textAreaScope).also {
         onTextUpdate()
      }
   }

   private val WS = Regex("""\s+""")
   private val SPLIT = Regex("""(\S+)(?:(\s+)(.*))?""")
   init {
      reHighlight()
   }
   fun reHighlight() = lines.atomic {
      val highlighted = highlight(this)
      clear()
      addAll(highlighted)
   }
   fun highlight(lines: List<TextLine>) = lines.map { line ->
      val text = line.text
      val m = SPLIT.matchEntire(text) ?: return@map TextLine(listOf("" to textAttributes))
      val (_, a, s, b) = m.groupValues
      TextLine(listOf(
         a to if (a.length > 1) patternAttributes else charAttributes,
         s to textAttributes,
         b to textAttributes
      ).filter { it.first.isNotEmpty() })
   }
   fun onTextUpdate() {
      reHighlight()
      val pairs = lines.map { it.text.split(WS, 2) }
      val rules = pairs.mapNotNull {
         val a = it.first()
         val b = it.getOrNull(1) ?: ""
         if (a.isEmpty()) null
         else if (a.length > 1) RegexRule(a, b)
         else CharRule(a[0], b)
      }
      scene.lSystem.value = scene.lSystem.value.copy(rules=rules)
   }

   override fun UiScope.windowContent() = Column(Grow.Std, Grow.Std) {
      modifier.padding(horizontal = sizes.smallGap, vertical = sizes.smallGap)

      LabeledStringField("Axiom", scene.lSystem.use().axiom, { scene.lSystem.value = scene.lSystem.value.copy(axiom=it) }) {
         modifier.margin(top = sizes.smallGap)
      }

      TextArea(lineProvider, withVerticalScrollbar=false, withHorizontalScrollbar=false) {
         modifier.editorHandler(editorHandler)
         installDefaultSelectionHandler()
      }
   }
}