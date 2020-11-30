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

  def toSource: String = s"""HistoryRecord("$text", ${date}L, ${deleted map (_ + "L")})"""
}

object HistoryRecord0 extends HistoryRecord("-\\infty", Long.MinValue, None)

case class History(entries: Map[String, HistoryRecord]) {
  def apply(key: String): HistoryRecord = entries.getOrElse(key, HistoryRecord0)

  def sync(id: String)(implicit log: TheyLog = SilentBob): History = {
    log.info(s"sync for id=$id")
    val inStore = readFor(id) getOrElse this
    val newInStore = inStore updatedFrom this
    writeFor(id)(newInStore)
    val result = this updatedFrom inStore
    //    log.info(s"received:\n${this.toSource}")
    //    log.info(s"in store:\n${inStore.toSource}")
    //    log.info(s"new in store:\n${inStore.toSource}")
    //    log.info(s"new this: ${result.toSource}")
    result
  }

  def updatedFrom(other: History): History = {
    val keysToReport = entries.keySet ++ other.entries.keySet.filterNot(other(_).isDeleted)
    val newEntries = keysToReport map {
      (key: String) => key -> (this(key) max other(key))
    } toMap
    val yearago = Time.now() - 365 * 24 * 3600 * 1000
    History(newEntries.filter {
      case (k, v) => v.deleted.getOrElse(Long.MaxValue) > yearago
    })
  }

  def size: Int = entries.size

  def toSource: String =
    s"""History(Map(\n${
      entries.keySet.toList.sorted map { k => s""""$k"->${entries(k).toSource}""" } mkString ",\n"
    }\n))"""
}

object EmptyHistory extends History(Map.empty[String, HistoryRecord])

object Time {
  private[ops] var now: () => Long = () => 0L // System.currentTimeMillis() 
}
