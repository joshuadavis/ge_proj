import groovy.json.JsonSlurper

/**
 * Geometry programming example
 * <br>
 * User: Josh
 * Date: 5/29/2014
 * Time: 3:20 PM
 */

class Helpers
{
  static double normalizeTheta(double t)
  {
    t < 0.0 ? 2 * Math.PI + t : t
  }

  static double calculateTheta(int x, int y)
  {
    normalizeTheta(Math.atan2(y, x))
  }
}

class Point
{
  int x
  int y

  Point(jsonInput)
  {
    x = jsonInput.x
    y = jsonInput.y
  }


  @Override
  public String toString()
  {
    return "Point{" +
            "x=" + x +
            ", y=" + y +
            '}';
  }
}

class Segment
{
  Point start
  Point end
  double theta

  Segment(Point start, Point end)
  {
    this.start = start
    this.end = end
    // Absolute angle
    theta = Helpers.calculateTheta(dy, dx)
  }

  int getDx() { end.x - start.x }

  int getDy() { end.y - start.y }

  double getThetaDeg() { Math.toDegrees(theta) }

  public String toString()
  {
    return "Segment{start=$start, end=$end, theta= $theta ($thetaDeg) }}";
  }
}

class Vertex
{
  Segment prev
  Segment next
  double theta
  boolean clockwise

  double getThetaDeg() { Math.toDegrees(theta) }

  public String toString()
  {
    return "Vertex{prev=$prev, next=$next, theta=$theta ($thetaDeg), clockwise=$clockwise}";
  }
}

class Shape
{
  String id

  /**
   * Line segments for the shape.
   */
  List<Segment> segments = []

  /**
   * Vertices - pairs of segments
   */
  List<Vertex> vertices = []

  boolean convex = true

  Shape(jsonInput)
  {
    id = jsonInput.get("id")
    println "== Shape ${id} =="
    Point prevPoint = null
    Segment prevSeg = null
    jsonInput.point.each { jsonPoint ->
      Point point = new Point(jsonPoint)
      if (prevPoint)
        prevSeg = addSegment(prevPoint, point, prevSeg)
      prevPoint = point
    }

    // Make a segment between the first and last point
    addSegment(prevPoint, segments[0].start, prevSeg)

    // Compute convexity
    computeConvex()
  }

  private void computeConvex()
  {
    convex = true // Assume it's convex...
    for (Vertex v : vertices)   // Not using Groovy style here so we can return
    {
      if (v.theta > Math.PI)
      {
        println "${id} is not convex, interior angle > 180 : ${v}"
        convex = false
        return // We're done here.
      }
      // We could do other checks here, but this is sufficient for the example data set.
    }

  }

  private Segment addSegment(Point start, Point end, Segment prevSeg)
  {
    def seg = new Segment(start, end)
    segments.add(seg)
    println "Added ${seg}"
    if (prevSeg)
      addVertex(prevSeg, seg)
    return seg
  }

  private void addVertex(Segment prev, Segment next)
  {
    // Relative angle
    double theta = Helpers.normalizeTheta(next.theta - prev.theta)

    // If this is the first vertex, determine the direction, otherwise use the direction of the first vertex.
    // If the angle between the two segments is greater than 180 deg, then we're going clockwise.
    boolean clockwise = vertices.isEmpty() ? theta > Math.PI : vertices.first().clockwise

    // Adjust theta: If clockwise, subtract 180 degrees.   We always want the interior angle
    theta = clockwise ? theta - Math.PI : theta

    def vertex = new Vertex(prev: prev, next: next, theta: theta, clockwise: clockwise)
    vertices.add(vertex)

    println "Added ${vertex}";
  }
}

class Geometry
{
  Map<String, Shape> shapesById = [:]

  Geometry(jsonInput)
  {
    // Handle the case where the 'shape' property is not a list.
    def shapes = (jsonInput.geometry.shape instanceof List) ? jsonInput.geometry.shape : Collections.singletonList(jsonInput.geometry.shape)
    shapes.each { jsonShape ->
      def shape = new Shape(jsonShape)
      shapesById.put(shape.id, shape)
    }
  }

  Collection<Shape> getShapes()
  {
    shapesById.values()
  }

  Collection<Shape> getConvexShapes()
  {
    shapesById.values().findAll { Shape s -> s.convex }
  }
}

def input1 = new JsonSlurper().parse(new FileReader("input1.json"))
def input2 = new JsonSlurper().parse(new FileReader("input2.json"))

println "input1=${input1}"
println "input2=${input2}"

Geometry geometry1 = new Geometry(input1)

Geometry geometry2 = new Geometry(input2)

// Go through shapes in geometry2 and generate the report...

println "== Report =="

geometry2.shapes.each { Shape s ->
  if (!s.convex)
  {
    println "\"${s.id}\" is not a (convex) polygon"
  }
  else
  {
    // Show surrounds/intersects/separate for every other shape that is convex.
    geometry2.convexShapes.findAll( { it.id != s.id } ).each { Shape other ->
      println "${s.id} vs ${other.id} ..."
    }
  }
}