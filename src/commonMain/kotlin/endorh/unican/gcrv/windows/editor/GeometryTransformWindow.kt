package endorh.unican.gcrv.windows.editor

import de.fabmax.kool.math.Vec2f
import de.fabmax.kool.modules.ui2.*
import endorh.unican.gcrv.EditorScene
import endorh.unican.gcrv.scene.Object2D
import endorh.unican.gcrv.transformations.Transform2D
import endorh.unican.gcrv.ui2.*
import endorh.unican.gcrv.util.rad
import endorh.unican.gcrv.util.unaryMinus
import endorh.unican.gcrv.windows.BaseWindow

class GeometryTransformWindow(scene: EditorScene) : BaseWindow<EditorScene>("Geo Transform", scene, true) {
   val local = mutableStateOf(true)

   val translate = mutableStateOf(Vec2f(0F, 0F))
   val rotation = mutableStateOf(0F)
   val scale = mutableStateOf(Vec2f(1F, 1F))
   val shear = mutableStateOf(Vec2f(0F, 0F))

   val reflectPoint = mutableStateOf(Vec2f.ZERO)
   val reflectAngle = mutableStateOf(0F)

   fun applyTransform(t: Transform2D, o: Object2D, noLocal: Boolean = false) {
      val tt = if (local.value && !noLocal) t.localize(o.geometricCenter) else t
      for (prop in o.geometry) prop.applyTransform(tt)
      for (child in o.children)
         applyTransform(tt, child, true)
   }

   fun applyTransform(t: Transform2D) {
      for (o in scene.selectedObjects) {
         applyTransform(t, o)
      }
   }

   override fun UiScope.windowContent() = ScrollArea(Grow.Std) {
      modifier.width(Grow.Std)
      Column(Grow.Std) {
         LabeledBooleanField("Local", local.use(), { local.value = it }) {
            modifier.width(Grow.Std)
         }
         Section("Translate") {
            Row(Grow.Std) {
               modifier.margin(4.dp)
               Vec2fField(translate.use(), { translate.value = it }) { modifier.width(Grow(0.7F)).alignY(AlignmentY.Center) }
               Button("*") {
                  modifier.margin(4.dp).width(Grow(0.15F)).onClick {
                     applyTransform(Transform2D.translate(translate.value))
                  }
               }
               Button("/") {
                  modifier.margin(4.dp).width(Grow(0.15F)).onClick {
                     applyTransform(Transform2D.translate(translate.value).inverse)
                  }
               }
            }
         }

         Section("Scale") {
            Row(Grow.Std) {
               modifier.margin(4.dp)
               Vec2fField(scale.use(), { scale.value = it }) { modifier.width(Grow(0.7F)).alignY(AlignmentY.Center) }
               Button("*") {
                  modifier.margin(4.dp).width(Grow(0.15F)).onClick {
                     applyTransform(Transform2D.scale(scale.value))
                  }
               }
               Button("/") {
                  modifier.margin(4.dp).width(Grow(0.15F)).onClick {
                     applyTransform(Transform2D.scale(scale.value).inverse)
                  }
               }
            }
         }

         Section("Rotation") {
            Row(Grow.Std) {
               modifier.margin(4.dp)
               FloatField(rotation.use(), { rotation.value = it }) { modifier.width(Grow(0.7F)).alignY(AlignmentY.Center) }
               Button("*") {
                  modifier.margin(4.dp).width(Grow(0.15F)).onClick {
                     applyTransform(Transform2D.rotate(rad(rotation.value)))
                  }
               }
               Button("/") {
                  modifier.margin(4.dp).width(Grow(0.15F)).onClick {
                     applyTransform(Transform2D.rotate(rad(rotation.value)).inverse)
                  }
               }
            }
         }

         Section("Reflect") {
            LabeledField("Point") {
               Vec2fField(reflectPoint.use(), { reflectPoint.value = it }) { it() }
            }
            LabeledField("Angle") {
               FloatField(reflectAngle.use(), { reflectAngle.value = it }) { it() }
            }
            Row(Grow.Std) {
               Button("Apply") {
                  modifier.margin(4.dp).width(Grow(0.5F)).onClick {
                     val t = with (Transform2D) {
                        translate(reflectPoint.value) * rotate(rad(reflectAngle.value)) *
                          scale(Vec2f(1F, -1F)) * rotate(-rad(reflectAngle.value)) *
                          translate(-reflectPoint.value)
                     }
                     applyTransform(t)
                  }
               }
            }
         }

         Section("Shear") {
            Row(Grow.Std) {
               modifier.margin(4.dp)
               Vec2fField(shear.use(), { shear.value = it }) { modifier.width(Grow(0.7F)) }
               Button("*") {
                  modifier.margin(4.dp).width(Grow(0.15F)).onClick {
                     applyTransform(Transform2D.shear(shear.value))
                  }
               }
               Button("/") {
                  modifier.margin(4.dp).width(Grow(0.15F)).onClick {
                     applyTransform(Transform2D.shear(shear.value).inverse)
                  }
               }
            }
         }
      }
   }
}