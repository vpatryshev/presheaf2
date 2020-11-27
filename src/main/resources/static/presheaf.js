/**
 * Functionality for presheaf.com
 */
const _ = id => document.getElementById(id) 

const skip = () => {}

const setState = state => {
  _("d_status").innerHTML = state
  document.title = state
}

const error = msg => {
  setState("error")
  _("d_error").innerHTML = msg.replace(/\n/g, "<br/>")
  hideResults()
}

const srcRef = id => `cache/${id}.src`
const pdfRef = id => `cache/${id}.pdf`
const imgRef = id => `cache/${id}.png`

function newImage(key) {
  var img = new Image()
  img.src = imgRef(key)
  return img
}

const option = x => 
  typeof x === 'undefined' ? None : Some(x)

const None = {
  map: f => None,
  flatMap: f => None,
  toString: () => 'None'
}

const Some = x => ({
  flatMap: (f => f(x)),
  map: (f => option(f(x))),
  toString: () => `Some(${x})`
})

const hide = (id) => {
//  console.log("Whill hide " + id)
  const el = _(id)
  if (el) {
    const s = el.style
    s.display = "none"
    s.visibility = "hidden"
  }
}

hideResults = () => _("d_results").style.display="none"

const show = (id) => {
  const el = _(id)
  if (el) {
    const s = el.style
    s.visibility = "visible"
    s.display = "block"
  }
}

const quoteRef = id => ('<a href="' + getUrl() + '?d=' + id + '"><img src="https://presheaf.com/' + imgRef(id) +
         '" title="click to go to presheaf.com for editing"/></a>')

function justShow(id) {
  let ref = pdfRef(id)
  _("d_png").src  = imgRef(id)
  _("d_pdf").href = ref
  _("d_pdf_e").src = ref
  _("d_pdf_o").data = ref
  _("d_quote").value = quoteRef(id)
  getSrc(id)
  _("d_results").style.display="block"
}

idNumber = i => _(`i.${i}`).src.match("/([^\\./]+)\\.png")[1]

function sortByDate(map) {
  var a = []
  for (key in map) {
    if (key && map.hasOwnProperty(key))
    a.push(key)
  }

  a.sort(function(x,y) { return map[y].date - map[x].date })

  return a
}

var MAX_HISTORY_LENGTH = 1000

function getHistory() {
  if (!localStorage.history) {
    localStorage.history = "{}"
  }
  var found = JSON.parse(localStorage.history)
  delete found[undefined]
  return found
}

var myHistory = getHistory()

const saveHistory = () => {
  localStorage.history = JSON.stringify(myHistory)
  post("history", localStorage.history)
}

function touch(id) {
//  console.log("touching " + id)
  myHistory[id].date = new Date().getTime()
  saveHistory()
  showHistory()
}

function choose(i) {
  let id = idNumber(i)
  justShow(id)
  touch(id)
}

const addToHistory = (id, text) => {
  if (!myHistory[id]) myHistory[id] = {}
  myHistory[id].text = text
  touch(id)
  delete myHistory[id].deleted
  console.log(`added ${id}=>${JSON.stringify(myHistory[id])}`)
  saveHistory()
  showHistory()
}

deleteFromHistory = i => {
  const id = idNumber(i)
  console.log(`Will delete row ${i}=>${id}`)
  myHistory[id].deleted = new Date().getTime()
  saveHistory()
  showHistory()
}

function showHistory() {
  let sorted = sortByDate(myHistory)
  // kick out the last one if too many
  if (sorted.length > MAX_HISTORY_LENGTH) {
    for (i = MAX_HISTORY_LENGTH; i < sorted.length; i++) {
      delete myHistory[sorted[i]]
    }
    sorted = sorted.splice(MAX_HISTORY_LENGTH, sorted.length - MAX_HISTORY_LENGTH)
  }
  let validOnes = sorted.filter(id => !myHistory[id].deleted)
  fillImages(validOnes)
}

function fillImages(ids) {
  for (i = 0; i < ids.length; i++) {
    let id = ids[i]
    fillHistoryElement(i, id)
  }
  hide(`hi.${ids.length}`)
}

function fillHistoryElement(i, id) {
  let ref = _(`ai.${i}`)
  let hel = myHistory[id]
  if (id && !hel.deleted) {
    let loadedImage = newImage(id)
    loadedImage.id = `i.${i}`
    if (ref) {
      ref.title = hel.text
      loadedImage.onload = function() {
        let key = this.id
        let el = _(key)
        el.src = this.src
        el.width = Math.min(100, this.width)
        show(`h${key}`)
        show(key)
      }
    }
  }
}

