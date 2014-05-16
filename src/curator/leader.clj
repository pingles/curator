(ns curator.leader
  (:import [org.apache.curator.framework.recipes.leader LeaderSelector LeaderSelectorListener CancelLeadershipException]
           [org.apache.curator.framework.state ConnectionState]
           [java.util UUID]))

(defn listener [f participant-id]
  (reify LeaderSelectorListener
    (takeLeadership [this curator-framework] (f curator-framework participant-id))
    (stateChanged [this curator-framework new-state]
      (when (or (= new-state ConnectionState/SUSPENDED)
                (= new-state ConnectionState/LOST))
        (throw (CancelLeadershipException. ))))))

(defn leader-selector
  "Creates a Leader Selector.
   path: The path in ZooKeeper for storing this leadership group
   f: Function that will be called when the current process becomes
      leader. This function should block and only return when the
      process wishes to release leadership to someone else.
   participant-id: Uniquely identifies this participant. Defaults to UUID/randomUUID"
  [curator-framework path f & {:keys [participant-id]
                               :or   {participant-id (str (UUID/randomUUID))}}]
  {:pre [(.startsWith path "/") (not (nil? participant-id))]}
  (doto (LeaderSelector. curator-framework path (listener f participant-id))
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
