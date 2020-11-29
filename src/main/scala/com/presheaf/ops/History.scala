package com.presheaf.ops
import com.presheaf.http.Storage._

case class HistoryRecord(
    text: String,
    date: Long,
    deleted: Option[Long]
) {
  def isDeleted: Boolean = deleted.isDefined

  def timestamp: Long = Math.max(deleted getOrElse Long.MinValue, date) // won't work after 01/19/2038

  def max(that: HistoryRecord): HistoryRecord =
    if (timestamp > that.timestamp) this else that
}

object HistoryRecord0 extends HistoryRecord("-\\infty", Long.MinValue, None)

case class History(entries: Map[String, HistoryRecord]) {
  def apply(key: String): HistoryRecord = entries.getOrElse(key, HistoryRecord0)

  def sync(id: String): History = {
    val inStore = readFor(id) getOrElse this
    writeFor(id)(inStore updatedFrom this)
    this updatedFrom inStore
  }

  def updatedFrom(other: History): History = {
    val keysToReport = entries.keySet ++ other.entries.keySet.filterNot(other(_).isDeleted)
    val newEntries = keysToReport map {
      (key: String) => key -> (this(key) max other(key))
    } toMap

    History(newEntries)
  }

  def size: Int = entries.size
}

object EmptyHistory extends History(Map.empty[String, HistoryRecord])
