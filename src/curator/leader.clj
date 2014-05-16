(ns curator.leader
  (:require [curator.framework :refer (time-units)])
  (:import [org.apache.curator.framework.recipes.leader LeaderSelector LeaderSelectorListener CancelLeadershipException LeaderLatch]
           [org.apache.curator.framework.state ConnectionState]
           [java.util UUID]))

(defn listener [became-leader-fn losing-leader-fn participant-id]
  (reify LeaderSelectorListener
    (takeLeadership [this curator-framework] (became-leader-fn curator-framework participant-id))
    (stateChanged [this curator-framework new-state]
      (when (or (= new-state ConnectionState/SUSPENDED)
                (= new-state ConnectionState/LOST))
        (try (losing-leader-fn curator-framework new-state participant-id)
             (catch Exception e
               (throw (CancelLeadershipException. e)))
             (finally
               (throw (CancelLeadershipException.))))))))

(defn leader-selector
  "Creates a Leader Selector.
   path: The path in ZooKeeper for storing this leadership group
   leaderfn: Function that will be called when the current process becomes
      leader. This function should block and only return when the
      process wishes to release leadership to someone else.
   losingfn: Optional function that will be called when the connection state
      changes indicating we're going to lose leadership.
   participant-id: Uniquely identifies this participant. Defaults to UUID/randomUUID"
  [curator-framework path leaderfn & {:keys [participant-id losingfn]
                                      :or   {participant-id (str (UUID/randomUUID))
                                             losingfn       (constantly nil)}}]
  {:pre [(.startsWith path "/") (not (nil? participant-id))]}
  (doto (LeaderSelector. curator-framework path (listener leaderfn losingfn participant-id))
    (.setId participant-id)
    (.autoRequeue)))

(defn interrupt-leadership
  "Attempt to cancel current leadership if we currently have leadership"
  [leader-selector]
  (.interruptLeadership leader-selector))

(defn leader?
  [leader-selector]
  (.hasLeadership leader-selector))

(defn leader
  [leader-selector]
  (.getLeader leader-selector))

(defn participants
  [leader-selector]
  (.getParticipants leader-selector))
