package endorh.unican.gcrv.shaders

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.blocks.mvpMatrix
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.modules.ui2.Ui2Shader
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.BlendMode
import de.fabmax.kool.pipeline.CullMethod
import de.fabmax.kool.pipeline.DepthCompareOp
import endorh.unican.gcrv.util.F
import endorh.unican.gcrv.util.toMultiIntArray
import endorh.unican.gcrv.util.uInt1Array
import endorh.unican.gcrv.util.uint1VarAssignInt1

class MandelbrotFixedPointShader(val maxArraySize: Int = 16) : KslShader(Model(maxArraySize), pipelineConfig), GenTextureViewportShader {
   var iterations by uniform1i("uIterations", 1)
   private var uDiv by uniform1i("uPrecision", 1)
   var divSize: Int
      get() = uDiv
      set(value) {
         if (value > maxArraySize) throw IllegalArgumentException("divSize must be <= $maxArraySize")
         uDiv = value
      }
   private var uIntSize by uniform1i("uIntSize", 1)
   var intSize: Int
      get() = uIntSize
      set(value) {
         if (value > maxArraySize) throw IllegalArgumentException("intSize must be <= $maxArraySize")
         uIntSize = value
      }

   private val uLeftBorder by uniform1iv("uLeftBorder", maxArraySize)
   private val uTopBorder by uniform1iv("uTopBorder", maxArraySize)
   private val uRightBorder by uniform1iv("uRightBorder", maxArraySize)
   private val uBottomBorder by uniform1iv("uBottomBorder", maxArraySize)

   @OptIn(ExperimentalUnsignedTypes::class)
   override fun setViewport(left: BigDecimal, top: BigDecimal, right: BigDecimal, bottom: BigDecimal) {
      left.toMultiIntArray(maxArraySize, intSize, uDiv).asIntArray().copyInto(uLeftBorder)
      top.toMultiIntArray(maxArraySize, intSize, uDiv).asIntArray().copyInto(uTopBorder)
      right.toMultiIntArray(maxArraySize, intSize, uDiv).asIntArray().copyInto(uRightBorder)
      bottom.toMultiIntArray(maxArraySize, intSize, uDiv).asIntArray().copyInto(uBottomBorder)
   }

