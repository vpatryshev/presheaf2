package com.presheaf.ops

import com.presheaf.http.Storage.{historyFile, readFor}
import com.presheaf.ops.HistoryTestData.{initFromClient, initInDB, resultInDB, resultToClient}
import org.specs2.mutable._

import scala.util.Success

class HistoryTest extends Specification {

  Time.now = () => 0L

  def vr(content: String): HistoryRecord = {
    val time = content.charAt(0) - 'a' + 1
    HistoryRecord(content, time, None)
  }

  def dr(content: String): HistoryRecord = {
    val time = content.charAt(0) - 'a' + 2
    HistoryRecord(content, time, Some(time))
  }

  def history(records: HistoryRecord*): History = {
    History(records map (r => r.text.toUpperCase -> r) toMap)
  }

  def h(texts: String*): History = {
    History(texts map (s => s.toUpperCase -> vr(s)) toMap)
  }

  "HistoryRecord" should {
    "buildValid" in {
      val sut = vr("a")
      sut.isDeleted === false
      sut.timestamp === 1
      sut.text === "a"
    }

    "buildDeleted" in {
      val sut1 = dr("b")
      sut1.isDeleted === true
      sut1.timestamp === 3
      sut1.text === "b"

      val sut2 = HistoryRecord("c", 3, Some(2))
      sut2.isDeleted === true
      sut2.timestamp === 3
      sut2.text === "c"
    }

    "calculate max" in {
      val sut0 = vr("a")
      val sut1 = dr("b")
      val sut2 = dr("c")

      (HistoryRecord0 max HistoryRecord0) === HistoryRecord0

      def check(rec1: HistoryRecord, rec2: HistoryRecord, expected: HistoryRecord): Unit = {
        (rec1 max rec2) === expected
        (rec2 max rec1) === expected
        (rec1 max HistoryRecord0) === rec1
        (rec2 max HistoryRecord0) === rec2
        (HistoryRecord0 max rec1) === rec1
        (HistoryRecord0 max rec2) === rec2
        (rec1 max rec1) === rec1
        (rec2 max rec2) === rec2
      }

      check(sut0, sut1, sut1)
      check(sut0, sut2, sut2)
      check(sut1, sut2, sut2)

      ok
    }
  }

  "History" should {

    "work even empty" in {
      EmptyHistory("missing") === HistoryRecord0
      EmptyHistory.size === 0
    }

    val r1 = vr("a")
    val r2 = dr("b")
    val r3 = vr("c")
    val h3 = history(r1, r2, r3)

    "apply" in {
      h3.size === 3
      h3("A") === r1
      h3("B") === r2
      h3("C") === r3
      h3("D") === HistoryRecord0
    }

    "updatedFrom" in {
      (h3 updatedFrom h3) === h3
      (h3 updatedFrom EmptyHistory) === h3
      (EmptyHistory updatedFrom h3) === h("a", "c")
      val r4 = dr("d")
      val r5 = vr("e")
      val h3a = history(r1, r4, r3, r5)
      (h3 updatedFrom h3a) === history(r1, r2, r3, r5)
      (h3a updatedFrom h3) === history(r1, r3, r4, r5)
    }

    "sync" in {
      val r4 = dr("d")
      val r5 = vr("e")
      val h3a = history(r1, r4, r3, r5)
      historyFile("h3").delete()
      historyFile("h3").deleteOnExit()
      historyFile("h3a").delete()
      historyFile("h3a").deleteOnExit()
      (h3 sync "h3") === h3
      (h3a sync "h3a") === h3a
      (h3a sync "h3") === history(r1, r3, r4, r5)
      readFor("h3") === Success(history(r1, r2, r3, r5))
      readFor("h3a") === Success(h3a)
    }
    
    "sync big 1" in {
      historyFile("big1").delete()
      historyFile("big1").deleteOnExit()
      val big1 = initInDB sync "big1"
      big1 === initInDB
      val big2 = initFromClient sync "big1"
      resultToClient === big2
    }
  }
}
