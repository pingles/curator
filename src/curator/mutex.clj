(ns curator.mutex
  (:import
    [org.apache.curator.framework.recipes.locks InterProcessMutex]
    [org.apache.curator.framework CuratorFramework]
    [java.util.concurrent TimeUnit]))

(defn ^InterProcessMutex mutex
  "Create an InterProcessMutex."
  [^CuratorFramework client ^String path]
  (InterProcessMutex. client path))

(defn acquire
  "If no timeunits are given, blocks until acquired. Otherwise returns true or
   false depending on if it acquires the lock in the given time. Time defaults
   to milliseconds."
  ([^InterProcessMutex m] (.acquire m))
  ([^InterProcessMutex m ^long n] (.acquire m n TimeUnit/MILLISECONDS))
  ([^InterProcessMutex m ^long n ^TimeUnit tu] (.acquire m n tu)))

(defn release
  "Release a mutex."
  [^InterProcessMutex m] (.release m))
