package com.crawler.core.frontier

case class PrioritizedUrl(
                           url: String,
                           depth: Int,
                           priority: Int,
                           timestamp: Long = System.currentTimeMillis()
                         ) extends Comparable[PrioritizedUrl] {
  // Higher priority first, then earlier timestamp
  override def compareTo(other: PrioritizedUrl): Int = {
    val priorityCompare = other.priority.compareTo(this.priority)
    if (priorityCompare != 0) priorityCompare
    else this.timestamp.compareTo(other.timestamp)
  }
}