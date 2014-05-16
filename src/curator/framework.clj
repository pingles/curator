(ns curator.framework
  (:import [org.apache.curator.retry ExponentialBackoffRetry]
           [org.apache.curator.framework CuratorFramework CuratorFrameworkFactory]
           [org.apache.curator.framework.imps CuratorFrameworkState]))

(defn exponential-retry [sleep-millis num-retries]
  (ExponentialBackoffRetry. sleep-millis num-retries))

(defn curator-framework
  [connect-string & {:keys [retry-policy connect-timeout-millis session-timeout-millis namespace]
                     :or   {retry-policy           (exponential-retry 1000 10)
                            connect-timeout-millis 500
                            session-timeout-millis (* 40 1000)}}]
  (-> (doto (CuratorFrameworkFactory/builder)
        (.connectString connect-string)
        (.retryPolicy retry-policy)
        (.connectionTimeoutMs connect-timeout-millis)
        (.sessionTimeoutMs session-timeout-millis)
        (.namespace namespace))
      (.build)))

(defn state [^CuratorFramework curator-framework]
  (let [states {CuratorFrameworkState/STOPPED :stopped
                CuratorFrameworkState/LATENT  :latent
                CuratorFrameworkState/STARTED :started}]
    (states (.getState curator-framework))))
