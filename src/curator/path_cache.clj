(ns curator.path-cache
  (:import (org.apache.curator.framework.recipes.cache PathChildrenCache PathChildrenCacheListener)
           (org.apache.curator.framework CuratorFramework)))

(defn ^PathChildrenCache path-cache
  "A Path Cache is used to watch a ZNode. Whenever a child is added, updated or removed, the Path Cache will change
  its state to contain the current set of children, the children's data and the children's state.
  client - the client
  path - path to watch
  listener-fn - a function that accepts 2 arguments, the curator framework and the PathChildrenCacheEvent that occured"
  [^CuratorFramework curator-framework ^String path listener-fn]
  (let [cache (PathChildrenCache. curator-framework path true)
        listenable (.getListenable cache)]
    (.addListener listenable (reify PathChildrenCacheListener
                               (childEvent [this curator-framework event] (listener-fn curator-framework event))))
    cache))