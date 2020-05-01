import java.util

val s1 = Set("a", "b")
val s2 = Set("b", "a")
s1.hashCode()

val n = new Integer(97)
val js1 = new util.HashSet[Object]()
val js2 = new util.HashSet[Object]()
js1.add("a"); js1.add("b"); js1.add(n)
js2.add(n); js2.add("b"); js2.add("a")
js1
js2
js1.hashCode()
js2.hashCode()
"a".hashCode
new Integer(97).hashCode()

