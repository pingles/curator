(ns curator.exhibitor
  (:require [curator.framework :refer (exponential-retry)])
  (:import [org.apache.curator.ensemble.exhibitor
            Exhibitors
            ExhibitorEnsembleProvider
            DefaultExhibitorRestClient
            Exhibitors$BackupConnectionStringProvider]))

(defn ^ExhibitorEnsembleProvider exhibitor-ensemble-provider
  [^Exhibitors exhibitors & {:keys [ rest-client uri polling-ms retry-policy ]
                             :or   { rest-client (DefaultExhibitorRestClient.)
                                    uri          "/exhibitor/v1/cluster/list"
                                    polling-ms   (* 10 1000)
                                    retry-policy (exponential-retry 1000 10) }}]
  (ExhibitorEnsembleProvider. exhibitors rest-client uri polling-ms retry-policy))

(defn backup-connection-provider [s]
  (reify Exhibitors$BackupConnectionStringProvider
    (getBackupConnectionString [this]
      (str s))))

(defn ^Exhibitors exhibitors
  [hosts port backup-connection-string]
  (Exhibitors. (java.util.ArrayList. hosts)
               port (backup-connection-provider backup-connection-string)))
