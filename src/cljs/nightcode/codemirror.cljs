(ns nightcode.codemirror
  (:require [goog.functions :refer [debounce]]
            [cljsjs.codemirror]
            [cljsjs.codemirror.mode.css]
            [cljsjs.codemirror.mode.javascript]
            [cljsjs.codemirror.mode.markdown]
            [cljsjs.codemirror.mode.sass]
            [cljsjs.codemirror.mode.shell]
            [cljsjs.codemirror.mode.sql]
            [cljsjs.codemirror.mode.xml]
            [goog.dom :as gdom]))

(def ^:const extension->mode
  {"css" "css"
   "js" "javascript"
   "md" "markdown"
   "markdown" "markdown"
   "sass" "sass"
   "sh" "shell"
   "sql" "sql"
   "html" "xml"
   "xml" "xml"})

(def state (atom {:text-content "" :editor nil}))

(def auto-save
  (debounce
    #(.onautosave js/window.java)
    1000))

(defn undo []
  (some-> @state :editor .undo)
  (.onautosave js/window.java))

(defn redo []
  (some-> @state :editor .redo)
  (.onautosave js/window.java))

(defn can-undo? []
  (some-> @state :editor .historySize .-undo (> 0)))

(defn can-redo? []
  (some-> @state :editor .historySize .-redo (> 0)))

(defn set-text-content [content]
  (gdom/setTextContent (.querySelector js/document "#content") content))

(defn get-text-content []
  (some-> @state :editor .getValue))

(defn get-selected-text [])

(defn mark-clean []
  (swap! state assoc :text-content (get-text-content))
  (.onchange js/window.java))

(defn clean? []
  (some-> @state :text-content (= (get-text-content))))

(defn change-theme [dark?]
  (some-> @state :editor (.setOption "theme" (if dark? "lesser-dark" "default"))))

(defn set-text-size [size]
  (-> js/document
      (.querySelector ".CodeMirror")
      .-style
      (aset "fontSize" (str size "px"))))

(defn init [extension]
  (let [content (.querySelector js/document "#content")]
    (swap! state assoc :editor
      (doto
        (.CodeMirror js/window
          js/document.body
          (clj->js {:value (.-textContent content)
                    :lineNumbers true
                    :mode (extension->mode extension)}))
        (.on "change"
          (fn [editor-object change]
            (auto-save)
            (.onchange js/window.java)))
        (.setOption "extraKeys"
          (clj->js {"Ctrl-Z" false
                    "Cmd-Z" false
                    "Shift-Ctrl-Z" false
                    "Shift-Cmd-Z" false}))))
    (.removeChild js/document.body content))
  (mark-clean))

(doto js/window
  (aset "undo" undo)
  (aset "redo" redo)
  (aset "canUndo" can-undo?)
  (aset "canRedo" can-redo?)
  (aset "setTextContent" set-text-content)
  (aset "getTextContent" get-text-content)
  (aset "getSelectedText" get-selected-text)
  (aset "markClean" mark-clean)
  (aset "isClean" clean?)
  (aset "changeTheme" change-theme)
  (aset "setTextSize" set-text-size)
  (aset "init" init))

(set! (.-onload js/window)
  (fn []
    ; hack thanks to http://stackoverflow.com/a/28414332/1663009
    (set! (.-status js/window) "MY-MAGIC-VALUE")
    (set! (.-status js/window) "")
    (.onload js/window.java)
    (.onchange js/window.java)))

