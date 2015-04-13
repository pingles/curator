(ns curator.node-cache
  (:import [org.apache.curator.framework.recipes.cache NodeCache NodeCacheListener]))

(defn node-cache
  "A Node Cache is used to watch a ZNode. Whenever the data is modified or the ZNode is deleted, the Node Cache will
  change its state to contain the current data (or null if ZNode was deleted).
  client - the client
  path - path to watch
  listener-fn - a function that accepts no arguments"
  [curator-framework path listener-fn]
  (let [cache (NodeCache. curator-framework path)
        listenable (.getListenable cache)]
    (.addListener listenable (reify NodeCacheListener
                               (nodeChanged [this] (listener-fn))))
    cache))