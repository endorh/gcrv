package endorh.unican.gcrv.windows.editor

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.ui2.FloatField
import endorh.unican.gcrv.ui2.LabeledField
import endorh.unican.gcrv.ui2.Vec2fField
import endorh.unican.gcrv.transformations.TaggedTransform2D
import endorh.unican.gcrv.transformations.Transform2D
import endorh.unican.gcrv.ui2.Section
import endorh.unican.gcrv.util.rad
import endorh.unican.gcrv.windows.BaseWindow

class TransformWindow(scene: EditorScene) : BaseWindow<EditorScene>("Transform", scene, true) {
   val translate = mutableStateOf(Vec2f(0F, 0F))
   val rotation = mutableStateOf(0F)
   val scale = mutableStateOf(Vec2f(1F, 1F))
   val shearX = mutableStateOf(0F)

   val transform get() = with(Transform2D) {
      translate(translate.value) * rotate(rad(rotation.value)) * shearX(shearX.value) * scale(scale.value)
   }

   val taggedTransform get() = TaggedTransform2D(rotation.value, scale.value, translate.value, shearX.value)

   override fun UiScope.windowContent() = ScrollArea(Grow.Std) {
      modifier.width(Grow.Std)
      Column(Grow.Std) {
         Column(Grow.Std) {
            modifier.margin(2.dp).background(RoundRectBackground(colors.backgroundVariant, 4.dp))

            LabeledField("Translate") {
               Vec2fField(translate.use(), { translate.value = it }) { it() }
            }

            LabeledField("Rotation") {
               FloatField(rotation.use(), { rotation.value = it }) { it() }
            }

            LabeledField("Scale") {
               Vec2fField(scale.use(), { scale.value = it }) { it() }
            }

            LabeledField("Shear") {
               FloatField(shearX.use(), { shearX.value = it }) { it() }
            }
         }

         Section("Transform") {
            Row(Grow.Std) {
               Text("Local") {
                  modifier.margin(4.dp).width(Grow(0.4F, max = 80.dp)).alignY(AlignmentY.Center)
               }
               Button("*") {
                  modifier.margin(4.dp).width(Grow(0.2F)).onClick {
                     val t = transform
                     for (o in scene.selectedObjects)
                        o.localTransforms.entries.lastOrNull()?.let {
                           it.setFrom(t * it.transform)
                        }
                  }
               }
               Button("/") {
                  modifier.margin(4.dp).width(Grow(0.2F)).onClick {
                     val t = transform.inverse
                     for (o in scene.selectedObjects)
                        o.localTransforms.entries.lastOrNull()?.let {
                           it.setFrom(t * it.transform)
                        }
                  }
               }
               Button("+") {
                  modifier.margin(4.dp).width(Grow(0.2F)).onClick {
                     val t = taggedTransform
                     for (o in scene.selectedObjects) {
                        o.localTransforms.insert()
                        o.localTransforms.entries.last().setFromTaggedTransform(t)
                     }
                  }
               }
            }

            Row(Grow.Std) {
               Text("Global") {
                  modifier.margin(4.dp).width(Grow(0.4F, max = 80.dp)).alignY(AlignmentY.Center)
               }
               Button("*") {
                  modifier.margin(4.dp).width(Grow(0.2F)).onClick {
                     val t = transform
                     for (o in scene.selectedObjects)
                        o.globalTransforms.entries.lastOrNull()?.let {
                           it.setFrom(t * it.transform)
                        }
                  }
               }
               Button("/") {
                  modifier.margin(4.dp).width(Grow(0.2F)).onClick {
                     val t = transform.inverse
                     for (o in scene.selectedObjects)
                        o.globalTransforms.entries.lastOrNull()?.let {
                           it.setFrom(t * it.transform)
                        }
                  }
               }
               Button("+") {
                  modifier.margin(4.dp).width(Grow(0.2F)).onClick {
                     val t = taggedTransform
                     for (o in scene.selectedObjects) {
                        o.globalTransforms.insert()
                        o.globalTransforms.entries.last().setFromTaggedTransform(t)
                     }
                  }
               }
            }
         }
         Section("Geometry") {
            Row(Grow.Std) {
               Text("Local") {
                  modifier.margin(4.dp).width(Grow(0.3F, max = 80.dp)).alignY(AlignmentY.Center)
               }
               Button("Apply") {
                  modifier.margin(4.dp).width(Grow(0.35F)).onClick {
                     val t = transform
                     for (o in scene.selectedObjects) {
                        val l = t.localize(o.geometricCenter)
                        for (prop in o.geometry)
                           prop.applyTransform(l)
                     }
                  }
               }
               Button("Revert") {
                  modifier.margin(4.dp).width(Grow(0.35F)).onClick {
                     val t = transform.inverse
                     for (o in scene.selectedObjects) {
                        val l = t.localize(o.geometricCenter)
                        for (prop in o.geometry)
                           prop.applyTransform(l)
                     }
                  }
               }
            }
            Row(Grow.Std) {
               Text("Global") {
                  modifier.margin(4.dp).width(Grow(0.3F, max = 80.dp)).alignY(AlignmentY.Center)
               }
               Button("Apply") {
                  modifier.margin(4.dp).width(Grow(0.35F)).onClick {
                     val t = transform
                     for (o in scene.selectedObjects)
                        for (prop in o.geometry)
                           prop.applyTransform(t)
                  }
               }
               Button("Revert") {
                  modifier.margin(4.dp).width(Grow(0.35F)).onClick {
                     val t = transform.inverse
                     for (o in scene.selectedObjects)
                        for (prop in o.geometry)
                           prop.applyTransform(t)
                  }
               }
            }
         }
      }
   }
}