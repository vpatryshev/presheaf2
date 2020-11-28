package com.presheaf.ops
import com.presheaf.http.Storage._

case class HistoryRecord(
  text: String,
  date: Long,
  deleted: Option[Long]
) {
  def timestamp: Long = Math.max(deleted getOrElse Long.MinValue, date) // won't work after 01/19/2038
  
  def max(that: HistoryRecord): HistoryRecord =
    if (timestamp > that.timestamp) this else that
}

object Inf extends HistoryRecord("-\\infty", Long.MinValue, None)

final case class History(entries: Map[String, HistoryRecord]) {
  def entryAt(key: String): HistoryRecord = entries.getOrElse(key, Inf)
  
  def syncup(id: String): History = readFor(id) map syncup getOrElse this

  def syncup(other: History): History = {
    val allkeys = entries.keySet ++ other.entries.keySet
    val newEntries = allkeys map {
      (key: String) => key -> (entryAt(key) max other.entryAt(key))
    } toMap
    
    History(newEntries)
  }
  
  def size: Int = entries.size
}
