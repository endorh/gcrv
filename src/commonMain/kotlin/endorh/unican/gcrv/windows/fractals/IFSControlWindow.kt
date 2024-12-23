package endorh.unican.gcrv.windows.fractals

import de.fabmax.kool.Clipboard
import de.fabmax.kool.demo.LabeledSwitch
import de.fabmax.kool.math.DEG_2_RAD
import de.fabmax.kool.math.RAD_2_DEG
import de.fabmax.kool.math.clamp
import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.FractalsScene
import endorh.unican.gcrv.fractals.*
import endorh.unican.gcrv.serialization.JsonFormat
import endorh.unican.gcrv.ui2.*
import endorh.unican.gcrv.util.*
import endorh.unican.gcrv.windows.BaseWindow
import kotlinx.coroutines.*
import kotlinx.serialization.serializer

class IFSControlWindow(scene: FractalsScene) : BaseWindow<FractalsScene>("IFS List", scene) {
   init {
      windowDockable.setFloatingBounds(width = Dp(420F), height = Dp(380F))
   }

   val selectedIndex = mutableStateOf<Int?>(null)
   val expanded = mutableStateOf(false)

   override fun UiScope.windowContent() = Column(Grow.Std, Grow.Std) {
      modifier.padding(horizontal = sizes.smallGap, vertical = sizes.smallGap)

      Row {
         modifier.margin(sizes.smallGap)
         FilePicker("Load") {
            modifier.width(Grow(0.5F))
               .showFileName(false)
               .fileFilters(FileFilter.JSON)
               .onFileChosen {
                  scene.launch {
                     val text = it.readAsText().await()
                     val ifs = JsonFormat.decodeFromString(serializer<IFS>(), text)
                     scene.ifsDraft.load(ifs)
                  }
            }
         }
         FileSaver("Save") {
            modifier.width(Grow(0.5F)).margin(start=sizes.smallGap)
               .showFileName(false)
               .fileFilters(FileFilter.JSON)
               .onFileRequested {
                  val ifs = JsonFormat.encodeToString(serializer<IFS>(), scene.ifsDraft.create())
                  FileWriteContents.StringContents(ifs)
               }
         }
         Button("Paste") {
            modifier.width(Grow(0.5F)).margin(start=sizes.smallGap)
            onClick {
               Clipboard.getStringFromClipboard { it?.let { text ->
                  IFS.importFromText(text)?.let { ifs ->
                     scene.launch {
                        scene.ifsDraft.load(ifs)
                     }
                  }
               }}
            }
         }
         Button("Copy") {
            modifier.width(Grow(0.5F)).margin(start=sizes.smallGap)
            onClick {
               Clipboard.copyToClipboard(IFS.exportToText(scene.ifs))
            }
         }
      }
      Row {
         modifier.height(28.dp)
         Button("+") {
            modifier.height(28.dp).onClick {
               val l = scene.ifsDraft.functions.value
               scene.ifsDraft.functions.value = l + listOf(IFSFunctionDraft())
            }
         }
         Button("-") {
            modifier.margin(start = sizes.smallGap).height(28.dp).onClick {
               val l = scene.ifsDraft.functions.value
               if (l.isNotEmpty()) scene.ifsDraft.functions.value =
                  selectedIndex.value?.clamp(0, scene.ifsDraft.functions.value.size - 1)?.let {
                     l.toMutableList().apply { removeAt(it) }.toList()
                  } ?: l.subList(0, l.size - 1)
            }
         }

         LabeledSwitch("Details", expanded) {
            modifier.height(28.dp).alignY(AlignmentY.Top)
         }
      }

      ScrollArea {
         modifier.width(Grow.Std)
         Column(Grow.Std) {
            for ((i, ifs) in scene.ifsDraft.functions.use(surface).withIndex()) {
               fun getIFS(i: Int) = scene.ifsDraft.functions.value[i]
               Column(Grow.Std, scopeName = "IFS/$i") {
                  modifier.backgroundColor(
                     if (selectedIndex.use() == i) colors.backgroundVariantAlpha(0.6F)
                     else colors.backgroundVariantAlpha(0.2F)
                  ).padding(sizes.smallGap).onClick {
                     selectedIndex.value = i
                  }

                  if (expanded.use()) Column(Grow.Std) {
                     modifier.margin(top = sizes.smallGap)
                     LabeledFloatField("Weight", ifs.weight.use(surface), { getIFS(i).weight.value = it }) {
                        modifier.onClick { selectedIndex.value = i }
                     }
                     LabeledColorField("Color", ifs.color.use(surface), { getIFS(i).color.value = it }) {
                        modifier.zLayer(10)
                     }
                     LabeledFloatField("Rotation", (ifs.transform.use(surface).rotation * RAD_2_DEG).F, { getIFS(i).transform.value = getIFS(i).transform.value.copy(rotation = (it * DEG_2_RAD).F) }) {
                        modifier.onClick { selectedIndex.value = i }
                     }
                     LabeledVec2fField("Scale", ifs.transform.use(surface).scale, { getIFS(i).transform.value = getIFS(i).transform.value.copy(scale = it) }) {
                        modifier.onClick { selectedIndex.value = i }
                     }
                     LabeledVec2fField("Translate", ifs.transform.use(surface).translate, { getIFS(i).transform.value = getIFS(i).transform.value.copy(translate = it) }) {
                        modifier.onClick { selectedIndex.value = i }
                     }
                     LabeledFloatField("Shear X", ifs.transform.use(surface).shearX, { getIFS(i).transform.value = getIFS(i).transform.value.copy(shearX = it) }) {
                        modifier.onClick { selectedIndex.value = i }
                     }
                  } else Row(Grow.Std) {
                     ColorField(ifs.color.use(surface), { getIFS(i).color.value = it }) {
                        modifier.width(Grow(0.7F))
                     }
                     FloatField(ifs.weight.use(surface), { getIFS(i).weight.value = it }) {
                        modifier.width(Grow(0.3F)).margin(start = sizes.smallGap).onClick { selectedIndex.value = i }
                     }
                  }
               }
            }
         }
      }
   }
}