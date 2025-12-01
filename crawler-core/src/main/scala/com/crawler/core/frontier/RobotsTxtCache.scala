// File: crawler-core/src/main/scala/com/crawler/core/frontier/RobotsTxtCache.scala
package com.crawler.core.frontier

import java.util.concurrent.ConcurrentHashMap

case class RobotsTxt(
                      allowedPaths: Set[String] = Set.empty,
                      disallowedPaths: Set[String] = Set.empty,
                      crawlDelay: Option[Int] = None
                    ) {
  def isAllowed(path: String): Boolean = {
    if (allowedPaths.exists(p => path.startsWith(p))) return true
    if (disallowedPaths.exists(p => path.startsWith(p))) return false
    true
  }
}

class RobotsTxtCache {
  private val cache = new ConcurrentHashMap[String, RobotsTxt]()

  def get(domain: String): Option[RobotsTxt] = {
    Option(cache.get(domain))
  }

  def put(domain: String, robots: RobotsTxt): Unit = {
    cache.put(domain, robots)
  }

  def parse(content: String): RobotsTxt = {
    val lines = content.split("\n").map(_.trim).filter(_.nonEmpty)
    var inUserAgentBlock = false
    var allowed = Set.empty[String]
    var disallowed = Set.empty[String]
    var crawlDelay: Option[Int] = None

    for (line <- lines) {
      val lower = line.toLowerCase
      if (lower.startsWith("user-agent:")) {
        val agent = lower.substring(11).trim
        inUserAgentBlock = agent == "*" || agent.contains("crawler")
      } else if (inUserAgentBlock) {
        if (lower.startsWith("disallow:")) {
          val path = line.substring(9).trim
          if (path.nonEmpty) disallowed += path
        } else if (lower.startsWith("allow:")) {
          val path = line.substring(6).trim
          if (path.nonEmpty) allowed += path
        } else if (lower.startsWith("crawl-delay:")) {
          crawlDelay = lower.substring(12).trim.toIntOption
        }
      }
    }

    RobotsTxt(allowed, disallowed, crawlDelay)
  }
}