   private class Model(val maxArraySize: Int = 16) : KslProgram("Mandelbrot fixed-point shader") {
      init {
         val iterations = uniformInt1("uIterations")
         val div = uniformInt1("uPrecision")
         val intSize = uniformInt1("uIntSize")

         val leftBorder = uniformInt1Array("uLeftBorder", maxArraySize)
         val topBorder = uniformInt1Array("uTopBorder", maxArraySize)
         val rightBorder = uniformInt1Array("uRightBorder", maxArraySize)
         val bottomBorder = uniformInt1Array("uBottomBorder", maxArraySize)

         val texCoords = interStageFloat2()
         val screenPos = interStageFloat2()
         val tint = interStageFloat4()
         val clipBounds = interStageFloat4(interpolation = KslInterStageInterpolation.Flat)

         vertexStage {
            main {
               texCoords.input set vertexAttribFloat2(Attribute.TEXTURE_COORDS.name)
               tint.input set vertexAttribFloat4(Attribute.COLORS.name)
               clipBounds.input set vertexAttribFloat4(Ui2Shader.ATTRIB_CLIP.name)

               val vertexPos = float4Var(float4Value(vertexAttribFloat3(Attribute.POSITIONS.name), 1f))
               screenPos.input set vertexPos.xy
               outPosition set mvpMatrix().matrix * vertexPos
            }
         }

         fragmentStage {
            val mulAccMultiInt = getOrCreateFunction("mulAccMultiInt") {
               functionUint1Array("mulAccMultiInt", maxArraySize) {
                  val a = paramUint1Array(maxArraySize, "a")
                  val b = paramUint1Array(maxArraySize, "b")
                  val out = paramUint1Array(maxArraySize, "out")
                  body {
                     val next = uint1Var(0u.const)
                     val carry = uint1Var(0u.const)
                     val prev = uint1Var()
                     val ah = uint1Var()
                     val bh = uint1Var()
                     val mx = int1Var(div)
                     fori(0.const, mx) { i ->
                        val j = div - 1.const - i
                        ah set (a[i] shr 16u.const)
                        bh set (b[j] shr 16u.const)
                        prev set next
                        next set next + ah*bh
                        carry set (next lt prev).toUint1()
                        prev set next
                        next set next + ((a[i] and 0xFFFFu.const) * bh shr 16u.const)
                        carry set carry + (next lt prev).toUint1()
                        prev set next
                        next set next + ((b[j] and 0xFFFFu.const) * ah shr 16u.const)
                        carry set carry + (next lt prev).toUint1()
                     }
                     fori(0.const, intSize) { k ->
                        out[k] set out[k] + next
                        next set carry
                        carry set 0u.const
                        mx set min(intSize, k + div + 1.const)
                        fori(max(0.const, k + div - intSize + 1.const), mx) { i ->
                           val j = k + div - i
                           prev set out[k]
                           out[k] set out[k] + a[i] * b[j]
                           `if`(out[k] lt prev) {
                              prev set next
                              next set next + 1u.const
                              carry set carry + (next lt prev).toUint1()
                           }
                           ah set (a[i] shr 16u.const)
                           bh set (b[j] shr 16u.const)
                           prev set next
                           next set next + ah*bh
                           carry set carry + (next lt prev).toUint1()
                           prev set next
                           next set next + ((a[i] and 0xFFFFu.const) * bh shr 16u.const)
                           carry set carry + (next lt prev).toUint1()
                           prev set next
                           next set next + ((b[j] and 0xFFFFu.const) * ah shr 16u.const)
                           carry set carry + (next lt prev).toUint1()
                        }
                     }
                     out
                  }
               }
            }

            val addMultiInt = getOrCreateFunction("addMultiInt") {
               functionUint1Array("addMultiInt", maxArraySize) {
                  val a = paramUint1Array(maxArraySize, "a")
                  val b = paramUint1Array(maxArraySize, "b")
                  val out = paramUint1Array(maxArraySize, "out")
                  body {
                     val carry = uint1Var(0u.const);
                     fori(0.const, intSize) { i ->
                        out[i] set a[i] + b[i] + carry
                        carry set (out[i] lt a[i]).toUint1()
                     }
                     out
                  }
               }
            }

            val shlMultiInt = getOrCreateFunction("shlMultiInt") {
               functionUint1Array("shlMultiInt", maxArraySize) {
                  val a = paramUint1Array(maxArraySize, "a")
                  val shift = paramUint1("shift")
                  val out = paramUint1Array(maxArraySize, "out")
                  body {
                     val shOffset = (shift shr 5u.const).toInt1()
                     val shRem = (shift and 0x1Fu.const)
                     val i = int1Var(intSize - shOffset - 1.const)
                     `for`(i, i ge 0.const, (-1).const) {
                        out[i + shOffset] set (a[i] shl shRem)
                        `if`((i gt 1.const) and (shRem ne 0u.const)) {
                           out[i + shOffset] set (out[i + shOffset] or (a[i - 1.const] shr (32u.const - shRem)))
                        }
                     }
                     `for`(i, i lt shOffset, 1.const) {
                        out[i] set 0u.const
                     }
                     out
                  }
               }
            }

            val shrMultiInt = getOrCreateFunction("shrMultiInt") {
               functionUint1Array("shrMultiInt", maxArraySize) {
                  val a = paramUint1Array(maxArraySize, "a")
                  val shift = paramUint1("shift")
                  val out = paramUint1Array(maxArraySize, "out")
                  body {
                     val shOffset = (shift shr 5u.const).toInt1()
                     val shRem = (shift and 0x1Fu.const)
                     val lim = int1Var(intSize - shOffset)
                     fori(0.const, lim) { i ->
                        out[i] set (a[i + shOffset] shr shRem)
                        `if`((i lt lim - 1.const) and (shRem ne 0u.const)) {
                           out[i] set (out[i] or (a[i + shOffset + 1.const] shl (32u.const - shRem)))
                        }
                     }
                     fori(lim, intSize) { i ->
                        out[i] set 0u.const
                     }
                     out
                  }
               }
            }

            val subMultiInt = getOrCreateFunction("subMultiInt") {
               functionUint1Array("subMultiInt", maxArraySize) {
                  val a = paramUint1Array(maxArraySize, "a")
                  val b = paramUint1Array(maxArraySize, "b")
                  val out = paramUint1Array(maxArraySize, "out")
                  body {
                     val carry = uint1Var(0u.const);
                     fori(0.const, intSize) { i ->
                        out[i] set a[i] - b[i] - carry
                        carry set (out[i] gt a[i]).toUint1()
                     }
                     out
                  }
               }
            }

            val negMultiInt = getOrCreateFunction("negMultiInt") {
               functionUint1Array("negMultiInt", maxArraySize) {
                  val a = paramUint1Array(maxArraySize, "a")
                  val out = paramUint1Array(maxArraySize, "out")
                  body {
                     val carry = uint1Var(0u.const)
                     fori(0.const, intSize) { i ->
                        out[i] set 0u.const - a[i] - carry
                        carry set (out[i] gt 0u.const - a[i]).toUint1()
                     }
                     out
                  }
               }
            }

            val setMultiInt = getOrCreateFunction("setMultiInt") {
               functionUint1Array("setMultiInt", maxArraySize) {
                  val a = paramUint1Array(maxArraySize, "a")
                  val out = paramUint1Array(maxArraySize, "out")
                  body {
                     fori(0.const, intSize) { i ->
                        out[i] set a[i]
                     }
                     out
                  }
               }
            }

            val uIntToMultiInt = getOrCreateFunction("uIntToMultiInt") {
               functionUint1Array("uIntToMultiInt", maxArraySize) {
                  val a = paramUint1("a")
                  val out = paramUint1Array(maxArraySize, "out")
                  body {
                     fori(0.const, intSize) { i ->
                        out[i] set 0u.const
                     }
                     out[div] set a
                     out
                  }
               }
            }

            val floatToMultiInt = getOrCreateFunction("floatToMultiInt") {
               functionUint1Array("floatToMultiInt", maxArraySize) {
                  val f = paramFloat1("f")
                  val out = paramUint1Array(maxArraySize, "out")
                  body {
                     val sign = f lt 0F.const
                     val abs = float1Var(abs(f))
                     val uInt = uint1Var(abs.toUint1())
                     val frac = float1Var(abs - uInt.toFloat1())
                     val pow = float1Var(pow(2F.const, div.toFloat1() * 32F.const))
                     val uIntArray = uInt1Array(maxArraySize, 0u.const, "uIntArray")
                     uIntArray[maxArraySize - 1] set uInt
                     fori(0.const, intSize) { i ->
                        out[i] set uIntArray[i]
                     }
                     val fracArray = uInt1Array(maxArraySize, 0u.const, "fracArray")
                     fori(0.const, intSize) { i ->
                        frac set frac * pow(2F.const, 32F.const)
                        fracArray[i] set frac.toUint1()
                     }
                     val temp = uInt1Array(maxArraySize, 0u.const, "temp")
                     mulAccMultiInt(out, fracArray, temp)
                     fori(0.const, intSize) { i ->
                        out[i] set temp[i]
                     }
                     `if`(sign) {
                        negMultiInt(out, out)
                     }
                     out
                  }
               }
            }

            val multiIntToFloat = getOrCreateFunction("multiIntToFloat") {
               functionFloat1("multiIntToFloat") {
                  val a = paramUint1Array(maxArraySize, "a")
                  body {
                     val sign = a[intSize - 1.const] shr 31u.const
                     `if`(sign eq 1u.const) {

                     }
                     val out = float1Var(0F.const)
                     val pow = float1Var(pow(2F.const, -div.toFloat1() * 32F.const))
                     fori(0.const, intSize) { i ->
                        out set out + a[i].toFloat1() * pow
                        pow set pow * pow(2F.const, 32F.const)
                     }
                     out
                  }
               }
            }

            val multiIntLerp = getOrCreateFunction("multiIntLerp") {
               functionUint1Array("multiIntLerp", maxArraySize) {
                  val a = paramUint1Array(maxArraySize, "a")
                  val b = paramUint1Array(maxArraySize, "b")
                  val t = paramFloat1("t")
                  val out = paramUint1Array(maxArraySize, "out")
                  body {
                     val diff = uInt1Array(maxArraySize, 0u.const, "diff")
                     val f = uInt1Array(maxArraySize, 0u.const, "f")
                     subMultiInt(b, a, diff)
                     floatToMultiInt(t, f)
                     mulAccMultiInt(diff, f, out)
                     addMultiInt(a, out, out)
                     out
                  }
               }
            }

            main {
               `if`(
                  all(screenPos.output gt clipBounds.output.xy) and
                    all(screenPos.output lt clipBounds.output.zw)
               ) {
                  val i = uint1Var(0u.const)
                  val itersI = int1Var(iterations)
                  val iters = uint1Var()
                  uint1VarAssignInt1(iters, itersI)

                  val cr = uInt1Array(maxArraySize, 0u.const, "cr")
                  cr[maxArraySize - 1] set texCoords.output.x.toUint1()
                  val cc = uInt1Array(maxArraySize, 0u.const, "cc")
                  cc[maxArraySize - 1] set texCoords.output.y.toUint1()
                  val zr = uInt1Array(maxArraySize, 0u.const, "zr")
                  val zc = uInt1Array(maxArraySize, 0u.const, "zc")
                  val nz = uInt1Array(maxArraySize, 0u.const, "nz")
                  val nc = uInt1Array(maxArraySize, 0u.const, "nc")
                  val temp = uInt1Array(maxArraySize, 0u.const, "temp")
                  val normSquared = float1Var(0F.const)
                  val maxNormSquared = float1Var(normSquared)
                  `while`((i lt iters) and (normSquared lt 4F.const)) {
                     uIntToMultiInt(0u.const, nz)
                     uIntToMultiInt(0u.const, temp)
                     mulAccMultiInt(zc, zc, temp)
                     subMultiInt(cr, temp, nz)
                     mulAccMultiInt(zr, zr, nz)

                     uIntToMultiInt(0u.const, nc)
                     uIntToMultiInt(0u.const, temp)
                     mulAccMultiInt(zr, zc, temp)
                     shlMultiInt(temp, 1u.const, temp) // * 2F
                     addMultiInt(cc, temp, nc)

                     // nz set zr * zr - zc * zc + cr
                     // nc set zr * zc * 2F.const + cc
                     // zr set nz
                     // zc set nc

                     setMultiInt(nz, zr)
                     setMultiInt(nc, zc)

                     val zrf = float1Var(multiIntToFloat(zr))
                     val zcf = float1Var(multiIntToFloat(zc))
                     normSquared set zrf * zrf + zcf * zcf
                     maxNormSquared set max(maxNormSquared, normSquared)
                     i set i + 1u.const
                  }
                  val color = float4Var()
                  color.a set 1F.const
                  `if`(i ge iters) {
                     color.r set 0F.const
                     color.g set maxNormSquared / 8F.const
                     color.b set 0F.const
                  }.`else` {
                     val quotient = i.F / iters.F
                     val rr = quotient
                     val bb = 1F.const - quotient
                     color.r set rr
                     color.g set 2F.const * rr * bb
                     color.b set bb
                  }
                  colorOutput(color.rgb * color.a, color.a)
               }.`else` {
                  discard()
               }
            }
         }

      }
   }

   companion object {
      private val pipelineConfig = PipelineConfig().apply {
         blendMode = BlendMode.BLEND_PREMULTIPLIED_ALPHA
         cullMethod = CullMethod.NO_CULLING
         depthTest = DepthCompareOp.DISABLED
      }
   }
}
