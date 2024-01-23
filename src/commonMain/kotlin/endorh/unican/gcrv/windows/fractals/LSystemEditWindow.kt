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

class LSystemEditWindow(scene: FractalsScene) : BaseWindow<FractalsScene>("LSystem Editor", scene) {
   init {
      windowDockable.setFloatingBounds(width = Dp(420F), height = Dp(380F))
   }

   val textAttributes = TextAttributes(MsdfFont.DEFAULT_FONT, Color.WHITE)
   val errorAttributes = TextAttributes(MsdfFont.DEFAULT_FONT, Color.LIGHT_RED)
   val lines = mutableStateListOf(
      TextLine(listOf("F    F+F-F-F+F" to textAttributes)))
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
   fun onTextUpdate() {
      println("Lines: $lines")
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

      // Row {
      //    modifier.margin(sizes.smallGap)
      //    FilePicker("Load") {
      //       modifier.width(Grow(0.5F))
      //          .showFileName(false)
      //          .fileFilters(FileFilter.JSON)
      //          .onFileChosen {
      //             scene.launch {
      //                val text = it.readAsText().await()
      //                val lSystem = JsonFormat.decodeFromString(serializer<LSystem>(), text)
      //                scene.lSystem.value = lSystem
      //             }
      //       }
      //    }
      //    FileSaver("Save") {
      //       modifier.width(Grow(0.5F)).margin(start=sizes.smallGap)
      //          .showFileName(false)
      //          .fileFilters(FileFilter.JSON)
      //          .onFileRequested {
      //             val ifs = JsonFormat.encodeToString(serializer<LSystem>(), scene.lSystem.value)
      //             FileWriteContents.StringContents(ifs)
      //          }
      //    }
      //    // Button("Paste") {
      //    //    modifier.width(Grow(0.5F)).margin(start=sizes.smallGap)
      //    //    onClick {
      //    //       Clipboard.getStringFromClipboard { it?.let { text ->
      //    //          println("Text: $text")
      //    //       }}
      //    //    }
      //    // }
      // }

      LabeledStringField("Axiom", scene.lSystem.use().axiom, { scene.lSystem.value = scene.lSystem.value.copy(axiom=it) }) {
         modifier.margin(top = sizes.smallGap)
      }

      TextArea(lineProvider, withVerticalScrollbar=false, withHorizontalScrollbar=false) {
         modifier.editorHandler(editorHandler)
         installDefaultSelectionHandler()
      }
   }
}