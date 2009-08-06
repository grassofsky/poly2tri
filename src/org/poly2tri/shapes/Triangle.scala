/* Poly2Tri
 * Copyright (c) 2009, Mason Green
 * http://code.google.com/p/poly2tri/
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of Poly2Tri nor the names of its contributors may be
 *   used to endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.poly2tri.shapes

import scala.collection.mutable.HashSet
import scala.collection.mutable.ArrayBuffer

// Triangle-based data structures are know to have better performance than quad-edge structures
// See: J. Shewchuk, "Triangle: Engineering a 2D Quality Mesh Generator and Delaunay Triangulator"
//      "Triangulations in CGAL"
class Triangle(val points: Array[Point], val neighbors: Array[Triangle]) {

  // Flags to determine if an edge is the final Delauney edge
  val edges = Array(false, false, false)
  
  // Finalization flag
  var clean = false
  
  // Update neighbor pointers
  def updateNeighbors(ccwPoint: Point, cwPoint: Point, triangle: Triangle, mesh: HashSet[Triangle]) {
    if((ccwPoint == points(2) && cwPoint == points(1)) || (ccwPoint == points(1) && cwPoint == points(2))) 
      neighbors(0) = triangle 
    else if((ccwPoint == points(0) && cwPoint == points(2)) || (ccwPoint == points(2) && cwPoint == points(0)))
      neighbors(1) = triangle
    else if((ccwPoint == points(0) && cwPoint == points(1)) || (ccwPoint == points(1) && cwPoint == points(0)))
      neighbors(2) = triangle
    else {
      mesh += triangle
      //throw new Exception("Neighbor update error")
    }
  }
  
  def oppositePoint(t: Triangle) = {
    if(points(0) == t.points(1)) 
      points(1)
    else if(points(0) == t.points(2))
      points(2)
    else
      points(0)
  }
  
  def contains(p: Point): Boolean = (p == points(0) || p == points(1) || p == points(2))
  def contains(e: Segment): Boolean = (contains(e.p) && contains(e.q))
  
  // Fast point in triangle test
  def pointIn(point: Point): Boolean = {

    val pab = (point - points(0)).cross(ij)
    val pbc = (point - points(1)).cross(jk)
	var sameSign = Math.signum(pab) == Math.signum(pbc)
    if (!sameSign) return false

	val pca = (point - points(2)).cross(ki)
	sameSign = Math.signum(pab) == Math.signum(pca)
    if (!sameSign) return false
    
    true
  }

  // Locate first triangle crossed by constrained edge
  def locateFirst(edge: Segment): Triangle = {
    val p = edge.p
    val q = edge.q
    val e = p - q
    if(q == points(0)) {
      val sameSign = Math.signum(ik cross e) == Math.signum(ij cross e)
      if(!sameSign) return this
      if(neighbors(2) == null) return null
      return neighbors(2).locateFirst(edge)
    } else if(q == points(1)) {
      val sameSign = Math.signum(jk cross e) == Math.signum(ji cross e)
      if(!sameSign) return this
      if(neighbors(0) == null) return null
      return neighbors(0).locateFirst(edge)
    } else if(q == points(2)) {
      val sameSign = Math.signum(kj cross e) == Math.signum(ki cross e)
      if(!sameSign) return this
      if(neighbors(1) == null) return null
      return neighbors(1).locateFirst(edge)
    }
    throw new Exception("Point not found")
  }
  
  // Locate next triangle crossed by edge
  def findNeighbor(e: Point): Triangle = {
    if(orient(points(0), points(1), e) >= 0)
      return neighbors(2)
    else if(orient(points(1), points(2), e) >= 0)
      return neighbors(0)
    else if(orient(points(2), points(0), e) >= 0)
      return neighbors(1)
    else
      // Point must reside inside this triangle
      this
  }
  
  // Return: positive if point p is left of ab
  //         negative if point p is right of ab
  //         zero if points are colinear
  // See: http://www-2.cs.cmu.edu/~quake/robust.html
  def orient(b: Point, a: Point, p: Point): Float = {
    val acx = a.x - p.x
    val bcx = b.x - p.x
    val acy = a.y - p.y
    val bcy = b.y - p.y
    acx * bcy - acy * bcx  
  }
  
  // The neighbor clockwise to given point
  def neighborCW(point: Point): Triangle = {
    if(point == points(0)) {
      neighbors(2)
    }else if(point == points(1)) {
      neighbors(0)
    } else 
      neighbors(1)
  }
  
  // The neighbor counter-clockwise to given point
  def neighborCCW(point: Point): Triangle = {
    if(point == points(0)) {
      neighbors(1)
    }else if(point == points(1)) {
      neighbors(2)
    } else 
      neighbors(0)
  }
  
  // The neighbor clockwise to given point
  def neighborAcross(point: Point): Triangle = {
    if(point == points(0)) {
      neighbors(0)
    }else if(point == points(1)) {
      neighbors(1)
    } else 
      neighbors(2)
  }
  
  // The point counter-clockwise to given point
  def pointCCW(point: Point): Point = {
    if(point == points(0)) {
      points(1)
    } else if(point == points(1)) {
      points(2)
    } else if(point == points(2)){
      points(0)
    } else {
      throw new Exception("point location error")
    }
  }
  
  // The point counter-clockwise to given point
  def pointCW(point: Point): Point = {
    if(point == points(0)) {
      points(2)
    } else if(point == points(1)) {
      points(0)
    } else if(point == points(2)){
      points(1)
    } else {
      throw new Exception("point location error")
    }
  }
  
  // Legalized triangle by rotating clockwise around point(0)
  def legalize(oPoint: Point) {
	points(1) = points(0)
	points(0) = points(2)
	points(2) = oPoint
    updateEdges
  }
  
  // Legalize triagnle by rotating clockwise around oPoint
  def legalize(oPoint: Point, nPoint: Point) {
    if(oPoint == points(0)) {
      points(1) = points(0)
      points(0) = points(2)
      points(2) = nPoint 
    } else if (oPoint == points(1)) {
      points(2) = points(1)
      points(1) = points(0)
      points(0) = nPoint
    } else {
      points(0) = points(2)
      points(2) = points(1)
      points(1) = nPoint
    }
    updateEdges
  }
  
  // Rotate neighbors clockwise around give point. Share diagnal with triangle
  def rotateNeighborsCW(oPoint: Point, triangle: Triangle) {
    if(oPoint == points(0)) {
      neighbors(2) = neighbors(1)
      neighbors(1) = null
      neighbors(0) = triangle
    } else if (oPoint == points(1)) {
      neighbors(0) = neighbors(2)
      neighbors(2) = null
      neighbors(1) = triangle
    } else if (oPoint == points(2)) {
      neighbors(1) = neighbors(0)
      neighbors(0) = null
      neighbors(2) = triangle
    } else {
      throw new Exception("pointer bug")
    }
  }
  
  def printDebug = println(points(0) + "," + points(1) + "," + points(2))
  
  private var ik, ij , jk, ji, kj, ki: Point = null
  updateEdges
  
  // Update the edges that consitite this triangle
  // May change during legalization
  private def updateEdges {
    ik = points(2) - points(0)
    ij = points(1) - points(0)
    jk = points(2) - points(1)
    ji = points(0) - points(1)
    kj = points(1) - points(2)
    ki = points(0) - points(2)
  }
  
  // Mark edge as constrained
  def markEdge(e: Segment) {
    if((e.q == points(0) && e.p == points(1)) || (e.q == points(1) && e.p == points(0))) {
      edges(2) = true
    } else if ((e.q == points(0) && e.p == points(2)) || (e.q == points(2) && e.p == points(0))) {
      edges(1) = true
    } else {
      edges(0) = true
    } 
  }
  
}