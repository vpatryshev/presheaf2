package com.presheaf.ops

object HistoryTestData {
  val initFromClient: History = History(Map(
    "1" -> HistoryRecord("A", 332L, Some(328L)),
    "2" -> HistoryRecord("B", 121L, Some(116L)),
    "3" -> HistoryRecord("C", 435L, Some(432L)),
    "4" -> HistoryRecord("D", 1095L, Some(1092L)),
    "5" -> HistoryRecord("E", 524L, Some(522L)),
    "6" -> HistoryRecord("F", 1494L, None),
    "7" -> HistoryRecord("G", 759L, None),
    "8" -> HistoryRecord("H", 262L, Some(256L)),
    "9" -> HistoryRecord("I", 1124L, Some(1121L)),
    "a" -> HistoryRecord("J", 1611L, Some(1606L)),
    "b" -> HistoryRecord("K", 1545L, Some(1540L)),
    "c" -> HistoryRecord("L", 963L, Some(958L)),
    "d" -> HistoryRecord("M", 31L, Some(21L))
  ))

  val initInDB: History = History(Map(
    "1" -> HistoryRecord("A", 332L, Some(328L)),
    "2" -> HistoryRecord("B", 121L, Some(116L)),
    "3" -> HistoryRecord("C", 1208L, None),
    "4" -> HistoryRecord("D", 1208L, None),
    "5" -> HistoryRecord("E", 524L, Some(522L)),
    "6" -> HistoryRecord("F", 1494L, None),
    "7" -> HistoryRecord("G", 759L, None),
    "8" -> HistoryRecord("H", 1208L, None),
    "9" -> HistoryRecord("I", 1124L, Some(1121L)),
    "a" -> HistoryRecord("J", 1365L, Some(1606L)),
    "b" -> HistoryRecord("K", 1545L, Some(1540L)),
    "c" -> HistoryRecord("L", 963L, Some(958L)),
    "d" -> HistoryRecord("M", 31L, Some(21L))
  ))

  val resultInDB: History = History(Map(
    "1" -> HistoryRecord("A", 332L, Some(328L)),
    "2" -> HistoryRecord("B", 121L, Some(116L)),
    "3" -> HistoryRecord("C", 1208L, None),
    "4" -> HistoryRecord("D", 1208L, None),
    "5" -> HistoryRecord("E", 524L, Some(522L)),
    "6" -> HistoryRecord("F", 1494L, None),
    "7" -> HistoryRecord("G", 759L, None),
    "8" -> HistoryRecord("H", 1208L, None),
    "9" -> HistoryRecord("I", 1124L, Some(1121L)),
    "a" -> HistoryRecord("J", 1365L, Some(1606L)),
    "b" -> HistoryRecord("K", 1545L, Some(1540L)),
    "c" -> HistoryRecord("L", 963L, Some(958L)),
    "d" -> HistoryRecord("M", 31L, Some(21L))
  ))

  val resultToClient: History = History(Map(
    "1" -> HistoryRecord("A", 332L, Some(328L)),
    "2" -> HistoryRecord("B", 121L, Some(116L)),
    "3" -> HistoryRecord("C", 1208L, None),
    "4" -> HistoryRecord("D", 1208L, None),
    "5" -> HistoryRecord("E", 524L, Some(522L)),
    "6" -> HistoryRecord("F", 1494L, None),
    "7" -> HistoryRecord("G", 759L, None),
    "8" -> HistoryRecord("H", 1208L, None),
    "9" -> HistoryRecord("I", 1124L, Some(1121L)),
    "a" -> HistoryRecord("J", 1611L, Some(1606L)),
    "b" -> HistoryRecord("K", 1545L, Some(1540L)),
    "c" -> HistoryRecord("L", 963L, Some(958L)),
    "d" -> HistoryRecord("M", 31L, Some(21L))
  ))
}