function showDiagram(diagram) {
  setState("Here's your diagram.")
  justShow(diagram.id)
  addToHistory(diagram.id, diagram.source)
}

function httpGet(uri, onwait, onload, onerror) {
  var xhr = new XMLHttpRequest()
  xhr.onreadystatechange = function() {
    if (xhr.readyState == 4) {
      if (xhr.status == 200) {
        try {
          onload(xhr.responseText)
        } catch (e) {
          onerror(`oops, ${e}`)
        }
      } else {
        onerror(`Got error ${xhr.status} from the server.`)
      }
    }
  }
  xhr.open("GET", uri, true)
  xhr.send()
  onwait()
}

var postResponse = undefined
var postError = undefined

function post(uri, data) {
  try {
      httpPost(uri, localStorage.history, skip,
      text => { postResponse = text }, err => { postError = err })
  } catch (err) {
      postError += '\n' + err
  }
}

function httpPost(uri, data, onwait, onload, onerror) {
  var xhr = new XMLHttpRequest()
  xhr.onreadystatechange = function() {
    if (xhr.readyState == 4) {
      if (xhr.status == 200) {
        try {
          onload(xhr.responseText)
        } catch (e) {
          onerror(`oops, ${e}`)
        }
      } else {
        onerror(`Got error ${xhr.status} from the server.`)
      }
    }
  }
  xhr.open("POST", uri)
  xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8")
  xhr.send(data)
  onwait()
}

function getSrc(id) {
  httpGet(srcRef(id), skip,
   text => { _("d_in").value = text }, error
  )
}

function send(input) {
  httpGet(`dws?in=${encodeURIComponent(input)}`,
      () => {
        setState("please wait...")
        _("d_error").innerHTML = ""
      },
      text => {
        console.log(`Got response <<<${text}>>>`)
        try {
          let response = JSON.parse(text)
          if (response.error) {
            error(`Error: ${response.error}`)
          } else {
            response.image = newImage(response.id)
            response.image.onload = () => showDiagram(response)
          }
        } catch (e) {
          error(`error: ${e}`)
        }
      },
      error
  )
}

function commit() {
  const input = _("d_in").value
  send(input)
}

function fillSamples(sources) {
  var loadedImages = []
  for (i = 0; i < sources.length; i++) {
    let id = sources[i].id
    let toid = typeof(id)

    if (id && (toid != 'undefined')) {
      _(`samples${i % 2}`).innerHTML += `<div class='diagramEntry' id='s.i.${id}'/>`
      loadedImages[i] = newImage(id)
      loadedImages[i].id = `i.${id}`
      loadedImages[i].alt = sources[i].source
      setListeners(loadedImages[i], id)
//    } else {
//      console.log("#" + i + " is bad: " + id)
    }
  }
}

// separate function so the context does not leak into the closures
function setListeners(image, id) {
  image.onclick = Function('justShow("' + id + '")')
  image.onload = function() {
    this.width = Math.min(100, this.width)
    _(`s.${this.id}`).appendChild(this)
  }
}

function fillIn() {
  httpGet("dws?in=X",
      skip,
      text => {
        _("d_version").innerHTML = eval(`(${text})`).version
      },
      error
  )
  httpGet("dws?op=samples",
      function() {},
      function(text) {
        try {
          fillSamples(JSON.parse(text))
        } catch(e) {
          error(`exception: ${e}\n${text}`)
        }
      },
      error
  )
}

function getUrl() {
  if (x = new RegExp('([^?]+)').exec(location.href)) return x[1]
}

function getArg(name) {
  if (name = (new RegExp('[?&]' + name + '=([^&]+)')).exec(location.search)) return name[1]
}

historyFrag = i =>
  `<div class=historyEntry id="hi.${i}"  style='display:none'>
     <a id="ai.${i}" onclick='choose(${i})'> 
       <img id="i.${i}" width=100 style='visibility:hidden'/>
       <div class='overlay'>
         <div class=deletion onclick='deleteFromHistory(${i})'>&times;&nbsp;</div>
       </div>
     </a>
   </div>`
                 
redrawHistory = () => {
  var historyHtml = ""
  for (var i = 0; i < MAX_HISTORY_LENGTH; i++) {
    historyHtml += historyFrag(i)
  }
  _("history").innerHTML = historyHtml
  showHistory()
}

window.onload = function() {
  fillIn()
  redrawHistory()
  var id = getArg('d')
  if (id) justShow(id)
}

console.log("presheaf.js ready.")

