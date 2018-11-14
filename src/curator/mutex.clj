(ns curator.mutex
  (:import
    [org.apache.curator.framework.recipes.locks InterProcessMutex InterProcessSemaphoreMutex]
    [org.apache.curator.framework CuratorFramework]
    [java.util.concurrent TimeUnit]))

(defn ^InterProcessMutex mutex
  "Create an InterProcessMutex."
  [^CuratorFramework client ^String path]
  (InterProcessMutex. client path))

(defn ^InterProcessSemaphoreMutex semaphore-mutex
  "Create an InterProcessSemaphoreMutex. This mutex is not re-entrant."
  [^CuratorFramework client ^String path]
  (InterProcessSemaphoreMutex. client path))

(defn acquire
  "If no timeunits are given, blocks until acquired. Otherwise returns true or
   false depending on if it acquires the lock in the given time. Time defaults
   to milliseconds."
  ([m] (.acquire m))
  ([m ^long n] (.acquire m n TimeUnit/MILLISECONDS))
  ([m ^long n ^TimeUnit tu] (.acquire m n tu)))

(defn release
  "Release a mutex."
  [m] (.release m))
