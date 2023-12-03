## Práctica 1 - Algoritmos de Línea

### Sistema utilizado
La práctica se ha desarrollado en Kotlin utilizando
[kool](https://github.com/fabmax/kool), un motor gráfico
basado en OpenGL/Vulkan escrito en Kotlin.

Los algoritmos de línea se han implementado en el paquete
`endorh.unican.gcrv.renderers.line`, implementando la interfaz
`Line2DRenderer`.

El resultado de la renderización se muestra utilizando el componente
`endorh.unican.gcrv.ui2.BufferCanvas`, que también ha sido desarrollado para
esta práctica.
Este componente mantiene una textura de tamaño fijo en memoria como un array
utilizando formato RGBA, proporcionando distintas utilidades para actualizar
la copia existente de esta textura en GPU, y alterar píxeles concretos.

Esta actualización de la textura solo puede ocurrir en el thread de renderizado,
por lo que dentro de corutinas debería estar envuelto en una llamada a
`withContext(Dispatchers.RenderLoop) { ... }`.

Cuando es necesario redimensionar el lienzo, se sustituye el `BufferCanvas` existente,
ya que este no soporta cambios de tamaño.

Para abstraer las operaciones que son necesarias para redibujar el lienzo, se ha
creado la clase `endorh.unican.gcrv.line_algorithms.RenderingPipeline2D`.

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

### Controles del lienzo
- `Click izquierdo`: añade un nuevo punto de una línea
- `Alt`: Muestra una ventana de zoom
- `Rueda del ratón`: (con `Alt` pulsado) aumenta o disminuye el zoom