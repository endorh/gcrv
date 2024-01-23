## Prácticas - Gráficos por Computador y Realidad Virtual

Demo web: [endorh.github.io/gcrv](https://endorh.github.io/gcrv/).

> [!NOTE]
> El rendimiento es algo inferior a la versión sobre JVM

### Sistema utilizado
Las prácticas se han desarrollado en Kotlin utilizando
[kool](https://github.com/fabmax/kool), un motor gráfico
basado en OpenGL/Vulkan, escrito en Kotlin.

#### Práctica 1 - Algoritmos de línea
Con el objetivo de implementar algoritmos clásicos de trazado de línea,
se ha desarrollado un componente de IU capaz de mostrar un lienzo
en el que sea posible editar píxeles individuales,
`BufferCanvas`.

Para representar lienzos de tamaño variable, la clase `ResizableCanvas`
abstrae la lógica que sustituye dinámicamente estos lienzos cuando
el tamaño cambia.

Por otra parte, la clase `ZoomableViewport`, si bien no utilizada hasta
la práctica 3, abstrae la lógica involucrada en mostrar en un mismo
lienzo una región móvil y ampliable de un espacio geométrico virtual,
así como la lógica involucrada en permitir la interacción con *gizmos*
en el espacio geométrico a través de esta transformación.

Este componente mantiene una serie de texturas de tamaño fijo en memoria
como arrays en formato RGBA, proporcionando distintas abstracciones para
actualizar una de ellas, mientras la textura del frame anterior es
enviada a GPU, permitiendo elegir acceder al frame anterior en
operaciones de lectura.

La actualización de la textura en GPU solo puede ocurrir en el thread de
renderizado, por lo que dentro de corutinas debe lanzarse dentro de
`withContext(Dispatchers.RenderLoop)` o `launchOnMainThread`.

Cuando es necesario redimensionar el lienzo, se sustituye el `BufferCanvas` existente,
ya que este no soporta cambios de tamaño.

Con el objetivo de poder representar diferentes tipos de objetos
con flexibilidad, la lógica involucrada en dibujar estos sobre el
lienzo se ha abstraído en la clase `RenderingPipeline2D`, que permite
dividir el proceso de dibujo en una serie de pases, `RenderPass2D`,
cada uno responsable de dibujar objetos de un tipo concreto.

El pase `WireframeRenderPass2D` es responsable de dibujar todo tipo
de líneas rectas en el lienzo, para lo que, de manera similar a otros
pases, recoge todas las líneas descritas por los objetos a dibujar en
el lienzo, e invoca por cada una al *renderizador* adecuado, del
tipo `Line2DRenderer`.

Estos objetos, ubicados en el paquete `endorh.unican.gcrv.renderers.line`,
implementan una interfaz sencilla que abstrae la lógica involucrada
en el acceso a píxeles del lienzo, de manera que su implementación
se reduce únicamente a los distintos algoritmos de trazado de línea
utilizados.
- `Slope-Intercept`: Despeja la `y` respecto a la `x` y traza la línea
  recorriendo el rango en `x` entre el punto más a la izquierda y el
  más a la derecha. Naturalmente, solo produce un trazado continuo
  para rectas con pendiente menor en valor absoluto a `1`.
- `Modified Slope-Intercept`: Aplica `Slope-Intercept` intercambiando
  los papeles de los ejes para obtener un trazado continuo en todos los
  casos.
- `DDA` (Digital Differential Analyzer): Recorre la línea utilizando un
  paso fraccionario para obtener un trazado continuo.
  A diferencia del resto de algoritmos, requiere utilizar aritmética
  de coma flotante.
- `Algoritmo de Bressenham`: Utiliza una idea similar a DDA, pero
  aprovecha que la pendiente es un número racional para reducir las
  operaciones necesarias a aritmética entera.
- `Bresenham con anchura`: Aplica Bresenham teniendo en cuenta la anchura
  de la línea a trazar.
- `Bresenham con antialiasing`: Aprovecha el error relativo de cada píxel
  respecto a la posición real más cercana de la línea, del que el
  algoritmo lleva la cuenta, para reducir la intensidad de los píxeles
  más alejados de la recta, suavizando su aspecto a gran escala.

Además, existen algunos *renderizadores* más sencillos:
- `Orthogonal`: Solo traza líneas paralelas a los ejes.
- `Slope One`: Solo traza líneas con pendiente `1` o `-`.
- `Bresenham en el Primer Octante`: Implementación sencilla del algoritmo
  de Bresenham solo para el primer octante (cuadrante)

Además de estos algoritmos de trazado de líneas, existen
*renderizadores* análogos para trazar puntos y curvas cúbicas de Bézier,
así como para rellenar el interior de polígonos convexos,
también ubicados en el paquete `endorh.unican.gcrv.renderers`.

Para facilitar la observación del funcionamiento de estos algoritmos
de trazado a nivel de píxel, es posible presionar la tecla `Alt` para
mostrar una ventana de lupa flotante *pixel-perfect* que permite
ampliar sin distorsión una región con diferentes niveles de zoom
(controlado con la rueda del ratón mientras la lupa está activa).

#### Práctica 2 - Transformaciones 2D
Para la segunda práctica, se ha implementado un sencillo editor de
animación 2D basado en transformaciones afines, y la planificación
de propiedades de objetos en una línea de tiempo mediante *keyframes*.

Para describir transformaciones afines en 2D, se han implementado dos
clases, `Transform2D`, que describe las entradas en una matriz 3x3,
y `TaggedTransform2D`, que describe una transformación afín en
términos de rotación, escala, traslación y sesgo horizontal, y cuenta
con métodos para traducir entre ambas representaciones.

En el editor de animación, se pueden crear objetos de varios tipos:
- Líneas (segmentos)
- Puntos
- Splines cúbicos de Bézier
- Triángulos
- Polígonos (rellenos, aunque el relleno solo se ha implementado para
  polígonos convexos)
- Poli-líneas (varios segmentos)
- Grupos de otros objetos (permiten aplicar una misma transformación
  a varios objetos con facilidad)

Los objetos del canvas cuentan con una serie de propiedades,
que pueden ser modificadas desde la ventana *Inspector*, una vez
seleccionado un objeto desde la ventana *Outliner* o desde el
propio lienzo.

Todas estas propiedades pueden ser animadas, utilizando los controles
de la ventana con la Línea de Tiempo (*Timeline*), que permite
definir *keyframes* para cada propiedad de cada objeto.

Una vez una propiedad tiene al menos un *keyframe*, cualquier
modificación de dicha propiedad, ya sea en el Inspector o debida
a manipulaciones en el lienzo crea o modifica *keyframes*, en vez
de modificar directamente la propiedad, simplificando el proceso
de animación.

La Línea de Tiempo cuenta con diferentes controles para reproducir
la animación.

Entre las propiedades de cada objeto, ciertas propiedades se consideran
geométricas, y pueden ser modificadas mediante transformaciones
afines desde la ventana *Geometric Transformations*.

Además, todos los objetos cuentan con dos propiedades,
`globalTransforms` y `localTransforms`, que permiten aplicar
una cantidad arbitraria de transformaciones afines (animables) al
objeto.
Las transformaciones locales trasladan previamente el origen al centro
geométrico del objeto, para permitir aplicar transformaciones
frecuentemente aplicadas localmente con facilidad.

#### Práctica 3 - Fractales
Para la tercera práctica, se han implementado múltiples ventanas
para la visualización de distintos tipos de fractales.

##### Mandelbrot y Julia
En las ventanas de los fractales de *Mandelbrot* y *Julia*, se ha
utilizado programas sombreadores (*shaders*), escritos en *KSL*, un
DSL de Kotlin proporcionado por Kool que permite describir programas
de *GLSL* en Kotlin, si bien de manera algo incómoda.

Estos programas permiten calcular el color de cada pixel del fractal
en paralelo en GPU, lo que permite obtener una imagen de gran resolución
en tiempo real, incluso en navegadores web.
No obstante, la implementación está limitada por la precisión simple
de coma flotante disponible en *GLSL*.

Esta limitación, sin embargo, permite visualizar de manera bastante
visual la pérdida de precisión de la coma flotante al alejarse las
coordenadas de `0`.
Haciendo suficiente zoom en el fractal de *Mandelbrot* en zonas
alejadas del centro horizontalmente pero no verticalmente, se pueden
apreciar grupos de píxeles que forman rectángulos horizontales en los
que cada píxel redondea a la misma posición, mientras que en zonas
alejadas verticalmente del centro pero no horizontalmente, estos
grupos forman rectángulos verticales.

En algunas pantallas, dependiendo del controlador (es muy inconsistente),
también se puede apreciar una línea diagonal que separa las zonas en las
que algunos píxeles prefieren redondear al alza o a la baja,
produciendo un desplazamiento entre los rectángulos descritos
anteriormente a cada lado de la diagonal.

##### Fractales Recursivos
En la ventana *Recursive Fractals*, se pueden elegir distintos
*renderizadores* de fractales recursivos, que construyen de manera
recursiva de acuerdo a distintas reglas objetos a representar en el
lienzo.

Esta recursión está acotada por un número de iteraciones controlable
en la barra superior, y también puede restringirse al área visible
en el lienzo, para permitir visualizar con mayor nivel de detalle una
zona concreta del fractal.

##### Sistemas de Funciones Iteradas (IFS)
En las ventanas dedicadas a fractales IFS, se puede visualizar y
editar un sistema de funciones iteradas.

La ventana *IFS Canvas* permite visualizar el sistema, y controlar
la resolución del mismo.

La ventana *IFS List* permite editar manualmente las propiedades de
cada una de las funciones afines del sistema, así como su color y
su peso relativo en la generación del fractal.
También es posible cargar y guardar sistemas de funciones iteradas
en formato JSON, así como importarlas en un formato utilizado en
clase desde el portapapeles.

Por último, la ventana *IFS Editor* permite editar directamente las
funciones del sistema, manipulando directamente el cuadrilátero imagen
del cuadrado centrado `[-1, +1]²`, ya sea arrastrándolo en el espacio,
o los distintos controles en cada uno de sus vértices, que permiten
rotar la imagen, escalarla en cada eje, y aplicar distintos sesgos
arbitrarios.

#### Práctica 4 - L-Sistemas (Sistemas de Lindenmayer)
Para la cuarta práctica se ha implementado un sencillo editor de
L-Sistemas.

En la ventana *L-System Editor* se puede especificar un axioma para
el sistema, y una serie de reglas de producción, separadas en líneas
distintas, delimitando el símbolo o expresión regular a reemplazar con
una cantidad arbitraria de espacios al comienzo de la línea.

En caso de existir múltiples reglas de producción aplicables en un
paso, se elige una al azar de acuerdo a sus pesos, si bien no existe
ninguna forma de configurar estos pesos en el editor.

La ventana *L-System Canvas* permite visualizar el sistema, así como
controlar el número de iteraciones a simular.

No se ha implementado un editor análogo que permita especificar las
reglas que traducen los símbolos terminales del sistema en operaciones
geométricas, si bien la infraestructura necesaria está presente en
el código.
En su lugar, se ofrece un conjunto de operaciones predefinidas:
- `A`, `B`, `C`, `F`, `G`: avanzar una unidad en la dirección actual
  dibujando una línea
- `+`/`-`: girar +90º/-90º (en sentido antihorario)
- `/`/`\`: girar +60º/-60º (en sentido antihorario)
- `l`/`r`: girar +25º/-25º (en sentido antihorario)
- `[`/`]`: guardar una posición y dirección/volver a la posición y
  dirección guardada (permite describir bifurcaciones de manera
  sencilla en la gramática)

Cualquier otro símbolo producido por el sistema se ignora durante
la generación geométrica.

### Ejecución
Para ejecutar la práctica en JVM se puede ejecutar el siguiente comando:
```
gradlew jvmRun --quiet 
```

Para ejecutar la práctica en JS se puede ejecutar el siguiente comando,
que debería abrir automáticamente un navegador con en `localhost:8080` o un puerto
similar, donde se puede ver la práctica:
```
gradlew jsBrowserDevelopmentRun
```

Alternativamente, para compilar la versión web a una página estática,
se puede utilizar el comando:
```
./gradlew jsBrowserDistribution
```

Este comando genera una página estática en el directorio `dist/gcrv`.
Como ejemplo de como integrar esta tarea con un sistema de CI/CD,
sirve la [configuración](https://github.com/endorh/gcrv/blob/main/.github/workflows/kotlin-js-gh-pages.yml)
de este proyecto para servir la demo en GitHub Pages.