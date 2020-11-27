package com.presheaf.ops

final case class HistoryEntry(text: String,
                   date: Long,
                   deleted: Option[Long]
)

final case class History(entries: Map[String, HistoryEntry])
